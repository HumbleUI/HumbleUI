(ns io.github.humbleui.paint
  (:import
    [io.github.humbleui.skija Paint PaintMode]))

(defn ^Paint fill [color]
  (doto (Paint.)
    (.setColor (unchecked-int color))))

(defn ^Paint stroke [color width]
  (doto (Paint.)
    (.setColor (unchecked-int color))
    (.setMode PaintMode/STROKE)
    (.setStrokeWidth width)))