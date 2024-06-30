(ns examples.testbed
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp shadow [*s]
  [ui/shadow {:blur @*s}
   [ui/rect {:paint (paint/fill 0x80FFFFFF)}
    [ui/text-field]]])

(ui/defcomp ui []
  (let [*s (signal/signal 0)]
    [ui/center
     [ui/column {:gap 10}
      [ui/label *s]
      [shadow *s]
      [ui/button {:on-click (fn [_] (swap! *s inc))} "Inc"]
      [ui/button {:on-click (fn [_] (swap! *s dec))} "Dec"]]]))