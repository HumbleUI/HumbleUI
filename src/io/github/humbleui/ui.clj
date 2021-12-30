(ns io.github.humbleui.ui
  (:require
   [io.github.humbleui.core :as core])
  (:import
   [java.lang AutoCloseable]
   [io.github.humbleui.types IPoint IRect Point Rect RRect]
   [io.github.humbleui.skija Canvas Font FontMetrics Paint TextLine]))

(defprotocol IComponent
  (-layout [_ ctx cs])
  (-draw   [_ ctx canvas])
  (-event  [_ event]))

(defn event-propagate [event child child-rect]
  (let [pos    (:hui.event/pos event)
        event' (cond
                 (nil? pos)
                 event
                 
                 (not (.contains ^IRect child-rect pos))
                 (dissoc event :hui.event/pos)

                 (= 0 (:x child-rect) (:y child-rect))
                 event

                 :else
                 (assoc event :hui.event/pos
                   (IPoint. (- (:x pos) (:x child-rect)) (- (:y pos) (:y child-rect)))))]
    (-event child event')))

(defn child-close [child]
  (when (instance? AutoCloseable child)
    (.close ^AutoCloseable child)))

(deftype Label [^String text ^Font font ^Paint paint ^TextLine line ^FontMetrics metrics]
  IComponent
  (-layout [_ ctx cs]
    (IPoint. (.getWidth line) (.getCapHeight metrics)))

  (-draw [_ ctx canvas]
    (.drawTextLine ^Canvas canvas line 0 (.getCapHeight metrics) paint))

  (-event [_ event])

  AutoCloseable
  (close [_]
    (.close line)))

(defn label [text font paint]
  (Label. text font paint (TextLine/make text font) (.getMetrics ^Font font)))

(deftype HAlign [coeff child-coeff child ^:unsynchronized-mutable child-rect]
  IComponent
  (-layout [_ ctx cs]
    (let [child-size (-layout child ctx cs)]
      (set! child-rect
        (IRect/makeXYWH
          (- (* (:width cs) coeff) (* (:width child-size) child-coeff))
          0
          (:width child-size)
          (:height child-size)))
      (IPoint. (:width cs) (:height child-size))))

  (-draw [_ ctx canvas]
    (let [canvas ^Canvas canvas
          layer (.save ^Canvas canvas)]
      (try
        (.translate canvas (:x child-rect) (:y child-rect))
        (-draw child ctx canvas)
        (finally
          (.restoreToCount canvas layer)))))
  
  (-event [_ event]
    (event-propagate event child child-rect))

  AutoCloseable
  (close [_]
    (child-close child)))

(defn halign
  ([coeff child] (halign coeff coeff child))
  ([coeff child-coeff child] (HAlign. coeff child-coeff child nil)))

(deftype VAlign [coeff child-coeff child ^:unsynchronized-mutable child-rect]
  IComponent
  (-layout [_ ctx cs]
    (let [child-size (-layout child ctx cs)]
      (set! child-rect
        (IRect/makeXYWH
          0
          (- (* (:height cs) coeff) (* (:height child-size) child-coeff))
          (:width child-size)
          (:height child-size)))
      (IPoint. (:width child-size) (:height cs))))

  (-draw [_ ctx canvas]
    (let [canvas ^Canvas canvas
          layer (.save ^Canvas canvas)]
      (try
        (.translate canvas (:x child-rect) (:y child-rect))
        (-draw child ctx canvas)
        (finally
          (.restoreToCount ^Canvas canvas layer)))))

  (-event [_ event]
    (event-propagate event child child-rect))

  AutoCloseable
  (close [_]
    (child-close child)))

(defn valign
  ([coeff child] (valign coeff coeff child))
  ([coeff child-coeff child] (VAlign. coeff child-coeff child nil)))

;; figure out align
(deftype Column [children ^:unsynchronized-mutable child-rects]
  IComponent
  (-layout [_ ctx cs]
    (loop [width    0
           height   0
           rects    []
           children children]
      (if children
        (let [child      (first children)
              remainder  (- (:height cs) height)
              child-cs   (IPoint. (:width cs) remainder)
              child-size (-layout child ctx child-cs)]
          (recur
            (max width (:width child-size))
            (+ height (:height child-size))
            (conj rects (IRect/makeXYWH 0 height (:width child-size) (:height child-size)))
            (next children)))
        (do
          (set! child-rects rects)
          (IPoint. width height)))))

  (-draw [_ ctx canvas]
    (doseq [[child rect] (map vector children child-rects)]
      (let [layer (.save ^Canvas canvas)]
        (try
          (.translate ^Canvas canvas (:x rect) (:y rect))
          (-draw child ctx canvas)
          (finally
            (.restoreToCount ^Canvas canvas layer))))))

  (-event [_ event]
    (reduce
      (fn [acc [child rect]]
        (core/eager-or acc (event-propagate event child rect)))
      false
      (map vector children child-rects)))

  AutoCloseable
  (close [_]
    (doseq [child children]
      (child-close child))))

(defn column [& children]
  (Column. (vec children) nil))

(defrecord Gap [width height]
  IComponent
  (-layout [_ ctx cs]
    (IPoint. width height))
  (-draw [_ ctx canvas])
  (-event [_ event]))

(defn gap [width height]
  (Gap. width height))

(deftype Padding [left top right bottom child ^:unsynchronized-mutable child-rect]
  IComponent
  (-layout [_ ctx cs]
    (let [child-cs   (IPoint. (- (:width cs) left right) (- (:height cs) top bottom))
          child-size (-layout child ctx child-cs)]
      (set! child-rect (IRect/makeXYWH left top (:width child-size) (:height child-size)))
      (IPoint.
        (+ (:width child-size) left right)
        (+ (:height child-size) top bottom))))

  (-draw [_ ctx canvas]
    (let [canvas ^Canvas canvas
          layer  (.save canvas)]
      (try
        (.translate canvas left top)
        (-draw child ctx canvas)
        (finally
          (.restoreToCount canvas layer)))))
  
  (-event [_ event]
    (event-propagate event child child-rect))

  AutoCloseable
  (close [_]
    (child-close child)))

(defn padding
  ([p child] (Padding. p p p p child nil))
  ([w h child] (Padding. w h w h child nil))
  ([l t r b child] (Padding. l t r b child nil)))

(deftype FillSolid [paint child ^:unsynchronized-mutable child-rect]
  IComponent
  (-layout [_ ctx cs]
    (let [child-size (-layout child ctx cs)]
      (set! child-rect (IRect/makeXYWH 0 0 (:width child-size) (:height child-size)))
      child-size))

  (-draw [_ ctx canvas]
    (.drawRect canvas (Rect/makeXYWH 0 0 (:width child-rect) (:height child-rect)) paint)
    (-draw child ctx canvas))

  (-event [_ event]
    (event-propagate event child child-rect))

  AutoCloseable
  (close [_]
    (child-close child)))

(defn fill-solid [paint child]
  (FillSolid. paint child nil))

(deftype ClipRRect [radii child ^:unsynchronized-mutable child-rect]
  IComponent
  (-layout [_ ctx cs]
    (let [child-size (-layout child ctx cs)]
      (set! child-rect (IRect/makeXYWH 0 0 (:width child-size) (:height child-size)))
      child-size))

  (-draw [_ ctx canvas]
    (let [canvas ^Canvas canvas
          layer  (.save canvas)
          rrect  (RRect/makeComplexXYWH 0 0 (:width child-rect) (:height child-rect) radii)]
      (try
        (.clipRRect canvas rrect true)
        (-draw child ctx canvas)
        (finally
          (.restoreToCount canvas layer)))))

  (-event [_ event]
    (event-propagate event child child-rect))

  AutoCloseable
  (close [_]
    (child-close child)))

(defn clip-rrect
  ([r child] (ClipRRect. (into-array Float/TYPE [r]) child nil)))

(deftype Hoverable [child ^:unsynchronized-mutable child-rect ^:unsynchronized-mutable hovered?]
  IComponent
  (-layout [_ ctx cs]
    (let [ctx'       (cond-> ctx hovered? (assoc :hui/hovered? true))
          child-size (-layout child ctx' cs)]
      (set! child-rect (IRect/makeXYWH 0 0 (:width child-size) (:height child-size)))
      child-size))

  (-draw [_ ctx canvas]
    (-draw child ctx canvas))

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
  (Hoverable. child nil false))

(deftype Clickable [on-click
                    child
                    ^:unsynchronized-mutable child-rect
                    ^:unsynchronized-mutable hovered?
                    ^:unsynchronized-mutable pressed?]
  IComponent
  (-layout [_ ctx cs]
    (let [ctx'       (cond-> ctx
                       hovered?                (assoc :hui/hovered? true)
                       (and pressed? hovered?) (assoc :hui/active? true))
          child-size (-layout child ctx' cs)]
      (set! child-rect (IRect/makeXYWH 0 0 (:width child-size) (:height child-size)))
      child-size))

  (-draw [_ ctx canvas]
    (-draw child ctx canvas))

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
  (Clickable. on-click child nil false false))

(deftype Contextual [child-ctor
                     ^:unsynchronized-mutable child
                     ^:unsynchronized-mutable child-rect]
  IComponent
  (-layout [_ ctx cs]
    (set! child (child-ctor ctx))
    (let [child-size (-layout child ctx cs)]
      (set! child-rect (IRect/makeXYWH 0 0 (:width child-size) (:height child-size)))
      child-size))

  (-draw [_ ctx canvas]
    (-draw child ctx canvas))

  (-event [_ event]
    (event-propagate event child child-rect))

  AutoCloseable
  (close [_]
    (child-close child)))

(defn contextual [child-ctor]
  (Contextual. child-ctor nil nil))

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

(comment
  (do
    (println)
    (set! *warn-on-reflection* true)
    (require 'io.github.humbleui.ui :reload)))