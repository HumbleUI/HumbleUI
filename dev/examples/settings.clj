(ns examples.settings
  (:require
    [io.github.humbleui.debug :as debug]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]))

(set! *warn-on-reflection* true)

(defonce *floating (atom true))

(def ui
  (ui/valign 0.5
    (ui/halign 0.5
      (ui/column
        (ui/checkbox *floating (ui/label "On top"))
        (ui/gap 0 10)
        (ui/checkbox debug/*enabled? (ui/label "Debug"))))))

; (reset! user/*example "settings")