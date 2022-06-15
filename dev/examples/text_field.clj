(ns examples.text-field
  (:require
    [clojure.string :as str]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(def ui
  (ui/valign 0.5
    (ui/halign 0.5
      (ui/column
        (ui/width 300
          (ui/fill (paint/fill 0xFFFFFFFF)
            (ui/padding 0 10
              (ui/text-field
                (atom
                  {:text "Change me ([{word1} word2] wo-rd3)  , word4 🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️ 🚵🏻‍♀️ more more more"
                   :from 9
                   :to 9})))))
        (ui/gap 0 10)
        (ui/width 300
          (ui/fill (paint/fill 0xFFFFFFFF)
            (ui/padding 0 10
              (ui/text-field
                (atom
                  {:text "0123456890 AaBbCcDdEe FfGgHhIiJj KkLlMmNnOo PpQqRrSsTt UuVvWwXxYyZz"
                   :from 0
                   :to 0})
                {:padding-h 5}))))
        (ui/gap 0 10)
        (ui/width 300
          (ui/halign 0.5
            (ui/fill (paint/fill 0xFFFFFFFF)
              (ui/padding 0 10
                (ui/text-field
                  (atom
                    {:text "Content width"
                     :from 13
                     :to 13})
                  {:padding-h 5})))))
        (ui/gap 0 10)
        (ui/width 300
          (ui/fill (paint/fill 0xFFFFFFFF)
            (ui/halign 0.5
              (ui/padding 0 10
                (ui/text-field
                  (atom
                    {:text "Align center"
                     :from 12
                     :to 12})
                  {:padding-h 5})))))
        (ui/gap 0 10)
        (ui/width 300
          (ui/fill (paint/fill 0xFFFFFFFF)
            (ui/halign 1
              (ui/padding 0 10
                (ui/text-field
                  (atom
                    {:text "Align right"
                     :from 11
                     :to 11})
                  {:padding-h 5})))))))))

(reset! user/*example "text-field")