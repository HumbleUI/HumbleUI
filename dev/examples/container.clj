(ns examples.container
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp label [text]
  [ui/rect {:paint (paint/fill 0xFFB2D7FE)}
   [ui/center
    [ui/padding {:padding 10}
     [ui/label text]]]])

(ui/defcomp ui []
  [ui/center
   [ui/column {:gap 10}
    [ui/padding {:top 10}
     [ui/label "Hug"]]
    [ui/row {:gap 10}
     [label "Ok"]
     [label "Cancel"]
     [label "Abort request"]]
          
    [ui/padding {:top 10}
     [ui/label "Stretch 1-1-1"]]
    [ui/row {:gap 10}
     ^{:stretch 1} [label "Ok"]
     ^{:stretch 1} [label "Cancel"]
     ^{:stretch 1} [label "Abort request"]]
          
    [ui/padding {:top 10}
     [ui/label "Stretch 3-2-1"]]
    [ui/row {:gap 10}
     ^{:stretch 3} [label "Ok"]
     ^{:stretch 2} [label "Cancel"]
     ^{:stretch 1} [label "Abort request"]]
          
    [ui/padding {:top 10}
      [ui/label "Hug 20%-30%-40%"]]
    [ui/row {:gap 10}
      [ui/size {:width #(* 0.2 (:width %))} [label "Ok"]]
      [ui/size {:width #(* 0.3 (:width %))} [label "Cancel"]]
      [ui/size {:width #(* 0.4 (:width %))} [label "Abort request"]]]
    ]])
