(ns examples.errors
  (:require
    [io.github.humbleui.ui :as ui]))

(def ui
  (ui/center
    (ui/column
      (ui/dynamic _ []
        (ui/label (/ 1 0))))))