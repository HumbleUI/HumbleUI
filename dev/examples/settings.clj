(ns examples.settings
  (:require
    [examples.shared :as shared]
    [io.github.humbleui.debug :as debug]
    [io.github.humbleui.ui :as ui]))

(defn setting [name signal]
  [ui/row
   [ui/align {:y :center}
    [ui/label name]]
   ^{:stretch 1} [ui/gap {:width 20}]
   [ui/switch {:*value signal}]])

(ui/defcomp ui []
  [ui/align {:y :center}
   [ui/vscroll
    [ui/align {:x :center}
     [ui/padding {:padding 20}
      [ui/rect {:radius 6, :paint {:fill 0xFFF2F2F2}}
       [ui/rect {:radius 6, :paint {:stroke 0xFFE0E0E0, :width 0.5}}
        [ui/padding {:padding 12}
         [ui/column {:gap [ui/padding {:vertical 12}
                           [ui/rect {:paint {:fill 0xFFE7E7E7}}
                            [ui/gap {:height 1}]]]}
          [setting "Always on top" shared/*floating?]
          [setting "Frame paint time" debug/*paint?]
          [setting "Frame pacing" debug/*pacing?]
          [setting "Event pacing" debug/*events?]
          [setting "Outlines" debug/*outlines?]
          [setting "Continuous render" debug/*continuous-render?]]]]]]]]])
