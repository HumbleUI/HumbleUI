(ns examples.stack
  (:require
        [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  [ui/align {:y :center}
   [ui/vscroll
    [ui/align {:x :center}
     [ui/padding {:padding 20}
      [ui/stack
       [ui/center
        [ui/rect {:paint {:fill 0xFFCCCCCC}}
         [ui/gap {:width 200 :height 200}]]]
       [ui/center
        [ui/padding {:padding 100}
         [ui/label "Stack"]]]
       [ui/center
        [ui/rect {:paint {:fill 0x80CC3333}}
         [ui/gap {:width 300 :height 100}]]]]]]]])
