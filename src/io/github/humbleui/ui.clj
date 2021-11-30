(ns io.github.humbleui.ui
  (:import
   [java.lang
    AutoCloseable]
   [io.github.humbleui.skija
    Canvas
    Font
    Paint
    TextLine]))

(defrecord Size [width height])

(defprotocol IComponent
  (-measure [_ ^Size size])
  (-draw [_ ^Canvas canvas ^Size size]))

(deftype Label [^String text ^Font font ^Paint paint ^:unsynchronized-mutable ^TextLine line]
  IComponent
  (-measure [_ size]
    (when (nil? line)
      (set! line (TextLine/make text font)))
    (Size. (.getWidth line) (.getHeight line)))

  (-draw [_ canvas size]
    (when (nil? line)
      (set! line (TextLine/make text font)))
    (.drawTextLine ^Canvas canvas line 0 0 paint))

  AutoCloseable
  (close [_]
    (when (some? line)
      (.close line))))

(defn label [text font paint]
  (Label. text font paint nil))

(deftype HAlign [coeff child-coeff child]
  IComponent
  (-measure [_ size]
    (let [child-size (-measure child size)]
      (Size. (.-width ^Size size) (.-height ^Size child-size))))

  (-draw [_ canvas size]
    (let [child-size (-measure child size)
          layer      (.save ^Canvas canvas)]
      (.translate ^Canvas canvas (- (* (.-width ^Size size) coeff) (* (.-width ^Size child-size) child-coeff)) 0)
      (-draw child canvas child-size)
      (.restoreToCount ^Canvas canvas layer)))

  AutoCloseable
  (close [_]
    (.close ^AutoCloseable child)))

(defn halign
  ([coeff child] (halign coeff coeff child))
  ([coeff child-coeff child] (HAlign. coeff child-coeff child)))

(deftype VAlign [coeff child-coeff child]
  IComponent
  (-measure [_ size]
    (let [child-size (-measure child size)]
      (Size. (.-width ^Size child-size) (.-height ^Size size))))

  (-draw [_ canvas size]
    (let [child-size (-measure child size)
          layer      (.save ^Canvas canvas)]
      (.translate ^Canvas canvas 0 (- (* (.-height ^Size size) coeff) (* (.-height ^Size child-size) child-coeff)))
      (-draw child canvas child-size)
      (.restoreToCount ^Canvas canvas layer)))

  AutoCloseable
  (close [_]
    (.close ^AutoCloseable child)))

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
              child-size (-measure child (Size. (.-width ^Size size) remainder))]
          (recur
            (max width (.-width ^Size child-size))
            (+ height (.-height ^Size child-size))
            (next children)))
        (Size. width height))))

  (-draw [_ canvas size]
    (let [layer (.save ^Canvas canvas)]
      (loop [remainder (.-height ^Size size)
             children  children]
        (when children
          (let [child      (first children)
                child-size (-measure child (Size. (.-width ^Size size) remainder))]
            (-draw child canvas child-size)
            (.translate ^Canvas canvas 0 (.-height child-size))
            (recur (- remainder (.-height child-size)) (next children)))))
      (.restoreToCount ^Canvas canvas layer)))

  AutoCloseable
  (close [_]
    (doseq [child children]
      (.close ^AutoCloseable child))))

(defn column [& children]
  (Column. (vec children)))

(comment
  (do
    (println)
    (set! *warn-on-reflection* true)
    (require 'io.github.humbleui.ui :reload)))