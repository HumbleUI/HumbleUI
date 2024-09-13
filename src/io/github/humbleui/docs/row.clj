(ns io.github.humbleui.docs.row
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

(ui/defcomp stack [label]
  [ui/stack
   label
   [ui/align {:x :left}
    [ui/size {:width  #(* 0.5 (:width %))}
     [ui/rect {:paint {:fill 0x20000000}}
      [ui/gap {:height 30}]]]]])

(ui/defcomp ui []
  (shared/table
    "Just column"
    [ui/row
     [ui/rect {:paint {:fill [0.7 0.18 100] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]
     [ui/rect {:paint {:fill [0.8 0.18 100] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]
     [ui/rect {:paint {:fill [0.9 0.18 100] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]]
    
    "With a gap"
    [ui/row {:gap 10}
     [ui/rect {:paint {:fill [0.7 0.18 100] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]
     [ui/rect {:paint {:fill [0.8 0.18 100] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]
     [ui/rect {:paint {:fill [0.9 0.18 100] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]]
    
    "Gap can be an element"
    [ui/row {:gap [ui/rect {:paint
                            {:fill [0.9 0.18 100]
                             :model :oklch}}
                   [ui/gap {:width 10}]]}
     ^{:stretch 1}
     [ui/rect {:paint {:fill [0.7 0 100] :model :oklch}}
      [ui/gap {:height 30}]]
     ^{:stretch 1}
     [ui/rect {:paint {:fill [0.8 0 100] :model :oklch}}
      [ui/gap {:height 30}]]
     ^{:stretch 1}
     [ui/rect {:paint {:fill [0.9 0 100] :model :oklch}}
      [ui/gap {:height 30}]]]
    
    "Stretches split leftover space"
    [ui/row
     [ui/rect {:paint {:fill [0.7 0 100] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]
     ^{:stretch 1}
     [ui/rect {:paint {:fill [0.8 0.18 100] :model :oklch}}
      [ui/gap {:height 30}]]
     ^{:stretch 1}
     [ui/rect {:paint {:fill [0.9 0.18 100] :model :oklch}}
      [ui/gap {:height 30}]]]
    
    "Proportional to their value"
    [ui/row
     [ui/rect {:paint {:fill [0.7 0 100] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]
     ^{:stretch 1}
     [ui/rect {:paint {:fill [0.8 0.18 100] :model :oklch}}
      [ui/gap {:height 30}]]
     ^{:stretch 10}
     [ui/rect {:paint {:fill [0.9 0.18 100] :model :oklch}}
      [ui/gap {:height 30}]]]
    
    "Percentage-size is counted from parent before stretch"
    [ui/row
     [ui/rect {:paint {:fill [0.7 0 250] :model :oklch}}
      [ui/gap {:width 100 :height 30}]]
     ^{:stretch 1}
     [ui/rect {:paint {:fill [0.8 0 250] :model :oklch}}
      [ui/gap {:height 30}]]
     [ui/rect {:paint {:fill [0.9 0.2 100] :model :oklch}}
      [ui/gap {:width #(* 0.5 (:width %))
               :height 30}]]]
    
    "Sequences are okay"
    [ui/row
     (for [lightness (range 0.5 1 0.05)]
       ^{:stretch 1}
       [ui/rect {:paint {:fill [lightness 0.18 100] :model :oklch}}
        [ui/gap {:height 30}]])]
    
    "Row stretches everything to its tallest children"
    [ui/row
     [ui/rect {:paint {:fill "B99E00"}}
      [ui/gap {:width 30 :height 30}]]
     [ui/rect {:paint {:fill "DABE02"}}
      [ui/gap {:width 30 :height 60}]]
     [ui/rect {:paint {:fill "FFDD22"}}
      [ui/gap {:width 30 :height 90}]]]
    
    "Use align to align"
    [ui/row
     [ui/align {:y :top}
      [ui/rect {:paint {:fill "B99E00"}}
       [ui/gap {:width 30 :height 30}]]]
     [ui/align {:y :top}
      [ui/rect {:paint {:fill "DABE02"}}
       [ui/gap {:width 30 :height 60}]]]
     [ui/align {:y :top}
      [ui/rect {:paint {:fill "FFDD22"}}
       [ui/gap {:width 30 :height 90}]]]]
    
    "Different aligns can be mixed in same container"
    [ui/row
     [ui/align {:y :top}
      [ui/rect {:paint {:fill "CAAE01"}}
       [ui/gap {:width 30 :height 30}]]]
     [ui/align {:y :center}
      [ui/rect {:paint {:fill "DABE02"}}
       [ui/gap {:width 30 :height 60}]]]
     [ui/align {:y :bottom}
      [ui/rect {:paint {:fill "E9CF01"}}
       [ui/gap {:width 30 :height 90}]]]
     [ui/rect {:paint {:fill "FBDF27"}}
      [ui/gap {:width 30 :height 120}]]
     [ui/rect {:paint {:fill "FFF044"}}
      [ui/gap {:width 30 :height 150}]]]
    
    "Default align"
    [ui/row {:align :center}
     [ui/rect {:paint {:fill "CAAE01"}}
      [ui/gap {:width 30 :height 30}]]
     [ui/rect {:paint {:fill "DABE02"}}
      [ui/gap {:width 30 :height 60}]]
     [ui/rect {:paint {:fill "E9CF01"}}
      [ui/gap {:width 30 :height 90}]]
     [ui/rect {:paint {:fill "FBDF27"}}
      [ui/gap {:width 30 :height 120}]]
     [ui/rect {:paint {:fill "FFF044"}}
      [ui/gap {:width 30 :height 150}]]]
    
    "Column elements set size root for children"
    [ui/row
     ^{:stretch 1}
     [stack
      [label "s/1" "DDFFFF"]]
    
     ^{:stretch 2}
     [stack
      [label "s/2" "FFDDFF"]]
    
     [ui/size {:width 100}
      [stack
       [label "100px" "DDFFDD"]]]
    
     [ui/size {:width #(* 0.5 (:width %))}
      [stack
       [label "50%" "FFFFDD"]]]]
      
    "Using stretch for left/right layout"
    [ui/align {:x :left}
     [ui/column
      [ui/row
       [label ":hug" "FD2"]
       ^{:stretch 1}
       [label "s/1" "EEE"]
       [label ":hug" "FD2"]]
      [ui/row
       [label ":hug long" "FD2"]
       ^{:stretch 1}
       [label "s/1" "EEE"]
       [label ":hug long" "FD2"]]]]))