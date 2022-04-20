(ns examples.label
  (:require
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(def ui
  (ui/valign 0.5
    (ui/halign 0.5
      (ui/label "Hello from Humble UI! 👋"))))