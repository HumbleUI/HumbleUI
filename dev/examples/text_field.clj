(ns examples.text-field
  (:require
    [clojure.string :as str]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(defonce *text (atom {:text "Change me" :from 9 :to 9}))

(def ui
  (ui/valign 0.5
    (ui/halign 0.5
      (ui/width #(/ (:width %) 2)
        (ui/fill (paint/fill 0xFFFFFFFF)
          (ui/padding 10 10
            (ui/text-field *text)))))))

(reset! user/*example "text-field")