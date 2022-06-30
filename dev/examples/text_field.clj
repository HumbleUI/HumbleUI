(ns examples.text-field
  (:require
    [clojure.string :as str]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(defn text-field [text & {:keys [from to] :or {from 0 to 0} :as opts}]
  (ui/with-cursor :ibeam
    (ui/fill (paint/fill 0xFFFFFFFF)
      (ui/text-field
        (atom
          {:text text
           :from from
           :to   to})
        (dissoc opts :from :to)))))

(def ui
  (ui/valign 0.5
    (ui/halign 0.5
      (ui/column
        (ui/width 300
          (text-field "Change me ([{word1} word2] wo-rd3)  , word4 🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️ 🚵🏻‍♀️ more more more" :from 9 :to 9))
        (ui/gap 0 10)
        (ui/width 300
          (text-field "0123456890 AaBbCcDdEe FfGgHhIiJj KkLlMmNnOo PpQqRrSsTt UuVvWwXxYyZz" :padding-h 5 :padding-v 10))
        (ui/gap 0 10)
        (ui/width 300
          (text-field "0123456890 AaBbCcDdEe FfGgHhIiJj KkLlMmNnOo PpQqRrSsTt UuVvWwXxYyZz" :padding-h 5 :padding-top 20 :padding-bottom 5))
        (ui/gap 0 10)
        (ui/width 300
          (ui/halign 0
            (text-field "Content width" :from 13 :to 13 :padding-h 5 :padding-v 10)))
        (ui/gap 0 10)
        (ui/width 300
          (ui/halign 0.5
            (text-field "Align center" :padding-h 5 :padding-v 10)))
        (ui/gap 0 10)
        (ui/width 300
          (ui/halign 1
            (text-field "Align right" :padding-h 5 :padding-v 10)))
        ))))

(reset! user/*example "text-field")