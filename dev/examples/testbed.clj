(ns examples.testbed
  (:require
    [io.github.humbleui.util :as util]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  [ui/grid {:cols [:hug {:stretch 1} :hug]
            :rows [:hug {:stretch 1} :hug]
            :col-gap 20
            :row-gap 10}
   [ui/rect {:paint {:fill [0.80 0.25   0] :model :oklch}} [ui/gap {:width 40 :height 40}]]
   [ui/rect {:paint {:fill [0.80 0.25  30] :model :oklch}} [ui/gap {:width 40 :height 40}]]
   [ui/rect {:paint {:fill [0.80 0.25  60] :model :oklch}} [ui/gap {:width 40 :height 40}]]
   
   [ui/rect {:paint {:fill [0.80 0.25  90] :model :oklch}} [ui/gap {:width 40 :height 40}]]
   [ui/rect {:paint {:fill [0.80 0.25 120] :model :oklch}} [ui/gap {:width 40 :height 40}]]
   [ui/rect {:paint {:fill [0.80 0.25 150] :model :oklch}} [ui/gap {:width 40 :height 40}]]
   
   ^{:col-span 2}
   [ui/rect {:paint {:fill [0.80 0.25 180] :model :oklch}} [ui/gap {:width 40 :height 40}]]
   [ui/rect {:paint {:fill [0.80 0.25 210] :model :oklch}} [ui/gap {:width 40 :height 40}]]
   
   [ui/rect {:paint {:fill [0.80 0.25 240] :model :oklch}} [ui/gap {:width 40 :height 40}]]
   ^{:col-span 2}
   [ui/rect {:paint {:fill [0.80 0.25 270] :model :oklch}} [ui/gap {:width 40 :height 40}]]
   
   ^{:col-span 3}
   [ui/rect {:paint {:fill [0.80 0.25 300] :model :oklch}} [ui/gap {:width 40 :height 40}]]])
                   