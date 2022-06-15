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

(defmulti edit (fn [state command arg] command))

(defmethod edit :insert [{:keys [text from to]} _ text']
  (assert (= from to))
  {:text (str (subs text 0 to) text' (subs text to))
   :from (+ to (count text'))
   :to   (+ to (count text'))})
  
(defmethod edit :replace [{:keys [text from to]} _ text']
  (assert (not= from to))
  (let [left  (min from to)
        right (max from to)]
    {:text (str (subs text 0 left) text' (subs text right))
     :from (+ left (count text'))
     :to   (+ left (count text'))}))

(defmethod edit :move-char-left [{:keys [text from to] :as state} _ _]
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

(defmethod edit :move-char-right [{:keys [text from to] :as state} _ _]
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

(defmethod edit :move-word-left [{:keys [text from to] :as state} _ _]
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

(defmethod edit :move-word-right [{:keys [text from to] :as state} _ _]
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

(defmethod edit :move-doc-start [{:keys [text from to] :as state} _ _]
  (assoc state
    :from 0
    :to   0))

(defmethod edit :move-doc-end [{:keys [text from to] :as state} _ _]
  (assoc state
    :from (count text)
    :to   (count text)))

(defmethod edit :expand-char-left [{:keys [text from to] :as state} _ _]
  (cond
    (= to 0)
    state
    
    :else
    (let [char-iter (char-iter state)
          to'       (.preceding char-iter to)]
      (assoc state
        :char-iter char-iter
        :to        to'))))

(defmethod edit :expand-char-right [{:keys [text from to] :as state} _ _]
  (cond
    (= (count text) to)
    state
    
    :else
    (let [char-iter (char-iter state)
          to'       (.following char-iter to)]
      (assoc state
        :char-iter char-iter
        :to  to'))))

(defmethod edit :expand-word-left [{:keys [text from to] :as state} _ _]
  (cond
    (= to 0)
    state
    
    :else
    (let [word-iter (word-iter state)
          to'       (preceding-word word-iter text to)]
      (assoc state
        :word-iter word-iter
        :to        to'))))

(defmethod edit :expand-word-right [{:keys [text from to] :as state} _ _]
  (cond
    (= (count text) to)
    state
    
    :else
    (let [word-iter (word-iter state)
          to'       (following-word word-iter text to)]
      (assoc state
        :word-iter word-iter
        :to  to'))))


(defmethod edit :expand-doc-start [{:keys [text from to] :as state} _ _]
  (assoc state
    :from (if (= 0 from) 0 (max from to))
    :to   0))

(defmethod edit :expand-doc-end [{:keys [text from to] :as state} _ _]
  (assoc state
    :from (if (= (count text) from) (count text) (min from to))
    :to   (count text)))

(defmethod edit :delete-char-left [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (if (> to 0)
    (let [char-iter (char-iter state)
          to'       (.preceding char-iter to)]
      {:text      (str (subs text 0 to') (subs text to))
       :from      to'
       :to        to'})
    state))

(defmethod edit :delete-char-right [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (if (< to (count text))
    (let [char-iter (char-iter state)
          to'       (.following char-iter to)]
      {:text (str (subs text 0 to) (subs text to'))
       :from to
       :to   to})
    state))

(defmethod edit :delete-word-left [{:keys [text from to] :as state} _ _]
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

(defmethod edit :delete-word-right [{:keys [text from to] :as state} _ _]
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

(defmethod edit :delete-doc-start [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (if (> to 0)
    {:text (subs text to)
     :from 0
     :to   0}
    state))

(defmethod edit :delete-doc-end [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (if (< to (count text))
    {:text (subs text 0 to)
     :from to
     :to   to}
    state))

(defmethod edit :kill [{:keys [text from to] :as state} _ _]
  (assert (not= from to))
  {:text (str (subs text 0 (min from to)) (subs text (max from to)))
   :from (min from to)
   :to   (min from to)})

(defmethod edit :transpose [{:keys [text from to] :as state} _ _]
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
      (edit :move-char-left nil)
      (edit :transpose nil))))

(defmethod edit :select-all [{:keys [text from to] :as state} _ _]
  (assoc state
    :from 0
    :to   (count text)))

(defmethod edit :copy [{:keys [text from to] :as state} _ _]
  (assert (not= from to))
  (clipboard/set {:format :text/plain :text (subs text (min from to) (max from to))})
  state)  

(defmethod edit :paste [{:keys [text from to] :as state} _ _]
  (assert (= from to))
  (when-some [{paste :text} (clipboard/get :text/plain)]
    {:text (str (subs text 0 to) paste (subs text to))
     :from (+ to (count paste))
     :to   (+ to (count paste))}))

(core/deftype+ TextField [*state
                          ^Font font
                          ^FontMetrics metrics
                          ^ShapingOptions features
                          ^Paint fill-text
                          ^Paint fill-cursor
                          ^Paint fill-selection
                          ^String ^:mut line-text
                          ^TextLine ^:mut line]
  protocols/IComponent
  (-measure [_ ctx cs]
    (IPoint. (:width cs) (Math/ceil (.getCapHeight metrics))))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [{:keys [text from to]} @*state
          baseline   (Math/ceil (.getCapHeight metrics))
          ascent     (- (+ baseline (Math/ceil (.getAscent metrics))))
          descent    (Math/ceil (.getDescent metrics))
          selection? (not= from to)]
      (when (not= text line-text)
        (some-> line .close)
        (set! line-text text)
        (set! line (.shapeLine core/shaper text font features)))
      (canvas/with-canvas canvas
        ;; TODO do not clip vertically
        (canvas/clip-rect canvas (Rect/makeXYWH (:x rect) (- (:y rect) ascent) (:width rect) (+ ascent baseline descent)))
        (when selection?
          (canvas/draw-rect canvas
            (Rect/makeLTRB
              (+ (:x rect) (.getCoordAtOffset line (min from to)))
              (- (:y rect) ascent)
              (+ (:x rect) (.getCoordAtOffset line (max from to)))
              (+ (:y rect) baseline descent))
            fill-selection))
        (.drawTextLine canvas line (:x rect) (+ (:y rect) baseline) fill-text)
        (when-not selection?
          (canvas/draw-rect canvas
            (Rect/makeLTRB
              (+ (:x rect) (.getCoordAtOffset line to))
              (- (:y rect) ascent)
              (+ (:x rect) (.getCoordAtOffset line to) (* 1 (:scale ctx)))
              (+ (:y rect) baseline descent))
            fill-cursor)))))
        
  (-event [_ event]
    (let [state @*state
          {:keys [text from to]} state]
      
      (cond 
        (= :text-input (:event event))
        (let [op     (if (= from to) :insert :replace)
              state' (swap! *state edit op (:text event))]
          (not= op state))
      
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
                             :right [:expand-doc-end])
                           
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
                             :v         [:paste])
                           
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

                           (core/when-case (and (not macos?) ctrl?) key
                             :a [:select-all])
                           
                           (core/when-case true key
                             :left      [:move-char-left]
                             :right     [:move-char-right]
                             :up        [:move-doc-start]
                             :down      [:move-doc-end]
                             :home      [:move-doc-start]
                             :end       [:move-doc-end]
                             :backspace [:delete-char-left]
                             :delete    [:delete-char-right]))]
          (when (seq ops)
            (let [state' (swap! *state (fn [state]
                                         (reduce #(edit %1 %2 nil) state ops)))]
              (not= state state')))))))
  
  AutoCloseable
  (close [_]
    #_(.close line))) ; TODO
  
(defn text-field
  ([*state]
   (text-field *state nil))
  ([*state opts]
   (dynamic/dynamic ctx [font           ^Font  (or (:font opts) (:font-ui ctx))
                         fill-text      ^Paint (or (:fill-text opts) (:fill-text ctx))
                         fill-cursor    ^Paint (or (:fill-cursor opts) (:fill-cursor ctx))
                         fill-selection ^Paint (or (:fill-selection opts) (:fill-selection ctx))]
     (let [features (reduce #(.withFeatures ^ShapingOptions %1 ^String %2) ShapingOptions/DEFAULT (:features opts))]
       (->TextField *state font (.getMetrics ^Font font) features fill-text fill-cursor fill-selection nil nil)))))

(comment
  (require 'examples.text-field :reload)
  (reset! user/*example "text-field"))