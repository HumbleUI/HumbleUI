(ns examples.stack
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  [ui/center
   [ui/stack
    [ui/center
     [ui/rect {:paint (paint/fill 0xFFCCCCCC)}
      [ui/gap {:width 200 :height 200}]]]
    [ui/center
     [ui/padding {:padding 100}
      [ui/label "Stack"]]]
    [ui/center
     [ui/rect {:paint (paint/fill 0x80CC3333)}
      [ui/gap {:width 300 :height 100}]]]]])
