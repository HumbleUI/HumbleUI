(ns examples.backdrop
  (:require
    [clojure.math :as math]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Color ColorFilter ColorMatrix FilterTileMode ImageFilter]))

(defn blur [radius]
  (ImageFilter/makeBlur radius radius FilterTileMode/CLAMP))

(def grayscale
  (let [matrix (ColorMatrix.
                 (float-array
                   [0.21 0.72 0.07 0 0
                    0.21 0.72 0.07 0 0
                    0.21 0.72 0.07 0 0
                    0    0    0    1 0]))
        filter (ColorFilter/makeMatrix matrix)]
    (ImageFilter/makeColorFilter filter nil nil)))

(defn square [name filter color]
  (let [color (unchecked-int color)
        a     (Color/getA color)
        r     (Color/getR color)
        g     (Color/getG color)
        b     (Color/getB color)]
    [ui/clip {:radius 8}
     [ui/backdrop {:filter filter}
      [ui/stack
       [ui/rect {:paint (paint/fill color)}
        [ui/gap {:width 100, :height 100}]]
       [ui/center
        [ui/column {:gap 10}
         [ui/label name]
         [ui/label (format "Fill: #%02X%02X%02X" r g b)]
         [ui/label (format "Opacity: %d%%" (math/round (/ a 2.55)))]]]]]]))

(defn ui []
  [ui/stack
   [ui/center
    [ui/column {:gap 10}
     [ui/align {:x :center}
      [ui/label "Hello"]]
     [ui/align {:x :center}
      [ui/button {} "Click me"]]
     [ui/align {:x :center}
      [ui/checkbox {} "Toggle me"]]]]
   [ui/draggable {:pos (util/ipoint 10 10)}
    [ui/with-context {:fill-text (paint/fill 0xFFFFFFFF)}
     [square "Blur: 5" (blur 5) 0x40000000]]]
   [ui/draggable {:pos (util/ipoint 120 10)}
    [square "Blur: 10" (blur 10) 0x80FFFFFF]]
   [ui/draggable {:pos (util/ipoint 10 120)}
    [square "Blur: 20" (blur 20) 0x40CC3333]]
   [ui/draggable {:pos (util/ipoint 120 120)}
    [square "Grayscale" grayscale 0x80FFFFFF]]])
