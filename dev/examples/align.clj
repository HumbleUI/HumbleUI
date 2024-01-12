(ns examples.align
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp label [text]
  [ui/rect {:paint (paint/fill 0xFFB2D7FE)}
   [ui/center
    [ui/padding {:padding 10}
     [ui/label text]]]])

(ui/defcomp ui []
  [ui/valign {:position 0.5}
   [ui/row
    ^{:stretch 1}
    [ui/gap]
    
    ^{:stretch 2}
    [ui/rect {:paint (paint/fill 0xFFE1EFFA)}
     [ui/column {:gap 1}
      ^{:stretch 1}
      [ui/halign {:position 1
                  :child-position 0}
       [label "Right to left (1 0)"]]
                   
      ^{:stretch 1}
      [ui/halign {:position 0.5
                  :child-position 0}
       [label "Center to left (0.5 0)"]]
    
      ^{:stretch 1}
      [ui/halign {:position 0.6
                  :child-position 0.2}
       [label "Arbitrary (0.6 0.2)"]]
   
      ^{:stretch 1}
      [ui/halign {:position 0}
       [label "Left to left (0 0)"]]
  
      ^{:stretch 1}
      [ui/halign {:position 1
                  :child-position 0.5}
       [label "Right to center (1 0.5)"]]
  
      ^{:stretch 1}
      [ui/halign {:position 0.5}
       [label "Center to center (0.5 0.5)"]]
  
      ^{:stretch 1}
      [ui/halign {:position 0
                  :child-position 0.5}
       [label "Left to center (0 0.5)"]]
  
      ^{:stretch 1}
      [ui/halign {:position 1}
       [label "Right to right (1 1)"]]
  
      ^{:stretch 1}
      [ui/halign {:position 0.5
                  :child-position 1}
       [label "Center to right (0.5 1)"]]
  
      ^{:stretch 1}
      [ui/halign {:position 0
                  :child-position 1}
       [label "Left to right (0 1)"]]
  
      ^{:stretch 1}
      [label "Stretch"]]]
    
    ^{:stretch 1}
    [ui/gap]
    
    ^{:stretch 2}
    [ui/rect {:paint (paint/fill 0xFFE1EFFA)}
     [ui/row {:gap 1}
      ^{:stretch 1}
      [ui/valign {:position 1
                  :child-position 0}
       [label "Bottom to top"]]

      ^{:stretch 1}
      [ui/valign {:position 0.5
                  :child-position 0}
       [label "Middle to top"]]

      ^{:stretch 1}
      [ui/valign {:position 0}
       [label "Top to top"]]

      ^{:stretch 1}
      [ui/valign {:position 1
                  :child-position 0.5}
       [label "Bottom to middle"]]

      ^{:stretch 1}
      [ui/valign {:position 0.5}
       [label "Middle to middle"]]

      ^{:stretch 1}
      [ui/valign {:position 0
                  :child-position 0.5}
       [label "Top to middle"]]

      ^{:stretch 1}
      [ui/valign {:position 1}
       [label "Bottom to bottom"]]

      ^{:stretch 1}
      [ui/valign {:position 0.5
                  :child-position 1}
       [label "Middle to bottom"]]

      ^{:stretch 1}
      [ui/valign {:position 0
                  :child-position 1}
       [label "Top to bottom"]]

      ^{:stretch 1}
      [label "Stretch"]]]
    
    ^{:stretch 1}
    [ui/gap]]])
