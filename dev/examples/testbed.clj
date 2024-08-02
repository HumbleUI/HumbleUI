(ns examples.testbed
  (:require
    [io.github.humbleui.util :as util]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  (let [*state (signal/signal #{})]
    (fn []
      [ui/center
       [ui/hoverable
        (fn [state]
          [ui/label state])]])))
   