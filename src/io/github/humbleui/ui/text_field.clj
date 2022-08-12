(ns io.github.humbleui.ui.text-field
  (:require
    [clojure.java.io :as io]
    [clojure.math :as math]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.clipboard :as clipboard]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.ui.dynamic :as dynamic])
  (:import
    [java.lang AutoCloseable]
    [java.io File]
    [io.github.humbleui.types IPoint IRect Point Rect RRect]
    [io.github.humbleui.skija BreakIterator Canvas Data Font FontMetrics Image Paint Surface TextLine]
    [io.github.humbleui.skija.shaper Shaper ShapingOptions]
    [io.github.humbleui.skija.svg SVGDOM SVGSVG SVGLength SVGPreserveAspectRatio SVGPreserveAspectRatioAlign SVGPreserveAspectRatioScale]))

(set! *warn-on-reflection* true)

(def double-click-threshold-ms 500)

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
      {:text text'
       :from from'   
       :to   to'    
       :last-changed to'})
    state))

(defmethod -edit :insert [{:keys [text from to marked-from marked-to] :as state} _ s]
  (assert (= from to))
  {:text         (str (subs text 0 to) s (subs text to))
   :from         (+ to (count s))
   :to           (+ to (count s))
   :last-changed (+ to (count s))})

(defmethod -edit :insert-marked [{:keys [text from to] :as state} _ {s :text left :selection-start right :selection-end}]
  (assert (= from to))
  {:text         (str (subs text 0 to) s (subs text to))
   :from         (+ to left)
   :to           (+ to right)
   :marked-from  to
   :marked-to    (+ to (count s))
   :last-changed (+ to (count s))})

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
        :char-iter char-iter
        :from      to'
        :to        to'))))

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
        :char-iter char-iter
        :from      to'
        :to        to'))))

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
        :word-iter word-iter
        :from      to'
        :to        to'))))

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
        :word-iter word-iter
        :from      to'
        :to        to'))))

(defmethod -edit :move-doc-start [{:keys [text from to] :as state} _ _]
  (assoc state
    :from 0
    :to   0))

(defmethod -edit :move-doc-end [{:keys [text from to] :as state} _ _]
  (assoc state
    :from (count text)
    :to   (count text)))

