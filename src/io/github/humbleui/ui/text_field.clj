(ns io.github.humbleui.ui.text-field
  (:require
    [clojure.java.io :as io]
    [clojure.math :as math]
    [clojure.string :as str]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.clipboard :as clipboard]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.ui.dynamic :as dynamic]
    [io.github.humbleui.ui.focusable :as focusable]
    [io.github.humbleui.ui.rect :as rect]
    [io.github.humbleui.ui.with-cursor :as with-cursor]
    [io.github.humbleui.window :as window])
  (:import
    [java.lang AutoCloseable]
    [java.io File]
    [io.github.humbleui.skija BreakIterator Canvas Data Font FontMetrics Image Paint Surface TextLine]
    [io.github.humbleui.skija.shaper Shaper ShapingOptions]
    [io.github.humbleui.skija.svg SVGDOM SVGSVG SVGLength SVGPreserveAspectRatio SVGPreserveAspectRatioAlign SVGPreserveAspectRatioScale]
    [io.github.humbleui.types IPoint IRect Point Rect RRect]))

(set! *warn-on-reflection* true)

(def undo-stack-depth 100)

(defn- ^BreakIterator char-iter [state]
  (or (:char-iter state)
    (doto (BreakIterator/makeCharacterInstance)
      (.setText (:text state)))))

(defn- ^BreakIterator word-iter [state]
  (or (:word-iter state)
    (doto (BreakIterator/makeWordInstance)
      (.setText (:text state)))))

