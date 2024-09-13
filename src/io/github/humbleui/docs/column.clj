(ns io.github.humbleui.docs.column
  (:require
    [clojure.string :as str]
    [io.github.humbleui.docs.shared :as shared]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp label
  ([text]
   [label text "FFDD22"])
  ([text color]
   [ui/rect {:paint {:fill color}}
    [ui/center
     [ui/padding {:padding 10}
      [ui/label text]]]]))

(ui/defcomp ui []
  (shared/table
    "Just column"
    [ui/column
     [ui/rect {:paint {:fill [0.7 0.18 100] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]
     [ui/rect {:paint {:fill [0.8 0.18 100] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]
     [ui/rect {:paint {:fill [0.9 0.18 100] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]]
    
    "With a gap"
    [ui/column {:gap 10}
     [ui/rect {:paint {:fill [0.7 0.18 100] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]
     [ui/rect {:paint {:fill [0.8 0.18 100] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]
     [ui/rect {:paint {:fill [0.9 0.18 100] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]]
    
    "Gap can be an element"
    [ui/column {:gap [ui/rect {:paint
                               {:fill [0.9 0.18 100]
                                :model :oklch}}
                      [ui/gap {:height 10}]]}
     [ui/rect {:paint {:fill [0.7 0 100] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]
     [ui/rect {:paint {:fill [0.8 0 100] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]
     [ui/rect {:paint {:fill [0.9 0 100] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]]
    
    "Stretches split leftover space"
    [ui/size {:height 200}
     [ui/column
      [ui/rect {:paint {:fill [0.7 0 100] :model :oklch}}
       [ui/gap {:width 100 :height 30}]]
      ^{:stretch 1}
      [ui/rect {:paint {:fill [0.8 0.18 100] :model :oklch}}
       [ui/gap {:width 100}]]
      ^{:stretch 1}
      [ui/rect {:paint {:fill [0.9 0.18 100] :model :oklch}}
       [ui/gap {:width 100}]]]]
    
    "Proportional to their value"
    [ui/size {:height 200}
     [ui/column
      [ui/rect {:paint {:fill [0.7 0 100] :model :oklch}}
       [ui/gap {:width 100 :height 30}]]
      ^{:stretch 1}
      [ui/rect {:paint {:fill [0.8 0.18 100] :model :oklch}}
       [ui/gap {:width 100}]]
      ^{:stretch 10}
      [ui/rect {:paint {:fill [0.9 0.18 100] :model :oklch}}
       [ui/gap {:width 100}]]]]
    
    "Percentage-size is counted from parent before stretch"
    [ui/size {:height 200}
     [ui/column
      [ui/rect {:paint {:fill [0.7 0 250] :model :oklch}}
       [ui/gap {:width 100 :height 30}]]
      ^{:stretch 1}
      [ui/rect {:paint {:fill [0.8 0 250] :model :oklch}}
       [ui/gap {:width 100}]]
      [ui/rect {:paint {:fill [0.9 0.2 100] :model :oklch}}
       [ui/gap {:height #(* 0.5 (:height %))}]]]]
    
    "Sequences are okay"
    [ui/column
     (for [lightness (range 0.5 1 0.05)]
       [ui/rect {:paint {:fill [lightness 0.18 100] :model :oklch}}
        [ui/gap {:width 100 :height 10}]])]
    
    "Column stretches everything to its widest children"
    [ui/column
     [label "Ok" "B99E00"]
     [label "Cancel" "DABE02"]
     [label "Abort request" "FFDD22"]]
    
    "Use align to align"
    [ui/column
     [ui/align {:x :left}
      [label "Ok" "B99E00"]]
     [ui/align {:x :left}
      [label "Cancel" "DABE02"]]
     [ui/align {:x :left}
      [label "Abort request" "FFDD22"]]]
    
    "Different aligns can be mixed in same container"
    [ui/column
     [ui/align {:x :left}
      [label "Left" "CAAE01"]]
     [ui/align {:x :center}
      [label "Center" "DABE02"]]
     [ui/align {:x :right}
      [label "Right" "E9CF01"]]
     [label "Stretch" "FBDF27"]
     [label "Longer Stretch" "FFF044"]]
    
    "Default align"
    [ui/column {:align :center}
     [label "Left" "CAAE01"]
     [label "Center" "DABE02"]
     [label "Right" "E9CF01"]
     [label "Stretch" "FBDF27"]
     [label "Longer Stretch" "FFF044"]]))
