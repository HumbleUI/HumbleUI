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
      
      "Color space"
      [ui/column
       (for [color ["F00" "0F0" "00F" "FF0" "F0F" "0FF"]]
         [ui/size {:width 100}
          [ui/row
           (for [cs [nil :srgb :display-p3]]
             ^{:stretch 1}
             [ui/rect {:paint {:fill color
                               :color-space cs}}
              [ui/size {:height 25}]])]])]

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
