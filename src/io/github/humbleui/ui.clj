(ns io.github.humbleui.ui
  (:require
    [io.github.humbleui.core :as core :refer [deftype+]]
    [io.github.humbleui.protocols :as protocols :refer [IComponent -measure -draw -event]])
  (:import
    [java.lang AutoCloseable]
    [io.github.humbleui.types IPoint IRect Point Rect RRect]
    [io.github.humbleui.skija Canvas Font FontMetrics Paint TextLine]))

(set! *warn-on-reflection* true)

(defn draw [app ctx cs canvas]
  (-measure app ctx cs)
  (-draw app ctx cs canvas))

(defn event [app event]
  (-event app event))
  
(defn event-propagate
  ([event child child-rect]
   (event-propagate event child child-rect child-rect))
  ([event child child-rect offset]
   (when child-rect
     (let [pos    (:hui.event/pos event)
           event' (cond
                    (nil? pos)
                    event
                    
                    (not (.contains ^IRect child-rect pos))
                    (dissoc event :hui.event/pos)
                    
                    (= 0 (:x offset) (:y offset))
                    event
                    
                    :else
                    (assoc event :hui.event/pos
                      (IPoint. (- (:x pos) (:x offset)) (- (:y pos) (:y offset)))))]
       (-event child event')))))

(defn child-close [child]
  (when (instance? AutoCloseable child)
    (.close ^AutoCloseable child)))

(deftype+ Label [^String text ^Font font ^Paint paint ^TextLine line ^FontMetrics metrics]
  IComponent
  (-measure [_ ctx cs]
    (IPoint. (.getWidth line) (.getCapHeight metrics)))
  
  (-draw [_ ctx cs ^Canvas canvas]
    (.drawTextLine canvas line 0 (.getCapHeight metrics) paint))
  
  (-event [_ event])
  
  AutoCloseable
  (close [_]
    #_(.close line))) ; TODO

(defn label [text font paint]
  (->Label text font paint (TextLine/make text font) (.getMetrics ^Font font)))

(deftype+ HAlign [coeff child-coeff child ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (-measure child ctx cs))
  
  (-draw [_ ctx cs ^Canvas canvas]
    (let [layer      (.save canvas)
          child-size (-measure child ctx cs)
          left       (- (* (:width cs) coeff) (* (:width child-size) child-coeff))]
      (set! child-rect (IRect/makeXYWH left 0 (:width child-size) (:height cs)))
      (try
        (.translate canvas left 0)
        (-draw child ctx child-rect canvas)
        (finally
          (.restoreToCount canvas layer)))))
  
  (-event [_ event]
    (event-propagate event child child-rect))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn halign
  ([coeff child] (halign coeff coeff child))
  ([coeff child-coeff child] (->HAlign coeff child-coeff child nil)))

(deftype+ VAlign [coeff child-coeff child ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (-measure child ctx cs))
  
  (-draw [_ ctx cs ^Canvas canvas]
    (let [layer      (.save canvas)
          child-size (-measure child ctx cs)
          top        (- (* (:height cs) coeff) (* (:height child-size) child-coeff))]
      (set! child-rect (IRect/makeXYWH 0 top (:width cs) (:height child-size)))
      (try
        (.translate canvas 0 top)
        (-draw child ctx child-rect canvas)
        (finally
          (.restoreToCount canvas layer)))))
  
  (-event [_ event]
    (event-propagate event child child-rect))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn valign
  ([coeff child] (valign coeff coeff child))
  ([coeff child-coeff child] (->VAlign coeff child-coeff child nil)))

(defn stretch
  ([child]
   (with-meta child {:stretch 1}))
  ([coeff child]
   (with-meta child {:stretch coeff})))

(deftype+ Column [children ^:mut child-rects]
  IComponent
  (-measure [_ ctx cs]
    (reduce
      (fn [{:keys [width height]} child]
        (let [child-size (-measure child ctx (update cs :height - height))]
          (IPoint. (max width (:width child-size)) (+ height (:height child-size)))))
      (IPoint. 0 0) children))
  
  (-draw [_ ctx cs ^Canvas canvas]
    (let [known   (core/for-map [child children
                                 :when (not (:stretch (meta child)))]
                    [child (-measure child ctx cs)]) ;; TODO cs
          space   (- (:height cs) (reduce + (for [[_ size] known] (:height size))))
          stretch (reduce + (map #(:stretch (meta %) 0) children))
          layer   (.save canvas)]
      (try
        (loop [height   0
               rects    []
               children children]
          (if-some [child (first children)]
            (let [child-size (or (known child)
                               (IPoint. (:width cs) (-> space (/ stretch) (* (:stretch (meta child))))))]
              (-draw child ctx (assoc child-size :width (:width cs)) canvas)
              (.translate canvas 0 (:height child-size))
              (recur
                (+ height (:height child-size))
                (conj rects (IRect/makeXYWH 0 height (:width cs) (:height child-size)))
                (next children)))
            (set! child-rects rects)))
        (.restoreToCount canvas layer))))
  
  (-event [_ event]
    (reduce
      (fn [acc [child rect]]
        (core/eager-or acc (event-propagate event child rect)))
      false
      (core/zip children child-rects)))
  
  AutoCloseable
  (close [_]
    (doseq [child children]
      (child-close child))))

(defn column [& children]
  (->Column (vec children) nil))

(deftype+ Row [children ^:mut child-rects]
  IComponent
  (-measure [_ ctx cs]
    (reduce
      (fn [{:keys [width height]} child]
        (let [child-size (-measure child ctx (update cs :width - width))]
          (IPoint. (+ width (:width child-size)) (max height (:height child-size)))))
      (IPoint. 0 0) children))
  
  (-draw [_ ctx cs ^Canvas canvas]
    (let [known   (core/for-map [child children
                                 :when (not (:stretch (meta child)))]
                    [child (-measure child ctx cs)]) ;; TODO cs
          space   (- (:width cs) (reduce + (for [[_ size] known] (:width size))))
          stretch (reduce + (map #(:stretch (meta %) 0) children))
          layer   (.save canvas)]
      (try
        (loop [width   0
               rects    []
               children children]
          (if-some [child (first children)]
            (let [child-size (or (known child)
                               (IPoint. (-> space (/ stretch) (* (:stretch (meta child)))) (:height cs)))]
              (-draw child ctx (assoc child-size :height (:height cs)) canvas)
              (.translate canvas (:width child-size) 0)
              (recur
                (+ width (:width child-size))
                (conj rects (IRect/makeXYWH width 0 (:width child-size) (:height cs)))
                (next children)))
            (set! child-rects rects)))
        (.restoreToCount canvas layer))))
  
  (-event [_ event]
    (reduce
      (fn [acc [child rect]]
        (core/eager-or acc (event-propagate event child rect)))
      false
      (core/zip children child-rects)))
  
  AutoCloseable
  (close [_]
    (doseq [child children]
      (child-close child))))

(defn row [& children]
  (->Row (vec children) nil))

(defrecord Gap [width height]
  IComponent
  (-measure [_ ctx cs]
    (let [{:keys [scale]} ctx]
      (IPoint. (* scale width) (* scale height))))
  (-draw [_ ctx cs canvas])
  (-event [_ event]))

(defn gap [width height]
  (->Gap width height))

(deftype+ Padding [left top right bottom child ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (let [{:keys [scale]} ctx
          left'   (* scale left)
          right'  (* scale right)
          top'    (* scale top)
          bottom' (* scale bottom)
          child-cs   (IPoint. (- (:width cs) left' right') (- (:height cs) top' bottom'))
          child-size (-measure child ctx child-cs)]
      (IPoint.
        (+ (:width child-size) left' right')
        (+ (:height child-size) top' bottom'))))
  
  (-draw [_ ctx cs ^Canvas canvas]
    (let [{:keys [scale]} ctx
          left'    (* scale left)
          top'     (* scale top)
          right'   (* scale right)
          bottom'  (* scale bottom)
          layer    (.save canvas)
          width'   (- (:width cs) left' right')
          height'  (- (:height cs) top' bottom')]
      (set! child-rect (IRect/makeXYWH left' top' width' height'))
      (try
        (.translate canvas left' top')
        (-draw child ctx (IPoint. width' height') canvas)
        (finally
          (.restoreToCount canvas layer)))))
  
  (-event [_ event]
    (event-propagate event child child-rect))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn padding
  ([p child] (->Padding p p p p child nil))
  ([w h child] (->Padding w h w h child nil))
  ([l t r b child] (->Padding l t r b child nil)))

(deftype+ Fill [paint child ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (-measure child ctx cs))
  
  (-draw [_ ctx cs ^Canvas canvas]
    (set! child-rect (IRect/makeXYWH 0 0 (:width cs) (:height cs)))
    (.drawRect canvas (Rect/makeXYWH 0 0 (:width cs) (:height cs)) paint)
    (-draw child ctx cs canvas))
  
  (-event [_ event]
    (event-propagate event child child-rect))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn fill [paint child]
  (->Fill paint child nil))

(deftype+ ClipRRect [radii child ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (-measure child ctx cs))
  
  (-draw [_ ctx cs ^Canvas canvas]
    (let [{:keys [scale]} ctx
          radii' (into-array Float/TYPE (map #(* scale %) radii))
          rrect  (RRect/makeComplexXYWH 0 0 (:width cs) (:height cs) radii')
          layer  (.save canvas)]
      (try
        (set! child-rect (IRect/makeXYWH 0 0 (:width cs) (:height cs)))
        (.clipRRect canvas rrect true)
        (-draw child ctx cs canvas)
        (finally
          (.restoreToCount canvas layer)))))
  
  (-event [_ event]
    (event-propagate event child child-rect))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn clip-rrect
  ([r child] (->ClipRRect [r] child nil)))

(deftype+ Hoverable [child ^:mut child-rect ^:mut hovered?]
  IComponent
  (-measure [_ ctx cs]
    (-measure child ctx cs))
  
  (-draw [_ ctx cs canvas]
    (set! child-rect (IRect/makeXYWH 0 0 (:width cs) (:height cs)))
    (let [ctx' (cond-> ctx hovered? (assoc :hui/hovered? true))]
      (-draw child ctx' cs canvas)))
  
  (-event [_ event]
    (core/eager-or
      (event-propagate event child child-rect)
      (when (= :hui/mouse-move (:hui/event event))
        (let [hovered?' (some? (:hui.event/pos event))]
          (when (not= hovered? hovered?')
            (set! hovered? hovered?')
            true)))))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn hoverable [child]
  (->Hoverable child nil false))

(deftype+ Clickable [on-click child ^:mut child-rect ^:mut hovered? ^:mut pressed?]
  IComponent
  (-measure [_ ctx cs]
    (-measure child ctx cs))
  
  (-draw [_ ctx cs canvas]
    (set! child-rect (IRect/makeXYWH 0 0 (:width cs) (:height cs)))
    (let [ctx' (cond-> ctx
                 hovered?                (assoc :hui/hovered? true)
                 (and pressed? hovered?) (assoc :hui/active? true))]
      (-draw child ctx' cs canvas)))
  
  (-event [_ event]
    (core/eager-or
      (when (= :hui/mouse-move (:hui/event event))
        (let [hovered?' (some? (:hui.event/pos event))]
          (when (not= hovered? hovered?')
            (set! hovered? hovered?')
            true)))
      (when (= :hui/mouse-button (:hui/event event))
        (let [pressed?' (if (:hui.event.mouse-button/is-pressed event)
                          hovered?
                          (do
                            (when (and pressed? hovered?) (on-click))
                            false))]
          (when (not= pressed? pressed?')
            (set! pressed? pressed?')
            true)))
      (event-propagate event child child-rect)))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn clickable [on-click child]
  (->Clickable on-click child nil false false))

(deftype+ Contextual [child-ctor ^:mut child ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (let [child' (child-ctor ctx)]
      (when-not (identical? child child')
        (child-close child)
        (set! child child')))
    (-measure child ctx cs))
  
  (-draw [_ ctx cs canvas]
    (let [child' (child-ctor ctx)]
      (when-not (identical? child child')
        (child-close child)
        (set! child child')))
    (set! child-rect (IRect/makeXYWH 0 0 (:width cs) (:height cs)))
    (-draw child ctx cs canvas))
  
  (-event [_ event]
    (event-propagate event child child-rect))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn contextual [child-ctor]
  (->Contextual child-ctor nil nil))

(defn collect
  ([pred form] (collect [] pred form))
  ([acc pred form]
   (cond
     (pred form)        (conj acc form)
     (sequential? form) (reduce (fn [acc el] (collect acc pred el)) acc form)
     (map? form)        (reduce-kv (fn [acc k v] (-> acc (collect pred k) (collect pred v))) acc form)
     :else              acc)))

(defn bindings->syms [bindings]
  (->> bindings
    (partition 2)
    (map first)
    (collect symbol?)
    (map name)
    (map symbol)
    (into #{})
    (vec)))

(defmacro dynamic [ctx-sym bindings & body]
  (let [syms (bindings->syms bindings)]
    `(let [inputs-fn# (core/memoize-last (fn [~@syms] ~@body))]
       (contextual
         (fn [~ctx-sym]
           (let [~@bindings]
             (inputs-fn# ~@syms)))))))

(deftype+ WithContext [data child ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (-measure child (merge ctx data) cs))
  
  (-draw [_ ctx cs ^Canvas canvas]
    (set! child-rect (IRect/makeXYWH 0 0 (:width cs) (:height cs)))
    (-draw child (merge ctx data) cs canvas))
  
  (-event [_ event]
    (event-propagate event child child-rect))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn with-context [data child]
  (->WithContext data child nil))

(deftype+ VScroll [child ^:mut offset ^:mut size ^:mut child-size]
  IComponent
  (-measure [_ ctx cs]
    (let [child-cs (assoc cs :height Integer/MAX_VALUE)]
      (set! child-size (-measure child ctx child-cs))
      (set! offset (core/clamp offset (- (:height cs) (:height child-size)) 0))
      (IPoint. (:width child-size) (:height cs))))
  
  (-draw [_ ctx cs ^Canvas canvas]
    (set! size cs)
    (let [layer    (.save canvas)
          child-cs (assoc cs :height Integer/MAX_VALUE)]
      (try
        (.clipRect canvas (Rect/makeXYWH 0 0 (:width cs) (:height cs)))
        (.translate canvas 0 offset)
        (-draw child ctx child-cs canvas)
        (finally
          (.restoreToCount canvas layer)))))
  
  (-event [_ event]
    (let [changed?   (not= 0 (:hui.event.mouse-scroll/dy event 0))
          _          (when changed?
                       (set! offset (-> offset
                                      (+ (:hui.event.mouse-scroll/dy event))
                                      (core/clamp (- (:height size) (:height child-size)) 0))))
          child-rect (IRect/makeLTRB 0 0 (min (:width child-size) (:width size)) (min (:height child-size) (:height size)))]
      (core/eager-or changed? (event-propagate event child child-rect (IPoint. 0 offset)))))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn vscroll [child]
  (->VScroll child 0 nil nil))

(deftype+ VScrollbar [child ^Paint fill-track ^Paint fill-thumb ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (-measure child ctx cs))
  
  (-draw [_ ctx cs ^Canvas canvas]
    (set! child-rect (IRect/makeXYWH 0 0 (:width cs) (:height cs)))
    (-draw child ctx cs canvas)
    (let [{:keys [scale]} ctx
          content-y (- (:offset child))
          content-h (:height (:child-size child))
          scroll-y  (:y child-rect)
          scroll-h  (:height cs)
          scroll-r  (:right child-rect)
          
          padding (* 4 scale)
          track-w (* 4 scale)
          track-x (- (:width cs) track-w padding)
          track-y (+ scroll-y padding)
          track-h (- scroll-h (* 2 padding))
          track   (RRect/makeXYWH track-x track-y track-w track-h (* 2 scale))
          
          thumb-w       (* 4 scale)
          min-thumb-h   (* 16 scale)
          thumb-y-ratio (/ content-y content-h)
          thumb-y       (-> (* track-h thumb-y-ratio) (core/clamp 0 (- track-h min-thumb-h)) (+ track-y))
          thumb-b-ratio (/ (+ content-y scroll-h) content-h)
          thumb-b       (-> (* track-h thumb-b-ratio) (core/clamp min-thumb-h track-h) (+ track-y))
          thumb         (RRect/makeLTRB track-x thumb-y (+ track-x thumb-w) thumb-b (* 2 scale))]
      (.drawRRect canvas track fill-track)
      (.drawRRect canvas thumb fill-thumb)))

  (-event [_ event]
    (event-propagate event child child-rect))
  
  AutoCloseable
  (close [_]
    ;; TODO causes crash
    ; (.close fill-track)
    ; (.close fill-thumb)
    (child-close child)))

(defn vscrollbar [child]
  (when-not (instance? VScroll child)
    (throw (ex-info (str "Expected VScroll, got: " (type child)) {:child child})))
  (->VScrollbar child
    (doto (Paint.) (.setColor (unchecked-int 0x10000000)))
    (doto (Paint.) (.setColor (unchecked-int 0x60000000)))
    nil))

(comment
  (do
    (println)
    (set! *warn-on-reflection* true)
    (require 'io.github.humbleui.ui :reload)))