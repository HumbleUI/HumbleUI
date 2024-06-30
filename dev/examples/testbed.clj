(ns examples.testbed
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  [ui/center
   [ui/column {:gap 10}
    [ui/hoverable
     [ui/label "With label"]]
    [ui/hoverable
     "Without label"]]])