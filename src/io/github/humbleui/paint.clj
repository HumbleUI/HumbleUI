(ns io.github.humbleui.paint
  (:import
    [io.github.humbleui.skija ImageFilter MaskFilter Paint PaintMode]))

(defn fill ^Paint [color]
  (doto (Paint.)
    (.setColor (unchecked-int color))))

(defn stroke ^Paint [color width]
  (doto (Paint.)
    (.setColor (unchecked-int color))
    (.setMode PaintMode/STROKE)
    (.setStrokeWidth width)))

(defn set-mask-filter ^Paint [^Paint p ^MaskFilter f]
  (.setMaskFilter p f))

(defn set-image-filter ^Paint [^Paint p ^ImageFilter f]
  (.setImageFilter p f))