(defn- preceding-word [^BreakIterator word-iter text pos]
  (let [pos' (.preceding word-iter pos)]
    (core/cond+
      (= 0 pos')
      pos'
      
      :do (.next word-iter)
      
      (not (core/between? (.getRuleStatus word-iter) BreakIterator/WORD_NONE BreakIterator/WORD_NONE_LIMIT))
      pos'
      
      :else
      (recur word-iter text pos'))))

(defn- following-word [^BreakIterator word-iter text pos]
  (let [pos' (.following word-iter pos)]
    (cond
      (= (count text) pos')
      pos'
      
      (not (core/between? (.getRuleStatus word-iter) BreakIterator/WORD_NONE BreakIterator/WORD_NONE_LIMIT))
      pos'
      
      :else
      (recur word-iter text pos'))))

(defmulti -edit (fn [state command arg] command))

(defmethod -edit :kill-marked [{:keys [text from to marked-from marked-to] :as state} _ _]
  (if (and marked-from marked-to)
    (let [text' (str (subs text 0 marked-from) (subs text marked-to))
          from' (cond
                  (<= from marked-from) from
                  (<= from marked-to)   marked-from
                  :else                 (- from (- marked-to marked-from)))
          to'   (cond
                  (= from to)         from'
                  (<= to marked-from) to
                  (<= to marked-to)   marked-from
                  :else                 (- to (- marked-to marked-from)))]
      (assoc state
        :text        text'
        :from        from'   
        :to          to'
        :marked-from nil
        :marked-to   nil))
    state))

(defmethod -edit :insert [{:keys [text from to marked-from marked-to] :as state} _ s]
  (assert (= from to))
  (assoc state
    :text (str (subs text 0 to) s (subs text to))
    :from (+ to (count s))
    :to   (+ to (count s))))

(defmethod -edit :insert-marked [{:keys [text from to] :as state} _ {s :text left :selection-start right :selection-end}]
  (assert (= from to))
  (assoc state
    :text        (str (subs text 0 to) s (subs text to))
    :from        (+ to left)
    :to          (+ to right)
    :marked-from to
    :marked-to   (+ to (count s))))

(defmethod -edit :move-char-left [{:keys [text from to] :as state} _ _]
  (cond
    (not= from to)
    (assoc state
      :from (min from to)
      :to   (min from to))
    
    (= 0 to)
    state
    
    :else
    (let [char-iter (char-iter state)
          to'       (.preceding char-iter to)]
      (assoc state
        :from      to'
        :to        to'
        :char-iter char-iter))))

(defmethod -edit :move-char-right [{:keys [text from to] :as state} _ _]
  (cond
    (not= from to)
    (assoc state
      :from (max from to)
      :to   (max from to))
    
    (= (count text) to)
    state
    
    :else
    (let [char-iter (char-iter state)
          to'       (.following char-iter to)]
      (assoc state
        :from      to'
        :to        to'
        :char-iter char-iter))))

(defmethod -edit :move-word-left [{:keys [text from to] :as state} _ _]
  (cond
    (not= from to)
    (recur
      (assoc state
        :from (min from to)
        :to   (min from to))
      :move-word-left nil)
    
    (= 0 to)
    state
    
    :else
    (let [word-iter (word-iter state)
          to'       (preceding-word word-iter text to)]
      (assoc state
        :from      to'
        :to        to'
        :word-iter word-iter))))

(defmethod -edit :move-word-right [{:keys [text from to] :as state} _ _]
  (cond
    (not= from to)
    (recur
      (assoc state
        :from (max from to)
        :to   (max from to))
      :move-word-right nil)
    
    (= (count text) to)
    state
    
    :else
    (let [word-iter (word-iter state)
          to'       (following-word word-iter text to)]
      (assoc state
        :from      to'
        :to        to'
        :word-iter word-iter))))

(defmethod -edit :move-doc-start [{:keys [text from to] :as state} _ _]
  (assoc state
    :from 0
    :to   0))

(defmethod -edit :move-doc-end [{:keys [text from to] :as state} _ _]
  (assoc state
    :from (count text)
    :to   (count text)))

(defmethod -edit :move-to-position [state _ pos']
  (assert (<= 0 pos' (count (:text state))))
  (assoc state
    :from pos'
    :to   pos'))

(defmethod -edit :expand-char-left [{:keys [text from to] :as state} _ _]
  (cond
    (= to 0)
    state
    
    :else
    (let [char-iter (char-iter state)
          to'       (.preceding char-iter to)]
      (assoc state
        :to        to'
        :char-iter char-iter))))

(defmethod -edit :expand-char-right [{:keys [text from to] :as state} _ _]
  (cond
    (= (count text) to)
    state
    
    :else
    (let [char-iter (char-iter state)
          to'       (.following char-iter to)]
      (assoc state
        :to        to'
        :char-iter char-iter))))

(defmethod -edit :expand-word-left [{:keys [text from to] :as state} _ _]
  (cond
    (= to 0)
    state
    
    :else
    (let [word-iter (word-iter state)
          to'       (preceding-word word-iter text to)]
      (assoc state
        :to        to'
        :word-iter word-iter))))

(defmethod -edit :expand-word-right [{:keys [text from to] :as state} _ _]
  (cond
    (= (count text) to)
    state
    
    :else
    (let [word-iter (word-iter state)
          to'       (following-word word-iter text to)]
      (assoc state
        :to        to'
        :word-iter word-iter))))

(defmethod -edit :expand-doc-start [{:keys [text from to] :as state} _ _]
  (assoc state
    :from (if (= 0 from) 0 (max from to))
    :to   0))

(defmethod -edit :expand-doc-end [{:keys [text from to] :as state} _ _]
  (assoc state
    :from (if (= (count text) from) (count text) (min from to))
    :to   (count text)))

(defmethod -edit :expand-to-position [state _ pos']
  (assert (<= 0 pos' (count (:text state))))
  (assoc state
    :to pos'))

(defmethod -edit :delete-char-left [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (if (> to 0)
    (let [char-iter (char-iter state)
          to'       (.preceding char-iter to)]
      (assoc state
        :text (str (subs text 0 to') (subs text to))
        :from to'
        :to   to'))
    state))

(defmethod -edit :delete-char-right [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (if (< to (count text))
    (let [char-iter (char-iter state)
          to'       (.following char-iter to)]
      (assoc state
        :text (str (subs text 0 to) (subs text to'))
        :from to
        :to   to))
    state))

(defmethod -edit :delete-word-left [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (cond
    (= 0 to)
    state
    
    :else
    (let [word-iter (word-iter state)
          to'       (preceding-word word-iter text to)]
      (assoc state
        :text (str (subs text 0 to') (subs text to))
        :from to'
        :to   to'))))

(defmethod -edit :delete-word-right [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (cond
    (= (count text) to)
    state
    
    :else
    (let [word-iter (word-iter state)
          to'       (following-word word-iter text to)]
      (assoc state
        :text (str (subs text 0 to) (subs text to'))
        :from to
        :to   to))))

(defmethod -edit :delete-doc-start [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (if (> to 0)
    (assoc state
      :text (subs text to)
      :from 0
      :to   0)
    state))

(defmethod -edit :delete-doc-end [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (if (< to (count text))
    (assoc state
      :text (subs text 0 to)
      :from to
      :to   to)
    state))

(defmethod -edit :kill [{:keys [text from to] :as state} _ _]
  (assert (not= from to))
  (assoc state
    :text (str (subs text 0 (min from to)) (subs text (max from to)))
    :from (min from to)
    :to   (min from to)))

(defmethod -edit :transpose [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (cond
    (= to 0)
    state
    
    (< to (count text))
    (let [char-iter (char-iter state)
          preceding (.preceding char-iter to)
          following (.following char-iter to)]
      (assoc state
        :text (str
                (subs text 0 preceding)
                (subs text to following)
                (subs text preceding to)
                (subs text following))
        :from following
        :to   following))
    
    (= to (count text))
    (-> state
      (-edit :move-char-left nil)
      (-edit :transpose nil))))

(defmethod -edit :select-word [{:keys [text] :as state} _ pos']
  (assert (<= 0 pos' (count text)))
  (let [word-iter (word-iter state)
        last? (= pos' (count text))
        from' (cond
                last?
                (.preceding word-iter pos')
                
                (.isBoundary word-iter pos')
                pos'
                
                :else
                (.preceding word-iter pos'))
        to'   (if last?
                pos'
                (.following word-iter pos'))]
    (assoc state
      :from (if (= BreakIterator/DONE from') 0 from')
      :to   (if (= BreakIterator/DONE to') (count text) to'))))

(defmethod -edit :select-all [{:keys [text from to] :as state} _ _]
  (assoc state
    :from 0
    :to   (count text)))

(defmethod -edit :copy [{:keys [text from to] :as state} _ _]
  (assert (not= from to))
  (clipboard/set {:format :text/plain :text (subs text (min from to) (max from to))})
  state)  

(defmethod -edit :paste [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (when-some [{paste :text} (clipboard/get :text/plain)]
    (assoc state
      :text (str (subs text 0 to) paste (subs text to))
      :from (+ to (count paste))
      :to   (+ to (count paste)))))

(defmethod -edit :undo [{:keys [undo redo] :as state} _ _]
  (if-some [state' (peek undo)]
    (assoc state'
      :undo (pop undo)
      :redo (conj (or redo []) (select-keys state [:text :from :to :offset])))
    state))

(defmethod -edit :redo [{:keys [undo redo] :as state} _ _]
  (if-some [state' (peek redo)]
    (assoc state'
      :undo (conj (or undo []) (select-keys state [:text :from :to :offset]))
      :redo (pop redo))
    state))

(defn edit [state command arg]
  (let [state'  (-edit state command arg)
        edited? (not= (:text state') (:text state))
        state'  (cond-> state'
                  (or edited? (not= (:to state') (:to state)))
                  (assoc
                    :coord-to           nil
                    :cursor-blink-pivot (core/now)))]
    (core/cond+
      (not edited?)
      state'
      
      (#{:undo :redo} command)
      state'

      :do
      (when-some [line ^TextLine (:line state')]
        (.close line))
      
      ;; kill anything that depends on text
      :let [marked?    (#{:insert-marked :kill-marked} command)
            skip-undo? (and
                         (= (:last-change-to state) (:to state))
                         (= (:last-change-cmd state) command))]
      
      :else
      (cond-> state'
        true
        (assoc
          :line      nil
          :word-iter nil
          :char-iter nil
          :redo      nil)
        
        (not marked?)
        (assoc
          :last-change-cmd command
          :last-change-to  (:to state')
          :marked-from     nil
          :marked-to       nil)
        
        (and (not marked?) (not skip-undo?))
        (update :undo core/conjv-limited (select-keys state [:text :from :to]) undo-stack-depth)))))

(defn- get-cached [text-field ctx key-source key-source-cached key-derived fn]
  (let [{:keys [*state]} text-field
        state        @*state
        source         (state key-source)
        source-cached  (state key-source-cached)
        derived-cached (state key-derived)]
    (or
      (when (= source source-cached)
        derived-cached)
      (let [derived (fn source state)]
        (when (instance? AutoCloseable derived-cached)
          (.close ^AutoCloseable derived-cached))
        (swap! *state assoc
          key-source-cached source
          key-derived       derived)
        derived))))

(defn- ^TextLine text-line [text-field ctx]
  (get-cached text-field ctx :text :cached/text :line
    (fn [text state]
      (.shapeLine core/shaper text (:hui.text-field/font ctx) (:features text-field)))))

(defn- ^TextLine placeholder-line [text-field ctx]
  (get-cached text-field ctx :placeholder :cached/placeholder :line-placeholder
    (fn [placeholder state]
      (.shapeLine core/shaper placeholder (:hui.text-field/font ctx) (:features text-field)))))

(defn- coord-to [text-field ctx]
  (get-cached text-field ctx :to :cached/to :coord-to
    (fn [to state]
      (let [line (text-line text-field ctx)]
        (math/round (.getCoordAtOffset line to))))))

(defn- correct-offset! [text-field ctx]
  (let [{:keys [*state my-rect]} text-field
        state  @*state
        {:keys [offset]} state
        {:keys [scale]
         :hui.text-field/keys [cursor-width padding-left padding-right]} ctx
        line          (text-line text-field ctx)
        coord-to      (coord-to text-field ctx)
        line-width    (.getWidth line)
        rect-width    (:width my-rect)
        padding-left  (* scale padding-left)
        padding-right (* scale (+ cursor-width padding-right))
        min-offset    (- padding-left)
        max-offset    (-> line-width
                        (+ padding-right)
                        (- rect-width)
                        (max min-offset))]
    (if (or
          (nil? offset)
          (< (- coord-to offset) padding-left) ;; cursor overflow left
          (> (- coord-to offset) (- rect-width padding-right)) ;; cursor overflow right
          (< offset min-offset)
          (> offset max-offset))
      (-> *state
        (swap! assoc
          :offset (-> (- coord-to (/ rect-width 2))
                    (core/clamp min-offset max-offset)
                    (math/round)))
        :offset)
      offset)))

(core/deftype+ TextField [*state
                          ^ShapingOptions features
                          ^:mut ^IRect    my-rect]
  protocols/IComponent
  (-measure [this ctx cs]
    (let [{:keys                [scale]
           :hui.text-field/keys [^Font font
                                 cursor-width
                                 padding-left
                                 padding-top
                                 padding-right 
                                 padding-bottom]} ctx
          metrics ^FontMetrics (.getMetrics font)
          line    (text-line this ctx)]
      (IPoint.
        (min
          (:width cs)
          (+ (* scale padding-left)
            (.getWidth line) 
            (* scale cursor-width)
            (* scale padding-right)))
        (+ (Math/round (.getCapHeight metrics))
          (* scale padding-top)
          (* scale padding-bottom)))))
  
  ;       coord-to                        
  ; ├──────────────────┤                  
  ;          ┌───────────────────┐        
  ; ┌────────┼───────────────────┼───────┐
  ; │        │         │         │       │
  ; └────────┼───────────────────┼───────┘
  ;          └───────────────────┘        
  ; ├────────┼───────────────────┤        
  ;   offset     (:width rect)            
  ;                                       
  ; ├────────────────────────────────────┤
  ;            (.getWidth line)           
  (-draw [this ctx rect ^Canvas canvas]
    (set! my-rect rect)
    (correct-offset! this ctx)
    (let [state @*state
          {:keys [text from to marked-from marked-to offset]} state
          {:keys                [scale]
           :hui/keys            [focused?]
           :hui.text-field/keys [^Font font
                                 padding-top]} ctx
          line       (text-line this ctx)
          metrics    ^FontMetrics (.getMetrics font)
          cap-height (Math/round (.getCapHeight metrics))
          ascent     (Math/ceil (- (- (.getAscent metrics)) (.getCapHeight metrics)))
          descent    (Math/ceil (.getDescent metrics))
          baseline   (+ (* scale padding-top) cap-height)
          selection? (not= from to)
          coord-to   (coord-to this ctx)
          coord-from (if (= from to)
                       coord-to
                       (Math/round (.getCoordAtOffset line from)))]
      (canvas/with-canvas canvas
        (canvas/clip-rect canvas (.toRect ^IRect rect))
        
        ;; selection
        (when selection?
          (canvas/draw-rect canvas
            (Rect/makeLTRB
              (+ (:x rect) (- offset) (min coord-from coord-to))
              (+ (:y rect) (* scale padding-top) (- ascent))
              (+ (:x rect) (- offset) (max coord-from coord-to))
              (+ (:y rect) baseline descent))
            (if focused?
              (:hui.text-field/fill-selection-active ctx)
              (:hui.text-field/fill-selection-inactive ctx))))
        
        ;; text
        (let [placeholder? (= "" text)
              line (if placeholder?
                     (placeholder-line this ctx)
                     line)
              x    (+ (:x rect) (- offset))
              y    (+ (:y rect) baseline)
              fill (if placeholder?
                     (:hui.text-field/fill-placeholder ctx)
                     (:hui.text-field/fill-text ctx))]
          (when line
            (.drawTextLine canvas line x y fill)))
        
        ;; composing region
        (when (and marked-from marked-to)
          (let [left  (.getCoordAtOffset line marked-from)
                right (.getCoordAtOffset line marked-to)]
            (canvas/draw-rect canvas
              (Rect/makeLTRB
                (+ (:x rect) (- offset) left)
                (+ (:y rect) baseline (* 1 scale))
                (+ (:x rect) (- offset) right)
                (+ (:y rect) baseline (* 2 scale)))
              (:hui.text-field/fill-text ctx))))
        
        ;; cursor
        (when focused?
          (let [now                   (core/now)
                cursor-width          (* scale (:hui.text-field/cursor-width ctx))
                cursor-left           (quot cursor-width 2)
                cursor-right          (- cursor-width cursor-left)
                cursor-blink-interval (:hui.text-field/cursor-blink-interval ctx)
                cursor-blink-pivot    (:cursor-blink-pivot state)]
            (when (or
                    (<= cursor-blink-interval 0)
                    (<= (mod (- now cursor-blink-pivot) (* 2 cursor-blink-interval)) cursor-blink-interval))
              (canvas/draw-rect canvas
                (Rect/makeLTRB
                  (+ (:x rect) (- offset) coord-to (- cursor-left))
                  (+ (:y rect) (* scale padding-top) (- ascent))
                  (+ (:x rect) (- offset) coord-to cursor-right)
                  (+ (:y rect) baseline descent))
                (:hui.text-field/fill-cursor ctx)))
            (when (> cursor-blink-interval 0)
              (core/schedule
                #(window/request-frame (:window ctx))
                (- cursor-blink-interval
                  (mod (- now cursor-blink-pivot) cursor-blink-interval)))))))))
        
  (-event [this ctx event]
    ; (when-not (#{:frame :frame-skija :window-focus-in :window-focus-out :mouse-move} (:event event))
    ;   (println (:hui/focused? ctx) event))
    (when (:hui/focused? ctx)
      (let [state @*state
            {:keys [text from to marked-from marked-to offset ^TextLine line mouse-clicks last-mouse-click]} state]
        (when (= :mouse-move (:event event))
          (swap! *state assoc
            :mouse-clicks 0))

        (core/cond+
          ;; mouse down
          (and
            (= :mouse-button (:event event))
            (= :primary (:button event))
            (:pressed? event)
            my-rect
            (.contains my-rect (IPoint. (:x event) (:y event))))
          (let [x             (-> (:x event)
                                (- (:x my-rect))
                                (+ offset))
                offset'       (.getOffsetAtCoord line x)
                now           (core/now)
                mouse-clicks' (if (<= (- now last-mouse-click) core/double-click-threshold-ms)
                                (inc mouse-clicks)
                                1)]
            (swap! *state #(cond-> %
                             true
                             (assoc
                               :selecting?       true
                               :last-mouse-click now
                               :mouse-clicks     mouse-clicks')
                         
                             (= 1 mouse-clicks')
                             (edit :move-to-position offset')
                         
                             (= 2 mouse-clicks')
                             (edit :select-word offset')
                          
                             (< 2 mouse-clicks')
                             (edit :select-all nil)))
            true)
          
          ; mouse up
          (and
            (= :mouse-button (:event event))
            (= :primary (:button event))
            (not (:pressed? event)))
          (do
            (swap! *state assoc :selecting? false)
            false)
          
          ;; mouse move
          (and
            (= :mouse-move (:event event))
            (:selecting? state))
          (let [x (-> (:x event)
                    (- (:x my-rect))
                    (+ offset))]
            (cond
              (.contains my-rect (IPoint. (:x event) (:y event)))
              (swap! *state edit :expand-to-position (.getOffsetAtCoord line x))
              
              (< (:y event) (:y my-rect))
              (swap! *state edit :expand-to-position 0)
              
              (>= (:y event) (:bottom my-rect))
              (swap! *state edit :expand-to-position (count (:text state))))
            true)
          
          ;; typing
          (= :text-input (:event event))
          (do
            (swap! *state #(cond-> %
                             true             (dissoc :postponed)
                             (:marked-from %) (edit :kill-marked nil)
                             (not= from to)   (edit :kill nil)
                             true             (edit :insert (:text event))))
            (when-some [postponed (:postponed state)]
              (protocols/-event this ctx postponed))
            true)
          
          ;; composing region
          (= :text-input-marked (:event event))
          (do
            (swap! *state
              #(cond-> %
                 (:marked-from %) (edit :kill-marked nil)
                 (not= from to)   (edit :kill nil)
                 true             (edit :insert-marked event)))
            true)
          
          ;; rect for composing region
          (= :get-rect-for-marked-range (:event event))
          (let [{:hui.text-field/keys [^Font font
                                       padding-top]} ctx
                metrics    ^FontMetrics (.getMetrics font)
                cap-height (Math/ceil (.getCapHeight metrics))
                ascent     (Math/ceil (- (- (.getAscent metrics)) cap-height))
                descent    (Math/ceil (.getDescent metrics))
                baseline   (+ padding-top cap-height)
                left       (.getCoordAtOffset line (or marked-from from))
                right      (if (= (or marked-to to) (or marked-from from))
                             left
                             (.getCoordAtOffset line (or marked-to to)))]
            (IRect/makeLTRB
              (+ (:x my-rect) (- offset) left)
              (+ (:y my-rect) padding-top (- ascent))
              (+ (:x my-rect) (- offset) right)
              (+ (:y my-rect) baseline descent)))
          
          ;; emoji popup macOS
          (and
            (= :macos app/platform)
            (= :key (:event event))
            (:pressed? event)
            (= :space (:key event)) 
            ((:modifiers event) :mac-command)
            ((:modifiers event) :control))
          (do
            (app/open-symbols-palette)
            false)
          
          ;; when exiting composing region with left/right/backspace/delete,
          ;; key down comes before text input, and we need it after
          (and (= :key (:event event)) (:pressed? event) (:marked-from state))
          (do
            (swap! *state assoc :postponed event)
            false)
          
          ;; command
          (and (= :key (:event event)) (:pressed? event))
          (let [key        (:key event)
                shift?     ((:modifiers event) :shift)
                macos?     (= :macos app/platform)
                cmd?       ((:modifiers event) :mac-command)
                option?    ((:modifiers event) :mac-option)
                ctrl?      ((:modifiers event) :control)
                selection? (not= from to)
                ops        (or
                             (core/when-case (and macos? cmd? shift?) key
                               :left  [:expand-doc-start]
                               :right [:expand-doc-end]
                               :z     [:redo])
                             
                             (core/when-case (and macos? option? shift?) key
                               :left  [:expand-word-left]
                               :right [:expand-word-right])

                             (core/when-case shift? key
                               :left  [:expand-char-left]
                               :right [:expand-char-right]
                               :up    [:expand-doc-start]
                               :down  [:expand-doc-end]
                               :home  [:expand-doc-start]
                               :end   [:expand-doc-end])
                             
                             (core/when-case selection? key
                               :backspace [:kill]
                               :delete    [:kill])
                             
                             (core/when-case (and macos? cmd? selection?) key
                               :x         [:copy :kill]
                               :c         [:copy]
                               :v         [:kill :paste])
                               
                             (core/when-case (and macos? cmd?) key
                               :left      [:move-doc-start]
                               :right     [:move-doc-end]
                               :a         [:select-all]
                               :backspace [:delete-doc-start]
                               :delete    [:delete-doc-end]
                               :v         [:paste]
                               :z         [:undo])
                             
                             (core/when-case (and macos? option?) key
                               :left      [:move-word-left]
                               :right     [:move-word-right]
                               :backspace [:delete-word-left]
                               :delete    [:delete-word-right])
                             
                             (core/when-case (and macos? ctrl? option? shift?) key
                               :b [:expand-word-left]
                               :f [:expand-word-right])
                               
                             (core/when-case (and macos? ctrl? shift?) key
                               :b [:expand-char-left]
                               :f [:expand-char-right]
                               :a [:expand-doc-start]
                               :e [:expand-doc-end]
                               :p [:expand-doc-start]
                               :n [:expand-doc-end])
                             
                             (core/when-case (and macos? ctrl? selection?) key
                               :h [:kill]
                               :d [:kill])
                             
                             (core/when-case (and macos? ctrl? option?) key
                               :b [:move-word-left]
                               :f [:move-word-right])
                               
                             (core/when-case (and macos? ctrl?) key
                               :b [:move-char-left]
                               :f [:move-char-right]
                               :a [:move-doc-start]
                               :e [:move-doc-end]
                               :p [:move-doc-start]
                               :n [:move-doc-end]
                               :h [:delete-char-left]
                               :d [:delete-char-right]
                               :k [:delete-doc-end])
                             
                             (core/when-case (and macos? ctrl? (not selection?)) key
                               :t [:transpose])

                             (core/when-case (and (not macos?) shift? ctrl?) key
                               :z [:redo])
                             
                             (core/when-case (and (not macos?) ctrl?) key
                               :a [:select-all]
                               :z [:undo])
                             
                             (core/when-case true key
                               :left      [:move-char-left]
                               :right     [:move-char-right]
                               :up        [:move-doc-start]
                               :down      [:move-doc-end]
                               :home      [:move-doc-start]
                               :end       [:move-doc-end]
                               :backspace [:kill-marked :delete-char-left]
                               :delete    [:kill-marked :delete-char-right]))]
            (when (seq ops)
              (swap! *state
                (fn [state]
                  (reduce #(edit %1 %2 nil) state ops)))
              true))))))
  
  (-iterate [this ctx cb]
    (cb this))
  
  AutoCloseable
  (close [this]
    #_(.close line))) ; TODO

(defn text-input
  ([*state]
   (text-input nil *state))
  ([opts *state]
   (dynamic/dynamic ctx [features (:hui.text-field/font-features ctx)]
     (let [features (reduce #(.withFeatures ^ShapingOptions %1 ^String %2) ShapingOptions/DEFAULT features)]
       (swap! *state #(core/merge-some
                        {:text               ""
                         :cached/text        nil
                         :line               nil
                         
                         :placeholder        ""
                         :cached/placeholder nil
                         :line-placeholder   nil
                         
                         :from               0
                         :to                 0
                         :cached/to          nil
                         :coord-to           nil
                         
                         :last-change-cmd    nil
                         :last-change-to     nil
                         :marked-from        nil
                         :marked-to          nil
                         :word-iter          nil
                         :char-iter          nil
                         :undo               nil
                         :redo               nil
                         :postponed          nil
                         :cursor-blink-pivot (core/now)
                         :offset             nil
                         :selecting?         false
                         :mouse-clicks       0
                         :last-mouse-click   0}
                        %))
       (->TextField *state features nil)))))

(defn text-field
  ([*state]
   (text-field nil *state))
  ([opts *state]
   (focusable/focusable opts
     (with-cursor/with-cursor :ibeam
       (dynamic/dynamic ctx [active? (:hui/focused? ctx)
                             stroke  (if active?
                                       (:hui.text-field/border-active ctx)
                                       (:hui.text-field/border-inactive ctx))
                             bg      (if active?
                                       (:hui.text-field/fill-bg-active ctx)
                                       (:hui.text-field/fill-bg-inactive ctx))
                             radius  (:hui.text-field/border-radius ctx)]
         (rect/rounded-rect {:radius radius} bg
           (rect/rounded-rect {:radius radius} stroke
             (text-input opts *state))))))))

; (require 'user :reload)

(comment
  (require 'examples.text-field :reload)
  (reset! user/*example "text-field-debug"))