(ns io.github.humbleui.ui
  (:require
   [io.github.humbleui.core :as hui])
  (:import
   [java.lang AutoCloseable]
   [io.github.humbleui.core Size]
   [io.github.humbleui.skija Canvas Font FontMetrics Paint TextLine]))

(defprotocol IComponent
  (-measure [_ size])
  (-draw [_ canvas size]))

(deftype Label [^String text ^Font font ^Paint paint *line *metrics]
  IComponent
  (-measure [_ size]
    (hui/->Size (.getWidth ^TextLine @*line) (.getCapHeight ^FontMetrics @*metrics)))

  (-draw [_ canvas size]
    (.drawTextLine ^Canvas canvas ^TextLine @*line 0 (.getCapHeight ^FontMetrics @*metrics) paint))

  AutoCloseable
  (close [_]
    (when (realized? *line)
      (.close ^AutoCloseable @*line))))

(defn label [text font paint]
  (Label. text font paint (delay (TextLine/make text font)) (delay (.getMetrics ^Font font))))

(deftype HAlign [coeff child-coeff child]
  IComponent
  (-measure [_ size]
    (let [child-size (-measure child size)]
      (hui/->Size (.-width ^Size size) (.-height ^Size child-size))))

  (-draw [_ canvas size]
    (let [child-size (-measure child size)
          layer      (.save ^Canvas canvas)]
      (.translate ^Canvas canvas (- (* (.-width ^Size size) coeff) (* (.-width ^Size child-size) child-coeff)) 0)
      (-draw child canvas child-size)
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
  (-measure [_ size]
    (let [child-size (-measure child size)]
      (hui/->Size (.-width ^Size child-size) (.-height ^Size size))))

  (-draw [_ canvas size]
    (let [child-size (-measure child size)
          layer      (.save ^Canvas canvas)]
      (.translate ^Canvas canvas 0 (- (* (.-height ^Size size) coeff) (* (.-height ^Size child-size) child-coeff)))
      (-draw child canvas child-size)
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
  (-measure [_ size]
    (loop [width    0
           height   0
           children children]
      (if children
        (let [child      (first children)
              remainder  (- (.-height ^Size size) height)
              child-size (-measure child (hui/->Size (.-width ^Size size) remainder))]
          (recur
            (max width (.-width ^Size child-size))
            (+ height (.-height ^Size child-size))
            (next children)))
        (hui/->Size width height))))

  (-draw [_ canvas size]
    (let [layer (.save ^Canvas canvas)]
      (loop [remainder (.-height ^Size size)
             children  children]
        (when children
          (let [child      (first children)
                child-size (-measure child (hui/->Size (.-width ^Size size) remainder))]
            (-draw child canvas child-size)
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
  (-measure [_ size]
    (hui/->Size width height))
  (-draw [_ canvas size]))

(defn gap [width height]
  (Gap. width height))

(comment
  (do
    (println)
    (set! *warn-on-reflection* true)
    (require 'io.github.humbleui.ui :reload)))