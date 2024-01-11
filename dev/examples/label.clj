(ns examples.label
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  [ui/center
    [ui/label "Hello from Humble UI! 👋"]])
