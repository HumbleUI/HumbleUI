(ns io.github.humbleui.ui
  (:require
   [io.github.humbleui.core :as hui])
  (:import
   [java.lang AutoCloseable]
   [io.github.humbleui.core Size]
   [io.github.humbleui.skija Canvas Font FontMetrics Paint Rect RRect TextLine]))

(defprotocol IComponent
  (-measure [_ ctx size])
  (-draw    [_ ctx canvas size]))

(defn ctx-resolve [ctx obj]
  (if (fn? obj)
    (obj ctx)
    obj))

(deftype Label [^String text ^Font font ^Paint paint *line *metrics]
  IComponent
  (-measure [_ ctx size]
    (hui/->Size (.getWidth ^TextLine @*line) (.getCapHeight ^FontMetrics @*metrics)))

  (-draw [_ ctx canvas size]
    (.drawTextLine ^Canvas canvas ^TextLine @*line 0 (.getCapHeight ^FontMetrics @*metrics) paint))

  AutoCloseable
  (close [_]
    (when (realized? *line)
      (.close ^AutoCloseable @*line))))

(defn label [text font paint]
  (Label. text font paint (delay (TextLine/make text font)) (delay (.getMetrics ^Font font))))

(deftype HAlign [coeff child-coeff child]
  IComponent
  (-measure [_ ctx size]
    (let [child-size (-measure child ctx size)]
      (hui/->Size (:width size) (:height child-size))))

  (-draw [_ ctx canvas size]
    (let [child-size (-measure child ctx size)
          layer      (.save ^Canvas canvas)]
      (.translate ^Canvas canvas (- (* (:width size) coeff) (* (:width child-size) child-coeff)) 0)
      (-draw child ctx canvas child-size)
      (.restoreToCount ^Canvas canvas layer)))

  AutoCloseable
  (close [_]
    (when (instance? AutoCloseable child)
      (.close ^AutoCloseable child))))

(defn halign
  ([coeff child] (halign coeff coeff child))
  ([coeff child-coeff child] (HAlign. coeff child-coeff child)))

(deftype VAlign [coeff child-coeff child]
  IComponent
  (-measure [_ ctx size]
    (let [child-size (-measure child ctx size)]
      (hui/->Size (:width child-size) (:height size))))

  (-draw [_ ctx canvas size]
    (let [child-size (-measure child ctx size)
          layer      (.save ^Canvas canvas)]
      (.translate ^Canvas canvas 0 (- (* (:height size) coeff) (* (:height child-size) child-coeff)))
      (-draw child ctx canvas child-size)
      (.restoreToCount ^Canvas canvas layer)))

  AutoCloseable
  (close [_]
    (when (instance? AutoCloseable child)
      (.close ^AutoCloseable child))))

(defn valign
  ([coeff child] (valign coeff coeff child))
  ([coeff child-coeff child] (VAlign. coeff child-coeff child)))

(defrecord Column [children]
  IComponent
  (-measure [_ ctx size]
    (loop [width    0
           height   0
           children children]
      (if children
        (let [child      (first children)
              remainder  (- (:height size) height)
              child-size (-measure child ctx (hui/->Size (:width size) remainder))]
          (recur
            (max width (:width child-size))
            (+ height (:height child-size))
            (next children)))
        (hui/->Size width height))))

  (-draw [_ ctx canvas size]
    (let [layer (.save ^Canvas canvas)]
      (loop [remainder (:height size)
             children  children]
        (when children
          (let [child      (first children)
                child-size (-measure child ctx (hui/->Size (:width size) remainder))]
            (-draw child ctx canvas child-size)
            (.translate ^Canvas canvas 0 (.-height child-size))
            (recur (- remainder (.-height child-size)) (next children)))))
      (.restoreToCount ^Canvas canvas layer)))

  AutoCloseable
  (close [_]
    (doseq [child children
            :when (instance? AutoCloseable child)]
      (.close ^AutoCloseable child))))

(defn column [& children]
  (Column. (vec children)))

(defrecord Gap [width height]
  IComponent
  (-measure [_ ctx size]
    (hui/->Size width height))
  (-draw [_ ctx canvas size]))

(defn gap [width height]
  (Gap. width height))

(defrecord Padding [left top right bottom child]
  IComponent
  (-measure [_ ctx size]
    (let [child-cs   (hui/->Size (- (:width size) left right) (- (:height size) top bottom))
          child-size (-measure child ctx child-cs)]
      (hui/->Size
        (+ (:width child-size) left right)
        (+ (:height child-size) top bottom))))

  (-draw [_ ctx canvas size]
    (let [canvas     ^Canvas canvas
          child-size (hui/->Size (- (:width size) left right) (- (:height size) top bottom))
          layer      (.save canvas)]
      (try
        (.translate canvas left top)
        (-draw child ctx canvas child-size)
        (finally
          (.restoreToCount canvas layer)))))

  AutoCloseable
  (close [_]
    (when (instance? AutoCloseable child)
      (.close ^AutoCloseable child))))

(defn padding
  ([p child] (Padding. p p p p child))
  ([w h child] (Padding. w h w h child))
  ([l t r b child] (Padding. l t r b child))) 

(defrecord FillSolid [paint child]
  IComponent
  (-measure [_ ctx size]
    (-measure child ctx size))

  (-draw [_ ctx canvas size]
    (.drawRect canvas (Rect/makeXYWH 0 0 (:width size) (:height size)) (ctx-resolve ctx paint))
    (-draw child ctx canvas size))

  AutoCloseable
  (close [_]
    (when (instance? AutoCloseable child)
      (.close ^AutoCloseable child))))

(defn fill-solid [paint child]
  (FillSolid. paint child))

(defrecord ClipRRect [radii child]
  IComponent
  (-measure [_ ctx size]
    (-measure child ctx size))

  (-draw [_ ctx canvas size]
    (let [canvas ^Canvas canvas
          layer  (.save canvas)
          rrect  (RRect/makeComplexXYWH 0 0 (:width size) (:height size) radii)]
      (try
        (.clipRRect canvas rrect true)
        (-draw child ctx canvas size)
        (finally
          (.restoreToCount canvas layer)))))

  AutoCloseable
  (close [_]
    (when (instance? AutoCloseable child)
      (.close ^AutoCloseable child))))

(defn clip-rrect
  ([r child] (ClipRRect. (into-array Float/TYPE [r]) child)))

(comment
  (do
    (println)
    (set! *warn-on-reflection* true)
    (require 'io.github.humbleui.ui :reload)))