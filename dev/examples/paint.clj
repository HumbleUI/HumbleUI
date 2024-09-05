(ns examples.paint
  (:require
    [clojure.string :as str]
    [examples.shared :as shared]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  (fn []
    (shared/table
      "RGB color"
      [ui/rect {:paint {:fill "FD2"}}
       [ui/size {:width 100 :height 50}]]
            
      "RRGGBB color"
      [ui/rect {:paint {:fill "FFDD22"}}
       [ui/size {:width 100 :height 50}]]
      
      "RRGGBBAA color"
      [ui/rect {:paint {:fill "FFDD2280"}}
       [ui/size {:width 100 :height 50}]]
      
      "RGB int array, 0..255 x 3"
      [ui/rect {:paint {:fill [255 221 34]}}
       [ui/size {:width 100 :height 50}]]
      
      "RGBA int array, 0..255 x 4"
      [ui/rect {:paint {:fill [255 221 34 127]}}
       [ui/size {:width 100 :height 50}]]
      
      "RGB float array, 0.0 .. 1.0 x 3"
      [ui/rect {:paint {:fill [1 0.87 0.13]}}
       [ui/size {:width 100 :height 50}]]
      
      "RGBA float array, 0.0 .. 1.0 x 4"
      [ui/rect {:paint {:fill [1 0.87 0.13 0.5]}}
       [ui/size {:width 100 :height 50}]]
      
      "Out of range sRGB"
      [ui/size {:width 100}
       [ui/column
        (for [[c1 c2] [[[1.0 0 0] [ 1.09 -0.23 -0.15]]
                       [[0 1.0 0] [-0.51  1.02 -0.31]]
                       [[0 0 1.0] [ 0.00  0.00  1.04]]]]
          [ui/row
           ^{:stretch 1}
           [ui/rect {:paint {:fill c1}}
            [ui/size {:height 50}]]
           ^{:stretch 1}
           [ui/rect {:paint {:fill c2}}
            [ui/size {:height 50}]]])]]
      
      "Color space"
      [ui/size {:width 100}
       [ui/row
        (for [tuple [[:srgb
                      ["F00" "0F0" "00F" "FF0" "F0F" "0FF"]]
                     [:oklch
                      [[0.63 0.26  29]
                       [0.87 0.29 142]
                       [0.45 0.31 264]
                       [0.97 0.21 110]
                       [0.70 0.32 328]
                       [0.91 0.15 195]]]
                     [:display-p3
                      ["F00" "0F0" "00F" "FF0" "F0F" "0FF"]]
                     [:oklch
                      [[0.64 0.29  28]
                       [0.84 0.36 145]
                       [0.46 0.32 264]
                       [0.96 0.24 110]
                       [0.72 0.36 331]
                       [0.89 0.20 193]]]]
              :let [[model colors] tuple]]
          ^{:stretch 1}
          [ui/column
           (for [color colors]
             [ui/rect {:paint {:fill color, :model model}}
              [ui/size {:width 10 :height 25}]])])]]

      "ARGB unsigned int (Skia native)"
      [ui/rect {:paint {:fill 0x80FFDD22}}
       [ui/size {:width 100 :height 50}]]

      "Stroke"
      [ui/rect {:paint {:stroke "0088FF80"}}
       [ui/size {:width 100 :height 50}]]
      
      "Stroke width"
      [ui/rect {:paint {:stroke "0088FF"
                        :width  6}}
       [ui/size {:width 100 :height 50}]]
      
      "Multiple paints"
      [ui/rect {:paint [{:fill   "FFDD22"}
                        {:stroke "0088FF", :width 6}]}
       [ui/size {:width 100 :height 50}]]
      
      "Multiple paints, reverse order"
      [ui/rect {:paint [{:stroke "0088FF", :width 6}
                        {:fill "FFDD22"}]}
       [ui/size {:width 100 :height 50}]]
      
      "Multiple paints on text"
      [ui/label {:font-cap-height 30
                 :paint [{:stroke "0088FF", :width 3}
                         {:fill   "FFDD22"}]}
       "Andy"]
      
      "Instanced paint"
      (let [paint (ui/paint {:fill "FFDD22"})]
        [ui/rect {:paint paint}
         [ui/size {:width 100 :height 50}]])
      
      "Stroke join"
      [ui/size {:width 100}
       [ui/row {:gap 20}
        (for [join [:miter :round :bevel]]
          ^{:stretch 1}
          [ui/rect {:paint {:stroke "0088FF"
                            :width  10
                            :join   join}}
           [ui/size {:height 50}]])]]
      
      )))
