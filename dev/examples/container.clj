(ns examples.container
  (:require
    [io.github.humbleui.ui :as ui]))

(ui/defcomp label [text]
  [ui/rect {:paint {:fill 0xFFB2D7FE}}
   [ui/center
    [ui/padding {:padding 10}
     [ui/label text]]]])

(ui/defcomp ui []
  [ui/align {:y :center}
   [ui/vscroll
    [ui/padding {:padding 20}
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
      
      
      [ui/padding {:top 10}
       [ui/label "Container size"]]
      [ui/row
       ^{:stretch 1}
       [ui/rect {:paint {:fill 0xFFDDFFFF}}
        [ui/padding {:vertical 10}
         [ui/align {:x :center}
          [ui/label "{:stretch 1}"]]]]
    
       ^{:stretch 2}
       [ui/stack
        [ui/rect {:paint {:fill 0xFFFFDDFF}}
         [ui/padding {:vertical 10}
          [ui/align {:x :center}
           [ui/label "{:stretch 2}"]]]]
        [ui/align {:x :left}
         [ui/size {:width  #(* 0.5 (:width %))
                   :height 30}
          [ui/rect {:paint {:fill 0x20000000}}
           [ui/gap]]]]]
    
       [ui/size {:width 100}
        [ui/rect {:paint {:fill 0xFFDDFFDD}}
         [ui/padding {:vertical 10}
          [ui/align {:x :center}
           [ui/label "{:width 100}"]]]]]
    
       [ui/size {:width #(* 0.5 (:width %))}
        [ui/stack
         [ui/rect {:paint {:fill 0xFFFFFFDD}}
          [ui/padding {:vertical 10}
           [ui/align {:x :center}
            [ui/label "{:width 50%}"]]]]
         [ui/align {:x :left}
          [ui/size {:width  #(* 0.5 (:width %))
                    :height 30}
           [ui/rect {:paint {:fill 0x20000000}}
            [ui/gap]]]]]]]
      
      [ui/padding {:top 10}
       [ui/label "Dynamic Stretch"]]
      [ui/align {:x :left}
       [ui/column
        [ui/row
         [ui/rect {:paint {:fill 0xFFFFDDFF}}
          [ui/padding {:padding 10}
           [ui/align {:x :center}
            [ui/label ":hug"]]]]
         ^{:stretch 1}
         [ui/rect {:paint {:fill 0xFFFFFFDD}}
          [ui/padding {:padding 10}
           [ui/align {:x :center}
            [ui/label "{:stretch 1}"]]]]
         [ui/rect {:paint {:fill 0xFFDDFFFF}}
          [ui/padding {:padding 10}
           [ui/align {:x :center}
            [ui/label ":hug"]]]]]
        [ui/row
         [ui/rect {:paint {:fill 0xFFFFDDFF}}
          [ui/padding {:padding 10}
           [ui/align {:x :center}
            [ui/label ":hug long"]]]]
         ^{:stretch 1}
         [ui/rect {:paint {:fill 0xFFFFFFDD}}
          [ui/padding {:padding 10}
           [ui/align {:x :center}
            [ui/label "{:stretch 1}"]]]]
         [ui/rect {:paint {:fill 0xFFDDFFFF}}
          [ui/padding {:padding 10}
           [ui/align {:x :center}
            [ui/label ":hug long"]]]]]]]
      
      ]]]])
