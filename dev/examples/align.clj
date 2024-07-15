(ns examples.align
  (:require
    [clojure.string :as str]
    [examples.util :as util]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp label [text]
  [ui/rect {:paint (paint/fill 0x80FFDB2C)}
   [ui/center
    [ui/padding {:padding 4}
     [ui/column {:gap 5}
      (seq (str/split text #"\n"))]]]])

(ui/defcomp box [child]
  (let [border (paint/stroke 0x40000000 (ui/scaled 1))]
    (fn [child]
      [ui/rect {:paint border}
       [ui/size {:width 100 :height 100}
        child]])))

(ui/defcomp ui []
  (util/table
    "Component stretch by default"
    [box
     [label "abc"]]
         
    "Horizontal align left"
    [box
     [ui/align {:x :left}
      [label "abc"]]]
             
    "Horizontal align center"
    [box
     [ui/align {:x :center}
      [label "abc"]]]
             
    "Horizontal align right"
    [box
     [ui/align {:x :right}
      [label "abc"]]]
             
    "Horizontal align with numbers 0..1"
    [box
     [ui/column
      ^{:stretch 1}
      [ui/align {:x 0}
       [label "abc"]]
      ^{:stretch 1}
      [ui/align {:x 0.25}
       [label "abc"]]
      ^{:stretch 1}
      [ui/align {:x 0.5}
       [label "abc"]]
      ^{:stretch 1}
      [ui/align {:x 0.75}
       [label "abc"]]
      ^{:stretch 1}
      [ui/align {:x 1}
       [label "abc"]]]]
         
    "Align child’s center to container’s left"
    [box
     [ui/align {:x :left :child-x :center}
      [label "abc"]]]
         
    "Align child’s left to container’s center"
    [box
     [ui/align {:x :center :child-x :left}
      [label "abc"]]]
        
    "Align child’s 0.2 to container’s 0.8"
    [box
     [ui/align {:x 0.8 :child-x 0.2}
      [label "abc"]]]
         
    "Vertical align top"
    [box
     [ui/align {:y :top}
      [label "abc"]]]
             
    "Vertical align center"
    [box
     [ui/align {:y :center}
      [label "abc"]]]
             
    "Vertical align bottom"
    [box
     [ui/align {:y :bottom}
      [label "abc"]]]
                          
    "Vertical align with numbers 0..1"
    [box
     [ui/row
      ^{:stretch 1}
      [ui/align {:y 0}
       [label "a\nb\nc"]]
      ^{:stretch 1}
      [ui/align {:y 0.25}
       [label "a\nb\nc"]]
      ^{:stretch 1}
      [ui/align {:y 0.5}
       [label "a\nb\nc"]]
      ^{:stretch 1}
      [ui/align {:y 0.75}
       [label "a\nb\nc"]]
      ^{:stretch 1}
      [ui/align {:y 1}
       [label "a\nb\nc"]]]]
         
    "Align child’s center to container’s top"
    [box
     [ui/align {:y :top :child-y :center}
      [label "abc"]]]
         
    "Align child’s top to container’s center"
    [box
     [ui/align {:y :center :child-y :top}
      [label "abc"]]]
         
    "Horizontal and vertical together"
    [box
     [ui/center
      [label "abc"]]]))
