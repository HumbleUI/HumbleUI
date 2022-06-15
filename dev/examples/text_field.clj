(ns examples.text-field
  (:require
    [clojure.string :as str]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(defonce *text1
  (atom
    {:text "Change me ([{word1} word2] wo-rd3)  , word4 🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️ 🚵🏻‍♀️ more more more"
     :from 9
     :to 9}))

(defonce *text2
  (atom
    {:text "0123456890 AaBbCcDdEe FfGgHhIiJj KkLlMmNnOo PpQqRrSsTt UuVvWwXxYyZz"
     :from 0
     :to 0}))

(defonce *text3
  (atom
    {:text "Hello Humble UI!"
     :from 0
     :to 0}))

(def ui
  (ui/valign 0.5
    (ui/halign 0.5
      (ui/column
        (ui/halign 0.5
          (ui/width #(- (:width %) 100)
            (ui/fill (paint/fill 0xFFFFFFFF)
              (ui/padding 0 10
                (ui/text-field *text1)))))
        (ui/gap 0 10)
        (ui/halign 0.5
          (ui/width 200
            (ui/fill (paint/fill 0xFFFFFFFF)
              (ui/padding 0 10
                (ui/text-field *text2 {:padding-h 5})))))
        (ui/gap 0 10)
        (ui/halign 0.5
          (ui/fill (paint/fill 0xFFFFFFFF)
            (ui/padding 0 10
              (ui/text-field *text3 {:padding-h 5}))))))))

(reset! user/*example "text-field")