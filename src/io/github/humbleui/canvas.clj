(ns io.github.humbleui.canvas
  (:import
    [io.github.humbleui.types IRect Rect RRect]
    [io.github.humbleui.skija Canvas Font Paint]))

(defn draw-line
  ([^Canvas canvas p1 p2 ^Paint paint]
   (.drawLine canvas (:x p1) (:y p1) (:x p2) (:y p2) paint))
  ([^Canvas canvas x1 y1 x2 y2 ^Paint paint]
   (.drawLine canvas x1 y1 x2 y2 paint)))

(defn draw-rect [^Canvas canvas r ^Paint paint]
  (condp instance? r
    IRect (.drawRect  canvas (.toRect ^IRect r) paint)
    RRect (.drawRRect canvas r paint)
    Rect  (.drawRect  canvas r paint)))

(defn draw-circle [^Canvas canvas x y r ^Paint paint]
  (.drawCircle canvas x y r paint))

(defn clear [^Canvas canvas color]
  (.clear canvas (unchecked-int color)))

(defn draw-string [^Canvas canvas s x y ^Font font ^Paint paint]
  (.drawString canvas (str s) x y font paint))

(defn clip-rect [^Canvas canvas r]
  (condp instance? r
    IRect (.clipRect canvas (.toRect ^IRect r))
    RRect (.clipRRect canvas ^RRect r true)
    Rect  (.clipRect canvas r)))

(defn translate [^Canvas canvas dx dy]
  (.translate canvas dx dy))

(defn scale
  ([^Canvas canvas s]
   (.scale canvas s s))
  ([^Canvas canvas sx sy]
   (.scale canvas sx sy)))

(defn rotate [^Canvas canvas deg]
  (.rotate canvas deg))

(defn skew [^Canvas canvas sx sy]
  (.skew canvas sx sy))

(defn save [^Canvas canvas]
  (.save canvas))

(defn save-count [^Canvas canvas]
  (.getSaveCount canvas))

(defn restore
  ([^Canvas canvas]
   (.restore canvas))
  ([^Canvas canvas count]
   (.restoreToCount canvas count)))

(defmacro with-canvas [canvas & body]
  `(let [canvas# ~canvas
         count# (save canvas#)]
     (try
       ~@body
       (finally
         (restore canvas# count#)))))