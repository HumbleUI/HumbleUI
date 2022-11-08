(ns io.github.humbleui.paint
  (:import
    [io.github.humbleui.skija Paint PaintMode]))

(defn fill ^Paint [color]
  (doto (Paint.)
    (.setColor (unchecked-int color))))

(defn stroke ^Paint [color width]
  (doto (Paint.)
    (.setColor (unchecked-int color))
    (.setMode PaintMode/STROKE)
    (.setStrokeWidth width)))