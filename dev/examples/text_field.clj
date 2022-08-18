(ns examples.text-field
  (:require
    [clojure.string :as str]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(defn text-field [text & {:keys [from to cursor-blink-interval cursor-width padding-h padding-v padding-top padding-bottom]
                          :or {cursor-blink-interval 500, cursor-width 1, padding-h 0, padding-v 3}
                          :as opts}]
  (ui/with-cursor :ibeam
    (ui/fill (paint/fill 0xFFFFFFFF)
      (ui/with-context
        {:hui.text-field/cursor-blink-interval cursor-blink-interval
         :hui.text-field/cursor-width   cursor-width
         :hui.text-field/padding-top    (float (or padding-top padding-v))
         :hui.text-field/padding-bottom (float (or padding-bottom padding-v))
         :hui.text-field/padding-left   (float padding-h)
         :hui.text-field/padding-right  (float padding-h)}
        (ui/text-field
          (atom
            {:text text
             :from from
             :to   to}))))))

(def ui
  (ui/valign 0.5
    (ui/halign 0.5
      (ui/column
        (ui/width 300
          (text-field "Change me ([{word1} word2] wo-rd3)  , word4 🚵🏻‍♀️🚵🏻‍♀️🚵🏻‍♀️ 🚵🏻‍♀️ more more more" :from 13 :to 18))
        (ui/gap 0 10)
        (ui/width 300
          (text-field "0123456890 AaBbCcDdEe FfGgHhIiJj KkLlMmNnOo PpQqRrSsTt UuVvWwXxYyZz" :padding-h 5 :padding-v 10 :cursor-width 2 :cursor-blink-interval 100))
        (ui/gap 0 10)
        (ui/width 300
          (text-field "0123456890 AaBbCcDdEe FfGgHhIiJj KkLlMmNnOo PpQqRrSsTt UuVvWwXxYyZz" :padding-h 5 :padding-top 20 :padding-bottom 5 :cursor-blink-interval 0))
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
            (text-field "Align right" :padding-h 5 :padding-v 10)))))))

(reset! user/*example "text-field")