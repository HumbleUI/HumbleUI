(ns io.github.humbleui.ui
  (:require
    [clojure.math :as math]
    [io.github.humbleui.core :as core :refer [deftype+]]
    [io.github.humbleui.profile :as profile]
    [io.github.humbleui.protocols :as protocols :refer [IComponent -measure -draw -event]])
  (:import
    [java.lang AutoCloseable]
    [io.github.humbleui.types IPoint IRect Point Rect RRect]
    [io.github.humbleui.skija Canvas Font FontMetrics Paint TextLine]
    [io.github.humbleui.skija.shaper Shaper ShapingOptions]))

(set! *warn-on-reflection* true)

(defn draw [comp ctx rect canvas]
  (-draw comp ctx rect canvas))

(defn event [comp event]
  (-event comp event))

(defn- draw-child [comp ctx ^IRect rect ^Canvas canvas]
  (when comp
    (let [count (.getSaveCount canvas)]
      (try
        (-draw comp ctx rect canvas)
        (finally
          (.restoreToCount canvas count))))))

(defn- event-child [comp event]
  (when comp
    (-event comp event)))

(defn child-close [child]
  (when (instance? AutoCloseable child)
    (.close ^AutoCloseable child)))

(defn dimension ^long [size cs ctx]
  (let [scale (:scale ctx)]
    (->
      (if (fn? size)
        (* scale
          (size {:width  (/ (:width cs) scale)
                 :height (/ (:height cs) scale)
                 :scale  scale}))
        (* scale size))
      (math/round)
      (long))))

(defn round ^double [^double n ^double scale]
  (-> n
    (* scale)
    (math/round)
    (/ scale)))

(def ^:private ^Shaper shaper (Shaper/makeShapeDontWrapOrReorder))

(deftype+ Label [^String text ^Font font ^Paint paint ^TextLine line ^FontMetrics metrics]
  IComponent
  (-measure [_ ctx cs]
    (IPoint.
      (Math/ceil (.getWidth line))
      (Math/ceil (.getCapHeight metrics))))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (.drawTextLine canvas line (:x rect) (+ (:y rect) (Math/ceil (.getCapHeight metrics))) paint))
  
  (-event [_ event])
  
  AutoCloseable
  (close [_]
    #_(.close line))) ; TODO

(defn label [^String text ^Font font ^Paint paint & features]
  ; (profile/measure "label"
  (let [opts (reduce #(.withFeatures ^ShapingOptions %1 ^String %2) ShapingOptions/DEFAULT features)
        line (.shapeLine shaper text font ^ShapingOptions opts)]
    (->Label text font paint line (.getMetrics ^Font font))))

(deftype+ HAlign [child-coeff coeff child ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (-measure child ctx cs))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [layer      (.save canvas)
          child-size (-measure child ctx rect)
          left       (+ (:x rect)
                       (* (:width rect) coeff)
                       (- (* (:width child-size) child-coeff)))]
      (set! child-rect (IRect/makeXYWH left (:y rect) (:width child-size) (:height rect)))
      (draw-child child ctx child-rect canvas)))
  
  (-event [_ event]
    (event-child child event))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn halign
  ([coeff child] (halign coeff coeff child))
  ([child-coeff coeff child] (->HAlign child-coeff coeff child nil)))

(deftype+ VAlign [child-coeff coeff child ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (-measure child ctx cs))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [layer      (.save canvas)
          child-size (-measure child ctx rect)
          top        (+ (:y rect)
                       (* (:height rect) coeff)
                       (- (* (:height child-size) child-coeff)))]
      (set! child-rect (IRect/makeXYWH (:x rect) top (:width rect) (:height child-size)))
      (draw-child child ctx child-rect canvas)))
  
  (-event [_ event]
    (event-child child event))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn valign
  ([coeff child] (valign coeff coeff child))
  ([child-coeff coeff child] (->VAlign child-coeff coeff child nil)))