(defmethod -edit :expand-char-left [{:keys [text from to] :as state} _ _]
  (cond
    (= to 0)
    state
    
    :else
    (let [char-iter (char-iter state)
          to'       (.preceding char-iter to)]
      (assoc state
        :char-iter char-iter
        :to        to'))))

(defmethod -edit :expand-char-right [{:keys [text from to] :as state} _ _]
  (cond
    (= (count text) to)
    state
    
    :else
    (let [char-iter (char-iter state)
          to'       (.following char-iter to)]
      (assoc state
        :char-iter char-iter
        :to  to'))))

(defmethod -edit :expand-word-left [{:keys [text from to] :as state} _ _]
  (cond
    (= to 0)
    state
    
    :else
    (let [word-iter (word-iter state)
          to'       (preceding-word word-iter text to)]
      (assoc state
        :word-iter word-iter
        :to        to'))))

(defmethod -edit :expand-word-right [{:keys [text from to] :as state} _ _]
  (cond
    (= (count text) to)
    state
    
    :else
    (let [word-iter (word-iter state)
          to'       (following-word word-iter text to)]
      (assoc state
        :word-iter word-iter
        :to  to'))))

(defmethod -edit :expand-doc-start [{:keys [text from to] :as state} _ _]
  (assoc state
    :from (if (= 0 from) 0 (max from to))
    :to   0))

(defmethod -edit :expand-doc-end [{:keys [text from to] :as state} _ _]
  (assoc state
    :from (if (= (count text) from) (count text) (min from to))
    :to   (count text)))

(defmethod -edit :delete-char-left [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (if (> to 0)
    (let [char-iter (char-iter state)
          to'       (.preceding char-iter to)]
      {:text (str (subs text 0 to') (subs text to))
       :from         to'
       :to           to'
       :last-changed to'})
    state))

(defmethod -edit :delete-char-right [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (if (< to (count text))
    (let [char-iter (char-iter state)
          to'       (.following char-iter to)]
      {:text         (str (subs text 0 to) (subs text to'))
       :from         to
       :to           to
       :last-changed to})
    state))

(defmethod -edit :delete-word-left [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (cond
    (= 0 to)
    state
    
    :else
    (let [word-iter (word-iter state)
          to'       (preceding-word word-iter text to)]
      {:text (str (subs text 0 to') (subs text to))
       :from to'
       :to   to'})))

(defmethod -edit :delete-word-right [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (cond
    (= (count text) to)
    state
    
    :else
    (let [word-iter (word-iter state)
          to'       (following-word word-iter text to)]
      {:text (str (subs text 0 to) (subs text to'))
       :from to
       :to   to})))

(defmethod -edit :delete-doc-start [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (if (> to 0)
    {:text (subs text to)
     :from 0
     :to   0}
    state))

(defmethod -edit :delete-doc-end [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (if (< to (count text))
    {:text (subs text 0 to)
     :from to
     :to   to}
    state))

(defmethod -edit :kill [{:keys [text from to] :as state} _ _]
  (assert (not= from to))
  {:text (str (subs text 0 (min from to)) (subs text (max from to)))
   :from (min from to)
   :to   (min from to)})

(defmethod -edit :transpose [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (cond
    (= to 0)
    state
    
    (< to (count text))
    (let [char-iter (char-iter state)
          preceding (.preceding char-iter to)
          following (.following char-iter to)]
      {:text (str
               (subs text 0 preceding)
               (subs text to following)
               (subs text preceding to)
               (subs text following))
       :from following
       :to   following})
    
    (= to (count text))
    (-> state
      (-edit :move-char-left nil)
      (-edit :transpose nil))))

(defmethod -edit :move-to-position [state _ pos']
  (assert (<= 0 pos' (count (:text state))))
  (assoc state
    :from pos'
    :to   pos'))

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
    {:text (str (subs text 0 to) paste (subs text to))
     :from (+ to (count paste))
     :to   (+ to (count paste))}))

(defmethod -edit :undo [{:keys [undo redo] :as state} _ _]
  (if (empty? undo)
    state
    (let [state' (peek undo)]
      {:text (:text state')
       :from (:from state')
       :to   (:to state')
       :undo (pop undo)
       :redo (conj (or redo []) (select-keys state [:text :from :to]))})))

(defmethod -edit :redo [{:keys [undo redo] :as state} _ _]
  (if (empty? redo)
    state
    (let [state' (peek redo)]
      {:text (:text state')
       :from (:from state')
       :to   (:to state')
       :undo (conj (or undo []) (select-keys state [:text :from :to]))
       :redo (pop redo)})))

(defn edit [state command arg]
  (let [state' (-edit state command arg)]
    (cond
      (= (:text state) (:text state'))
      state'
      
      (#{:undo :redo} command)
      state'
      
      (and
        (#{:insert :delete-char-left :delete-char-right} command)
        (= (:to state) (:last-changed state)))
      (-> state'
        (dissoc :redo)
        (assoc :undo (:undo state)))

      :else
      (let [undo   (:undo state [])
            undo'  (if (>= (count undo) undo-stack-depth)
                     (vec (next undo))
                     undo)
            undo'' (conj undo' (select-keys state [:text :from :to]))]
        (-> state'
          (dissoc :redo)
          (assoc :undo undo''))))))

(defn- recalc-line! [text-field]
  (let [{:keys [*state font features line-text line]} text-field
        {:keys [text marked-text marked-pos]} @*state
        text' (if marked-text
                (str (subs text 0 marked-pos) marked-text (subs text marked-pos))
                text)]
    (when (not= text' line-text)
      (some-> ^AutoCloseable line .close)
      (protocols/-set! text-field :line-text text')
      (protocols/-set! text-field :line (.shapeLine core/shaper text' font features))))
  text-field)

(defn- update-offset! [text-field]
  (recalc-line! text-field)
  (let [{:keys [*state cursor-w padding-h offset ^TextLine line my-rect]} text-field
        {:keys [text from to]} @*state
        coord-to    (.getCoordAtOffset line to)
        line-width  (.getWidth line)
        rect-width  (:width my-rect)
        min-offset  (- padding-h)
        max-offset  (-> line-width
                      (+ cursor-w)
                      (+ padding-h)
                      (- rect-width)
                      (max min-offset))]
    (when (or
            (< (- coord-to offset) 0)                        ;; cursor overflow left
            (> (- coord-to offset) (- rect-width padding-h)) ;; cursor overflow right
            (< offset min-offset)
            (> offset max-offset))                           ;; hanging right boundary
      (protocols/-set! text-field :offset
        (-> (- coord-to (/ rect-width 2))
          (core/clamp min-offset max-offset)
          (math/round))))))

(core/deftype+ TextField [*state
                          ^Font font
                          ^FontMetrics metrics
                          ^ShapingOptions features
                          ^Paint fill-text
                          ^Paint fill-cursor
                          ^Paint fill-selection
                          scale
                          cursor-w
                          padding-h
                          padding-top
                          padding-bottom
                          ^:mut           offset
                          ^:mut ^String   line-text
                          ^:mut ^TextLine line
                          ^:mut ^IRect    my-rect
                          ^:mut ^IPoint   mouse-pos
                          ^:mut           mouse-down?
                          ^:mut           clicks
                          ^:mut           last-click]
  protocols/IComponent
  (-measure [this ctx cs]
    (recalc-line! this)
    (IPoint.
      (min
        (:width cs)
        (+ padding-h (.getWidth line) cursor-w padding-h))
      (+ (Math/ceil (.getCapHeight metrics))
        padding-top
        padding-bottom)))
  
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
    (recalc-line! this)
    (let [{:keys [text from to marked-from marked-to]} @*state
          cap-height   (Math/ceil (.getCapHeight metrics))
          ascent       (Math/ceil (- (- (.getAscent metrics)) cap-height))
          descent      (Math/ceil (.getDescent metrics))
          baseline     (+ padding-top cap-height)
          selection?   (not= from to)
          coord-from   (.getCoordAtOffset line from)
          coord-to     (if (= from to)
                         coord-from
                         (.getCoordAtOffset line to))]      
      (canvas/with-canvas canvas
        (canvas/clip-rect canvas (.toRect ^IRect rect))
        
        ;; selection
        (when selection?
          (canvas/draw-rect canvas
            (Rect/makeLTRB
              (+ (:x rect) (- offset) (min coord-from coord-to))
              (+ (:y rect) padding-top (- ascent))
              (+ (:x rect) (- offset) (max coord-from coord-to))
              (+ (:y rect) baseline descent))
            fill-selection))
        
        ;; text
        (.drawTextLine canvas line (+ (:x rect) (- offset)) (+ (:y rect) baseline) fill-text)
        
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
              fill-cursor)))
        
        ;; cursor
        (when-not selection?
          (canvas/draw-rect canvas
            (Rect/makeLTRB
              (+ (:x rect) (- offset) coord-to)
              (+ (:y rect) padding-top (- ascent))
              (+ (:x rect) (- offset) coord-to cursor-w)
              (+ (:y rect) baseline descent))
            fill-cursor)))))
        
  (-event [this event]
    ; (when-not (#{:frame :frame-skija :window-focus-in :window-focus-out :mouse-move :mouse-button} (:event event))
    ;   (println event))
    (let [state @*state
          {:keys [text from to marked-from marked-to]} state]
      (when (= :mouse-move (:event event))
        (set! mouse-pos (IPoint. (:x event) (:y event)))
        (set! clicks 0))

      (core/cond+
        ;; mouse down
        (and
          (= :mouse-button (:event event))
          (= :primary (:button event))
          (:pressed? event)
          (.contains my-rect mouse-pos))
        (let [x      (-> (:x mouse-pos)
                       (- (:x my-rect))
                       (+ offset))
              offset (.getOffsetAtCoord line x)
              now    (System/currentTimeMillis)]
          (set! mouse-down? true)
          (set! last-click now)
          (if (<= (- now last-click) double-click-threshold-ms)
            (do
              (set! clicks (inc clicks))
              (set! last-click now))
            (set! clicks 1))
          
          (case (int clicks)
            1
            (swap! *state edit :move-to-position offset)
            
            2
            (swap! *state edit :select-word offset)
            
            ; else
            (swap! *state edit :select-all nil))
          
          (not= @*state state))
        
        ; mouse up
        (and
          (= :mouse-button (:event event))
          (= :primary (:button event))
          (not (:pressed? event)))
        (do
          (set! mouse-down? false)
          false)
        
        ;; mouse move
        (and
          (= :mouse-move (:event event))
          mouse-down?)
        (let [x (-> (:x mouse-pos)
                  (- (:x my-rect))
                  (+ offset))]
          (cond
            (.contains my-rect mouse-pos)
            (swap! *state assoc :to (.getOffsetAtCoord line x))
            
            (< (:y mouse-pos) (:y my-rect))
            (swap! *state assoc :to 0)
            
            (>= (:y mouse-pos) (:bottom my-rect))
            (swap! *state assoc :to (count (:text state))))
          (not= @*state state))
        
        ;; typing
        (= :text-input (:event event))
        (let [postponed (:postponed state)
              state'    (swap! *state #(cond-> %
                                         true             (dissoc :postponed)
                                         (:marked-from %) (edit :kill-marked nil)
                                         (not= from to)   (edit :kill nil)
                                         true             (edit :insert (:text event))))]
          (core/eager-or
            (when (not= state state')
              (update-offset! this)
              true)
            (when postponed
              (protocols/-event this postponed))))
        
        ;; composing region
        (= :text-input-marked (:event event))
        (swap! *state
          #(cond-> %
             (:marked-from %) (edit :kill-marked nil)
             (not= from to)   (edit :kill nil)
             true             (edit :insert-marked event)))
        
        ;; rect for composing region
        (= :get-rect-for-marked-range (:event event))
        (when (and marked-from marked-to)
          (let [cap-height (Math/ceil (.getCapHeight metrics))
                ascent     (Math/ceil (- (- (.getAscent metrics)) cap-height))
                descent    (Math/ceil (.getDescent metrics))
                baseline   (+ padding-top cap-height)
                left       (.getCoordAtOffset line marked-from)
                right      (.getCoordAtOffset line marked-to)]
            (IRect/makeLTRB
              (+ (:x my-rect) (- offset) left)
              (+ (:y my-rect) padding-top (- ascent))
              (+ (:x my-rect) (- offset) right)
              (+ (:y my-rect) baseline descent))))
      
        ;; postpone command if marked
        (and (= :key (:event event)) (:pressed? event) (:marked-from state))
        (swap! *state assoc :postponed event)
        
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
            (let [state' (swap! *state (fn [state]
                                         (reduce #(edit %1 %2 nil) state ops)))]
              (when (not= state state')
                (update-offset! this)
                true)))))))
  
  AutoCloseable
  (close [this]
    #_(.close line))) ; TODO
  
(defn text-field
  ([*state]
   (text-field *state nil))
  ([*state opts]
   (dynamic/dynamic ctx [^Font font     (or (:font opts) (:font-ui ctx))
                         metrics        (.getMetrics font)
                         scale          (:scale ctx)
                         cursor-w       (* 1 (:scale ctx))
                         padding-h      (Math/ceil (* (or (:padding-h opts) 0) (:scale ctx)))
                         padding-v      (or
                                          (some-> (:padding-v opts) (* (:scale ctx)))
                                          (max
                                            (- (- (.getAscent metrics)) (.getCapHeight metrics))
                                            (.getDescent metrics)))
                         padding-top    (Math/ceil (or (some-> (:padding-top opts) (* (:scale ctx))) padding-v))
                         padding-bottom (Math/ceil (or (some-> (:padding-bottom opts) (* (:scale ctx))) padding-v))
                         fill-text      ^Paint (or (:fill-text opts) (:fill-text ctx))
                         fill-cursor    ^Paint (or (:fill-cursor opts) (:fill-cursor ctx))
                         fill-selection ^Paint (or (:fill-selection opts) (:fill-selection ctx))]
     (let [features (reduce #(.withFeatures ^ShapingOptions %1 ^String %2) ShapingOptions/DEFAULT (:features opts))]
       (->TextField
         *state
         font
         metrics
         features
         fill-text
         fill-cursor
         fill-selection
         scale
         cursor-w
         padding-h
         padding-top
         padding-bottom
         (- padding-h)
         nil      ; line-text
         nil      ; line
         nil      ; my-rect
         (IPoint. 0 0) ; mouse-pos
         false    ; mouse-down?
         0        ; clicks
         0        ; last-click
         )))))

; (require 'user :reload)

(comment
  (require 'examples.text-field :reload)
  (reset! user/*example "text-field-debug"))