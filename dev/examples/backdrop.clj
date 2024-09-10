(ns examples.backdrop
  (:require
    [clojure.math :as math]
    [io.github.humbleui.util :as util]
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

(defn square [pos name filter color]
  (let [*pos (ui/signal pos)]
    (fn [_ name filter color]
      (let [color (unchecked-int color)
            a     (Color/getA color)
            r     (Color/getR color)
            g     (Color/getG color)
            b     (Color/getB color)]
        [ui/align {:x :left :y :top}
         [ui/padding {:left (:x @*pos)
                      :top  (:y @*pos)}
          [ui/draggable
           {:on-drag (fn [e]
                       (swap! *pos
                         (fn [pos]
                           (-> pos
                             (update :x + (-> e :delta-last :x))
                             (update :y + (-> e :delta-last :y))))))}
           [ui/clip {:radius 8}
            [ui/backdrop {:filter filter}
             [ui/stack
              [ui/rect {:paint {:fill color}}
               [ui/gap {:width 100, :height 100}]]
              [ui/center
               [ui/column {:gap 10}
                [ui/label name]
                [ui/label (format "Fill: #%02X%02X%02X" r g b)]
                [ui/label (format "Opacity: %d%%" (math/round (/ a 2.55)))]]]]]]]]]))))

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
   [ui/with-context {:fill-text {:fill 0xFFFFFFFF}}
    [square (util/ipoint 10 10) "Blur: 5" (blur 5) 0x40000000]]
   [square (util/ipoint 120 10) "Blur: 10" (blur 10) 0x80FFFFFF]
   [square (util/ipoint 10 120) "Blur: 20" (blur 20) 0x40CC3333]
   [square (util/ipoint 120 120) "Grayscale" grayscale 0x80FFFFFF]])