(deftype+ Width [value child ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (let [width'     (dimension value cs ctx)
          child-size (-measure child ctx (assoc cs :width width'))]
      (assoc child-size :width width')))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (draw-child child ctx child-rect canvas))
  
  (-event [_ event]
    (event-child child event))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn width [value child]
  (->Width value child nil))

(deftype+ Height [value child ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (let [height'    (dimension value cs ctx)
          child-size (-measure child ctx (assoc cs :height height'))]
      (assoc child-size :height height')))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (-draw child ctx rect canvas))
  
  (-event [_ event]
    (event-child child event))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn height [value child]
  (->Height value child nil))

(deftype+ Column [children ^:mut child-rects]
  IComponent
  (-measure [_ ctx cs]
    (reduce
      (fn [{:keys [width height]} child]
        (let [child-size (-measure child ctx cs)]
          (IPoint. (max width (:width child-size)) (+ height (:height child-size)))))
      (IPoint. 0 0)
      (keep #(nth % 2) children)))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [known   (for [[mode _ child] children]
                    (when (= :hug mode)
                      (-measure child ctx rect)))
          space   (- (:height rect) (transduce (keep :height) + 0 known))
          stretch (transduce (keep (fn [[mode value _]] (when (= :stretch mode) value))) + 0 children)
          layer   (.save canvas)]
      (loop [height   0
             rects    []
             known    known
             children children]
        (if-some [[mode value child] (first children)]
          (let [child-height (long
                               (case mode
                                 :hug     (:height (first known))
                                 :stretch (-> space (/ stretch) (* value) (math/round))))
                child-rect (IRect/makeXYWH (:x rect) (+ (:y rect) height) (:width rect) child-height)]
            (draw-child child ctx child-rect canvas)
            (recur
              (+ height child-height)
              (conj rects child-rect)
              (next known)
              (next children)))
          (set! child-rects rects)))))
  
  (-event [_ event]
    (reduce
      (fn [acc [_ _ child]]
        (core/eager-or acc (event-child child event)))
      false
      children))
  
  AutoCloseable
  (close [_]
    (doseq [[_ _ child] children]
      (child-close child))))

(defn- flatten-container [children]
  (into []
    (mapcat
      #(cond
         (nil? %)        []
         (vector? %)     [%]
         (sequential? %) (flatten-container %)
         :else           [[:hug nil %]]))
    children))

(defn column [& children]
  (->Column (flatten-container children) nil))

(deftype+ Row [children ^:mut child-rects]
  IComponent
  (-measure [_ ctx cs]
    (reduce
      (fn [{:keys [width height]} child]
        (let [child-size (-measure child ctx cs)]
          (IPoint. (+ width (:width child-size)) (max height (:height child-size)))))
      (IPoint. 0 0)
      (keep #(nth % 2) children)))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [known   (for [[mode _ child] children]
                    (when (= :hug mode)
                      (-measure child ctx rect)))
          space   (- (:width rect) (transduce (keep :width) + 0 known))
          stretch (transduce (keep (fn [[mode value _]] (when (= :stretch mode) value))) + 0 children)
          layer   (.save canvas)]
      (loop [width    0
             rects    []
             known    known
             children children]
        (if-some [[mode value child] (first children)]
          (let [child-size (case mode
                             :hug     (first known)
                             :stretch (IPoint. (-> space (/ stretch) (* value) (math/round)) (:height rect)))
                child-rect (IRect/makeXYWH (+ (:x rect) width) (:y rect) (:width child-size) (:height rect))]
            (draw-child child ctx child-rect canvas)
            (recur
              (+ width (long (:width child-size)))
              (conj rects )
              (next known)
              (next children)))
          (set! child-rects rects)))))
  
  (-event [_ event]
    (reduce
      (fn [acc [_ _ child]]
        (core/eager-or acc (event-child child event) false))
      false
      children))
  
  AutoCloseable
  (close [_]
    (doseq [[_ _ child] children]
      (child-close child))))

(defn row [& children]
  (->Row (flatten-container children) nil))

(defrecord Gap [width height]
  IComponent
  (-measure [_ ctx cs]
    (let [{:keys [scale]} ctx]
      (IPoint. (* scale width) (* scale height))))
  (-draw [_ ctx rect canvas])
  (-event [_ event]))

(defn gap [width height]
  (->Gap width height))

(deftype+ Padding [left top right bottom child ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (let [left'   (dimension left cs ctx)
          right'  (dimension right cs ctx)
          top'    (dimension top cs ctx)
          bottom' (dimension bottom cs ctx)
          child-cs   (IPoint. (- (:width cs) left' right') (- (:height cs) top' bottom'))
          child-size (-measure child ctx child-cs)]
      (IPoint.
        (+ (:width child-size) left' right')
        (+ (:height child-size) top' bottom'))))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [{:keys [scale]} ctx
          left'    (dimension left rect ctx)
          right'   (dimension right rect ctx)
          top'     (dimension top rect ctx)
          bottom'  (dimension bottom rect ctx)
          layer    (.save canvas)
          width'   (- (:width rect) left' right')
          height'  (- (:height rect) top' bottom')]
      (set! child-rect (IRect/makeXYWH (+ (:x rect) left') (+ (:y rect) top') width' height'))
      (draw-child child ctx child-rect canvas)))
  
  (-event [_ event]
    (event-child child event))
  
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
  
  (-draw [_ ctx ^IRect rect ^Canvas canvas]
    (set! child-rect rect)
    (.drawRect canvas (.toRect rect) paint)
    (draw-child child ctx child-rect canvas))
  
  (-event [_ event]
    (event-child child event))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn fill [paint child]
  (->Fill paint child nil))

(deftype+ Clip [child ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (-measure child ctx cs))
  
  (-draw [_ ctx ^IRect rect ^Canvas canvas]
    (let [layer (.save canvas)]
      (try
        (set! child-rect rect)
        (.clipRect canvas (.toRect rect))
        (-draw child ctx child-rect canvas)
        (finally
          (.restoreToCount canvas layer)))))
  
  (-event [_ event]
    (event-child child event))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn clip [child]
  (->Clip child nil))

(deftype+ ClipRRect [radii child ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (-measure child ctx cs))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [{:keys [scale]} ctx
          radii' (into-array Float/TYPE (map #(* scale %) radii))
          rrect  (RRect/makeComplexXYWH (:x rect) (:y rect) (:width rect) (:height rect) radii')
          layer  (.save canvas)]
      (try
        (set! child-rect rect)
        (.clipRRect canvas rrect true)
        (-draw child ctx child-rect canvas)
        (finally
          (.restoreToCount canvas layer)))))
  
  (-event [_ event]
    (event-child child event))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn clip-rrect [r child]
  (->ClipRRect [r] child nil))

(deftype+ Hoverable [child ^:mut child-rect ^:mut hovered?]
  IComponent
  (-measure [_ ctx cs]
    (-measure child ctx cs))
  
  (-draw [_ ctx rect canvas]
    (set! child-rect rect)
    (let [ctx' (cond-> ctx hovered? (assoc :hui/hovered? true))]
      (draw-child child ctx' child-rect canvas)))
  
  (-event [_ event]
    (core/eager-or
      (event-child child event)
      (let [type (:hui/event event)]
        (when (= :hui/mouse-move type)
          (let [pos (:hui.event/pos event)
                hovered?' (.contains ^IRect child-rect pos)]
            (when (not= hovered? hovered?')
              (set! hovered? hovered?')
              true))))))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn hoverable [child]
  (->Hoverable child nil false))

(deftype+ Clickable [on-click child ^:mut child-rect ^:mut hovered? ^:mut pressed?]
  IComponent
  (-measure [_ ctx cs]
    (-measure child ctx cs))
  
  (-draw [_ ctx rect canvas]
    (set! child-rect rect)
    (let [ctx' (cond-> ctx
                 hovered?                (assoc :hui/hovered? true)
                 (and pressed? hovered?) (assoc :hui/active? true))]
      (-draw child ctx' child-rect canvas)))
  
  (-event [_ event]
    (let [type (:hui/event event)]
      (core/eager-or
        (when (= :hui/mouse-move type)
          (let [pos (:hui.event/pos event)
                hovered?' (.contains ^IRect child-rect pos)]
            (when (not= hovered? hovered?')
              (set! hovered? hovered?')
              true)))
        (when (= :hui/mouse-button type)
          (let [pressed?' (if (:hui.event.mouse-button/is-pressed event)
                            hovered?
                            (do
                              (when (and pressed? hovered?) (on-click))
                              false))]
            (when (not= pressed? pressed?')
              (set! pressed? pressed?')
              true)))
        (event-child child event))))
  
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
  
  (-draw [_ ctx rect canvas]
    (let [child' (child-ctor ctx)]
      (when-not (identical? child child')
        (child-close child)
        (set! child child')))
    (set! child-rect rect)
    (draw-child child ctx child-rect canvas))
  
  (-event [_ event]
    (event-child child event))
  
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
  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (draw-child child (merge ctx data) child-rect canvas))
  
  (-event [_ event]
    (event-child child event))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn with-context [data child]
  (->WithContext data child nil))

(deftype+ WithBounds [key child ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (let [width  (-> (:width cs) (/ (:scale ctx)))
          height (-> (:height cs) (/ (:scale ctx)))]
      (-measure child (assoc ctx key (IPoint. width height)) cs)))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (let [width  (-> (:width rect) (/ (:scale ctx)))
          height (-> (:height rect) (/ (:scale ctx)))]
      (draw-child child (assoc ctx key (IPoint. width height)) child-rect canvas)))
  
  (-event [_ event]
    (event-child child event))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn with-bounds [key child]
  (->WithBounds key child nil))

(deftype+ VScroll [child ^:mut offset ^:mut size ^:mut child-size]
  IComponent
  (-measure [_ ctx cs]
    (let [child-cs (assoc cs :height Integer/MAX_VALUE)]
      (set! child-size (-measure child ctx child-cs))
      (set! offset (core/clamp offset (- (:height cs) (:height child-size)) 0))
      (IPoint. (:width child-size) (:height cs))))
  
  (-draw [_ ctx ^IRect rect ^Canvas canvas]
    (set! size rect)
    (let [layer      (.save canvas)
          child-rect (-> rect
                       (update :y + offset)
                       (assoc :height Integer/MAX_VALUE))]
      (try
        (.clipRect canvas (.toRect rect))
        (-draw child ctx child-rect canvas)
        (finally
          (.restoreToCount canvas layer)))))
  
  (-event [_ event]
    (let [changed? (not= 0 (:hui.event.mouse-scroll/dy event 0))]
      (when changed?
        (set! offset (-> offset
                       (+ (:hui.event.mouse-scroll/dy event))
                       (core/clamp (- (:height size) (:height child-size)) 0))))
      (core/eager-or
        changed?
        (event-child child event))))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn vscroll [child]
  (->VScroll child 0 nil nil))

(deftype+ VScrollbar [child ^Paint fill-track ^Paint fill-thumb ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (-measure child ctx cs))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (draw-child child ctx child-rect canvas)
    (let [{:keys [scale]} ctx
          content-y (- (:offset child))
          content-h (:height (:child-size child))
          scroll-y  (:y child-rect)
          scroll-h  (:height child-rect)
          scroll-r  (:right child-rect)
          
          padding (* 4 scale)
          track-w (* 4 scale)
          track-x (+ (:x rect) (:width child-rect) (- track-w) (- padding))
          track-y (+ (:y rect) scroll-y padding)
          track-h (- scroll-h (* 2 padding))
          track   (RRect/makeXYWH track-x track-y track-w track-h (* 2 scale))
          
          thumb-w       (* 4 scale)
          min-thumb-h   (* 16 scale)
          thumb-y-ratio (/ content-y content-h)
          thumb-y       (-> (* track-h thumb-y-ratio) (core/clamp 0 (- track-h min-thumb-h)) (+ (:y rect)) (+ track-y))
          thumb-b-ratio (/ (+ content-y scroll-h) content-h)
          thumb-b       (-> (* track-h thumb-b-ratio) (core/clamp min-thumb-h track-h) (+ track-y))
          thumb         (RRect/makeLTRB track-x thumb-y (+ track-x thumb-w) thumb-b (* 2 scale))]
      (.drawRRect canvas track fill-track)
      (.drawRRect canvas thumb fill-thumb)))

  (-event [_ event]
    (event-child child event))
  
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

(deftype+ CustomUI [width height on-paint on-event ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (IPoint. width height))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (when on-paint
      (let [layer  (.save canvas)]
        (try
          (.translate canvas (:x rect) (:y rect))
          (on-paint canvas width height)
          (finally
            (.restoreToCount canvas layer))))))
  
  (-event [_ event]
    (when on-event
      (on-event event))))

(defn custom-ui
  "(custom-ui 400 300 {:on-paint #'on-paint-impl
                       :on-event #'on-event-impl})"
  [width height {:keys [on-paint on-event]}]
  (->CustomUI width height on-paint on-event nil))

(deftype+ KeyListener [event-type callback child ^:mut child-rect]
  IComponent
  (-measure [_ ctx cs]
    (-measure child ctx cs))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (draw-child child ctx rect canvas))
  
  (-event [_ event]
    (core/eager-or
      (when (= event-type (:hui/event event))
        (callback event))
      (event-child child event)))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn on-key-down [callback child]
  (->KeyListener :hui/key-down callback child nil))

(defn on-key-up [callback child]
  (->KeyListener :hui/key-up callback child nil))

; (require 'user :reload)

(comment
  (do
    (println)
    (set! *warn-on-reflection* true)
    (require 'io.github.humbleui.ui :reload)))