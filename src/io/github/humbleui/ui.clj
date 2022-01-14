(ns io.github.humbleui.ui
  (:require
    [io.github.humbleui.core :as core :refer [deftype+]])
  (:import
    [java.lang AutoCloseable]
    [io.github.humbleui.types IPoint IRect Point Rect RRect]
    [io.github.humbleui.skija Canvas Font FontMetrics Paint TextLine]))

(set! *warn-on-reflection* true)

(defprotocol IComponent
  (-layout [_ ctx cs])
  (-draw   [_ ctx canvas])
  (-event  [_ event]))

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
  (-layout [_ ctx cs]
    (IPoint. (.getWidth line) (.getCapHeight metrics)))
  
  (-draw [_ ctx ^Canvas canvas]
    (.drawTextLine canvas line 0 (.getCapHeight metrics) paint))
  
  (-event [_ event])
  
  AutoCloseable
  (close [_]
    #_(.close line))) ; TODO

(defn label [text font paint]
  (Label. text font paint (TextLine/make text font) (.getMetrics ^Font font)))

(deftype+ HAlign [coeff child-coeff child ^:mut child-rect]
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
  
  (-draw [_ ctx ^Canvas canvas]
    (let [layer (.save canvas)]
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

(deftype+ VAlign [coeff child-coeff child ^:mut child-rect]
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
  
  (-draw [_ ctx ^Canvas canvas]
    (let [layer (.save canvas)]
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

(defn valign
  ([coeff child] (valign coeff coeff child))
  ([coeff child-coeff child] (VAlign. coeff child-coeff child nil)))

;; figure out align
(deftype+ Column [children ^:mut child-rects]
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
            (max width (int (:width child-size)))
            (+ height (int (:height child-size)))
            (conj rects (IRect/makeXYWH 0 height (:width child-size) (:height child-size)))
            (next children)))
        (do
          (set! child-rects rects)
          (IPoint. width height)))))
  
  (-draw [_ ctx ^Canvas canvas]
    (doseq [[child rect] (map vector children child-rects)]
      (let [layer (.save canvas)]
        (try
          (.translate canvas (:x rect) (:y rect))
          (-draw child ctx canvas)
          (finally
            (.restoreToCount canvas layer))))))
  
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

(deftype+ Row [children ^:mut child-rects]
  IComponent
  (-layout [_ ctx cs]
    (loop [width    0
           height   0
           rects    []
           children children]
      (if children
        (let [child      (first children)
              remainder  (- (:width cs) width)
              child-cs   (IPoint. remainder (:height cs))
              child-size (-layout child ctx child-cs)]
          (recur
            (+ width (int (:width child-size)))
            (max height (int (:height child-size)))
            (conj rects (IRect/makeXYWH width 0 (:width child-size) (:height child-size)))
            (next children)))
        (do
          (set! child-rects rects)
          (IPoint. width height)))))
  
  (-draw [_ ctx ^Canvas canvas]
    (doseq [[child rect] (map vector children child-rects)]
      (let [layer (.save canvas)]
        (try
          (.translate canvas (:x rect) (:y rect))
          (-draw child ctx canvas)
          (finally
            (.restoreToCount canvas layer))))))
  
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

(defn row [& children]
  (Row. (vec children) nil))

(defrecord Gap [width height]
  IComponent
  (-layout [_ ctx cs]
    (IPoint. width height))
  (-draw [_ ctx canvas])
  (-event [_ event]))

(defn gap [width height]
  (Gap. width height))

(deftype+ Padding [left top right bottom child ^:mut child-rect]
  IComponent
  (-layout [_ ctx cs]
    (let [child-cs   (IPoint. (- (:width cs) left right) (- (:height cs) top bottom))
          child-size (-layout child ctx child-cs)]
      (set! child-rect (IRect/makeXYWH left top (:width child-size) (:height child-size)))
      (IPoint.
        (+ (:width child-size) left right)
        (+ (:height child-size) top bottom))))
  
  (-draw [_ ctx ^Canvas canvas]
    (let [layer (.save canvas)]
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

(deftype+ Fill [paint child ^:mut child-rect]
  IComponent
  (-layout [_ ctx cs]
    (let [child-size (-layout child ctx cs)]
      (set! child-rect (IRect/makeXYWH 0 0 (:width child-size) (:height child-size)))
      child-size))
  
  (-draw [_ ctx ^Canvas canvas]
    (.drawRect canvas (Rect/makeXYWH 0 0 (:width child-rect) (:height child-rect)) paint)
    (-draw child ctx canvas))
  
  (-event [_ event]
    (event-propagate event child child-rect))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn fill [paint child]
  (Fill. paint child nil))

(deftype+ ClipRRect [radii child ^:mut child-rect]
  IComponent
  (-layout [_ ctx cs]
    (let [child-size (-layout child ctx cs)]
      (set! child-rect (IRect/makeXYWH 0 0 (:width child-size) (:height child-size)))
      child-size))
  
  (-draw [_ ctx ^Canvas canvas]
    (let [layer  (.save canvas)
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

(deftype+ Hoverable [child ^:mut child-rect ^:mut hovered?]
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

(deftype+ Clickable [on-click
                    child
                    ^:mut child-rect
                    ^:mut hovered?
                    ^:mut pressed?]
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

(deftype+ Contextual [child-ctor
                     ^:mut child
                     ^:mut child-rect]
  IComponent
  (-layout [_ ctx cs]
    (let [child' (child-ctor ctx)]
      (when-not (identical? child child')
        (child-close child)
        (set! child child'))
      (let [child-size (-layout child ctx cs)]
        (set! child-rect (IRect/makeXYWH 0 0 (:width child-size) (:height child-size)))
        child-size)))
  
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

(deftype+ VScroll [child ^:mut offset ^:mut size ^:mut child-size]
  IComponent
  (-layout [_ ctx cs]
    (set! child-size (-layout child ctx cs))
    (set! size cs)
    (set! offset (core/clamp offset (- (:height size) (:height child-size)) 0))
    (Point. (:width child-size) (:height cs)))
  
  (-draw [_ ctx ^Canvas canvas]
    (let [layer (.save canvas)]
      (try
        (.clipRect canvas (Rect/makeXYWH 0 0 (:width size) (:height size)))
        (.translate canvas 0 offset)
        (-draw child ctx canvas)
        (finally
          (.restoreToCount canvas layer)))))
  
  (-event [_ event]
    (let [changed?   (not= 0 (:hui.event.mouse-scroll/dy event 0))
          _          (when changed?
                       (set! offset (-> offset
                                      (+ (:hui.event.mouse-scroll/dy event))
                                      (core/clamp (- (:height size) (:height child-size)) 0))))
          child-rect (IRect/makeLTRB 0 0 (min (:width child-size) (:width size)) (min (:height child-size) (:height size)))]
      (core/eager-or changed? (event-propagate event child child-rect (Point. 0 offset)))))
  
  AutoCloseable
  (close [_]
    (child-close child)))

(defn vscroll [child]
  (VScroll. child 0 nil nil))


(comment
  (do
    (println)
    (set! *warn-on-reflection* true)
    (require 'io.github.humbleui.ui :reload)))