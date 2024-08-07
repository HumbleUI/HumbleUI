(ns examples.settings
  (:require
    [examples.shared :as shared]
    [io.github.humbleui.debug :as debug]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]))

(defn setting [name signal]
  [ui/row
   [ui/align {:y :center}
    [ui/label name]]
   ^{:stretch 1} [ui/gap {:width 20}]
   [ui/switch {:*value signal}]])

(ui/defcomp ui []
  (let [{:keys [window scale]} ui/*ctx*
        padding-inner  12
        fill-bg        (paint/fill 0xFFF2F2F2)
        stroke-bg      (paint/stroke 0xFFE0E0E0 (* 0.5 scale))
        fill-delimiter (paint/fill 0xFFE7E7E7)
        delimeter      [ui/rect {:paint fill-delimiter}
                        [ui/gap {:height 1}]]]
    [ui/align {:y :center}
     [ui/vscroll
      [ui/align {:x :center}
       [ui/padding {:padding 20}
        [ui/rect {:radius 6, :paint fill-bg}
         [ui/rect {:radius 6, :paint stroke-bg}
          [ui/padding {:padding padding-inner}
           [ui/column {:gap padding-inner}
            (interpose delimeter
              (list
                [setting "Always on top" shared/*floating?]
                [setting "Frame paint time" debug/*paint?]
                [setting "Frame pacing" debug/*pacing?]
                [setting "Event pacing" debug/*events?]
                [setting "Outlines" debug/*outlines?]
                [setting "Continuous render" debug/*continuous-render?]))]]]]]]]]))
