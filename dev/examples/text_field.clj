(ns examples.text-field
  (:require
    [clojure.string :as str]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(defonce *clicks (atom 0))

(def ui
  (ui/valign 0.5
    (ui/halign 0.5
      (ui/label "Text field !!!"))))

; (reset! user/*example "text-field")