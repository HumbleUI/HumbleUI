(ns examples.testbed
  (:require
    [io.github.humbleui.util :as util]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  #_[ui/align {:y :center}
     [ui/row
      ^{:stretch 1}
      [ui/rect {:paint (paint/fill 0xFFDDFFFF)}
       [ui/padding {:vertical 10}
        [ui/align {:x :center}
         [ui/label "123"]]]]
      ^{:stretch 1}
      [ui/rect {:paint (paint/fill 0xFFDDFFFF)}
       [ui/padding {:vertical 10}
        [ui/align {:x :center}
         [ui/label "123"]]]]]]
  [ui/align {:y :center}
   [ui/row
    ^{:stretch 1}
    [ui/rect {:paint (paint/fill 0xFFDDFFFF)}
     [ui/padding {:vertical 10}
      [ui/align {:x :center}
       [ui/label "{:stretch 1}"]]]]
    
    ^{:stretch 2}
    [ui/stack
     [ui/rect {:paint (paint/fill 0xFFFFDDFF)}
      [ui/padding {:vertical 10}
       [ui/align {:x :center}
        [ui/label "{:stretch 2}"]]]]
     [ui/align {:x :left}
       [ui/size {:width  #(* 0.5 (:width %))
                 :height 30}
        [ui/rect {:paint (paint/fill 0x20000000)}
         [ui/gap]]]]]
    
    [ui/size {:width 100}
     [ui/rect {:paint (paint/fill 0xFFDDFFDD)}
      [ui/padding {:vertical 10}
       [ui/align {:x :center}
        [ui/label "{:width 100}"]]]]]
    
    [ui/size {:width #(* 0.5 (:width %))}
     [ui/stack
      [ui/rect {:paint (paint/fill 0xFFFFFFDD)}
       [ui/padding {:vertical 10}
        [ui/align {:x :center}
         [ui/label "{:width 50%}"]]]]
      [ui/align {:x :left}
       [ui/size {:width  #(* 0.5 (:width %))
                 :height 30}
        [ui/rect {:paint (paint/fill 0x20000000)}
         [ui/gap]]]]]]
        
    ]])