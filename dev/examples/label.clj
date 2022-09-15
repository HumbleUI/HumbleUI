(ns examples.label
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]))

(set! *warn-on-reflection* true)

(def ui
  (ui/center
    (ui/label "Hello from Humble UI! 👋")))

; (reset! user/*example "label")