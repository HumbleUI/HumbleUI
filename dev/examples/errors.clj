(ns examples.errors
  (:require
    [io.github.humbleui.ui :as ui]))

(defn ui []
  [ui/center
   [ui/column
    (str (/ 1 0))]])
