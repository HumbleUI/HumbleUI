(ns examples.align
  (:require
    [clojure.string :as str]
    [examples.util :as util]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp label [text]
  [ui/rect {:paint (paint/fill 0xFFB2D7FE)}
   [ui/center
    [ui/padding {:padding 10}
     [ui/column {:gap 5}
      (seq (str/split text #"\n"))]]]])

(ui/defcomp box [child]
  (let [border (paint/stroke 0xFF0000FF (ui/scaled 1))]
    (fn [child]
      [ui/rect {:paint border}
       [ui/size {:width 100 :height 100}
        child]])))

(ui/defcomp ui []
  [ui/align {:y :center}
   [ui/vscrollbar
    [ui/align {:x :center}
     (let [fill (paint/fill 0xFFAACCFF)]
       (util/table
         "Component stretch by default"
         [box
          [label "abc"]]
         
         "Horizontal align left"
         [box
          [ui/align {:x 0}
           [label "abc"]]]
             
         "Horizontal align center"
         [box
          [ui/align {:x 0.5}
           [label "abc"]]]
             
         "Horizontal align right"
         [box
          [ui/align {:x 1}
           [label "abc"]]]
             
         "Horizontal align 0.2"
         [box
          [ui/align {:x 0.2}
           [label "abc"]]]
             
         "Horizontal align with keywords"
         [box
          [ui/column
           ^{:stretch 1}
           [ui/align {:x :left}
            [label "abc"]]
           ^{:stretch 1}
           [ui/align {:x :center}
            [label "abc"]]
           ^{:stretch 1}
           [ui/align {:x :right}
            [label "abc"]]]]
         
         "Align child’s center to container’s left"
         [box
          [ui/align {:x 0 :child-x 0.5}
           [label "abc"]]]
         
         "Align child’s left to container’s center"
         [box
          [ui/align {:x 0.5 :child-x 0}
           [label "abc"]]]
         
         "Vertical align top"
         [box
          [ui/align {:y 0}
           [label "abc"]]]
             
         "Vertical align center"
         [box
          [ui/align {:y 0.5}
           [label "abc"]]]
             
         "Vertical align bottom"
         [box
          [ui/align {:y 1}
           [label "abc"]]]
             
         "Vertical align 0.2"
         [box
          [ui/align {:y 0.2}
           [label "abc"]]]
             
         "Vertical align with keywords"
         [box
          [ui/row
           ^{:stretch 1}
           [ui/align {:y :top}
            [label "a\nb\nc"]]
           ^{:stretch 1}
           [ui/align {:y :center}
            [label "a\nb\nc"]]
           ^{:stretch 1}
           [ui/align {:y :bottom}
            [label "a\nb\nc"]]]]
         
         "Align child’s center to container’s top"
         [box
          [ui/align {:y 0 :child-y 0.5}
           [label "abc"]]]
         
         "Align child’s top to container’s center"
         [box
          [ui/align {:y 0.5 :child-y 0}
           [label "abc"]]]
         
         "Horizontal and vertical together"
         [box
          [ui/center
           [label "abc"]]]))]]])
