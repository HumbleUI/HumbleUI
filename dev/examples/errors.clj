(ns examples.errors
  (:require
    [io.github.humbleui.ui :as ui]))

(defn ui []
  [ui/center
   [ui/label (/ 1 0)]])
