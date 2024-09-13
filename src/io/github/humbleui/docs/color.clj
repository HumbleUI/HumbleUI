(ns io.github.humbleui.docs.color
  (:require
    [clojure.string :as str]
    [io.github.humbleui.docs.shared :as shared]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
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
            
    "RGB float array, 0.0 .. 1.0 x 3"
    [ui/rect {:paint {:fill [1 0.87 0.13]}}
     [ui/size {:width 100 :height 50}]]
      
    "RGBA float array, 0.0 .. 1.0 x 4"
    [ui/rect {:paint {:fill [1 0.87 0.13 0.5]}}
     [ui/size {:width 100 :height 50}]]

    "ARGB unsigned int (Skia native)"
    [ui/rect {:paint {:fill 0x80FFDD22}}
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
    
    "sRGB"
    [ui/column
     (for [blue  [0 0.25 0.5 0.75 1]
           green [0 0.25 0.5 0.75 1]]
       [ui/row
        (for [red [0 0.25 0.5 0.75 1]]
          [ui/rect {:paint {:fill [red green blue]}}
           [ui/size {:width 20 :height 10}]])])]
    
    "Display-P3"
    [ui/column
     (for [blue  [0 0.25 0.5 0.75 1]
           green [0 0.25 0.5 0.75 1]]
       [ui/row
        (for [red [0 0.25 0.5 0.75 1]]
          [ui/rect {:paint {:fill [red green blue]
                            :model :display-p3}}
           [ui/size {:width 20 :height 10}]])])]
    
    "OkLCH"
    [ui/column
     (for [l    [0 0.25 0.5 0.75 1]
           c    [0 0.1 0.2 0.3 0.4]]
       [ui/row
        (for [h [0 72 144 216 288]]
          [ui/rect {:paint {:fill [l c h]
                            :model :oklch}}
           [ui/size {:width 20 :height 10}]])])]
      
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
            [ui/size {:width 10 :height 25}]])])]]))