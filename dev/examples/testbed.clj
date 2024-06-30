(ns examples.testbed
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  (let [*s (signal/signal 0)]
    [ui/center
     [ui/column {:gap 10}
      [ui/label *s]
      [ui/button {:on-click (fn [_] (swap! *s inc))} "Click me"]
      [ui/button {:on-click (fn [_] (swap! *s inc))} "Click me"]
      [ui/button {:on-click (fn [_] (swap! *s inc))} "Click me"]]]))