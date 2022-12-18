(ns examples.7guis-converter
  (:require
    [clojure.string :as str]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.ui :as ui]))

(def *celsius
  (atom {:text "5"
         :from 1
         :to   1}))

(def *fahrenheit
  (atom {:text "41"}))

(def ^:dynamic *editing* false)

(add-watch *celsius ::update
  (fn [_ _ old new]
    (when-not *editing*
      (when (not= (:text old) (:text new))
        (binding [*editing* true]
          (if-some [c (parse-long (str/trim (:text new)))]
            (let [f (-> c (* 9) (quot 5) (+ 32) str)]
              (swap! *fahrenheit assoc
                :text f
                :from (count f)
                :to   (count f)))
            (swap! *fahrenheit assoc
              :text ""
              :from 0
              :to   0)))))))

(add-watch *fahrenheit ::update
  (fn [_ _ old new]
    (when-not *editing*
      (when (not= (:text old) (:text new))
        (binding [*editing* true]
          (if-some [f (parse-long (str/trim (:text new)))]
            (let [c (-> f (- 32) (* 5) (quot 9) str)]
              (swap! *celsius assoc
                :text c
                :from (count c)
                :to   (count c)))
            (swap! *celsius assoc
              :text ""
              :from 0
              :to   0)))))))

; C = (F - 32) * (5/9)
; F = C * (9/5) + 32

(def ui
  (ui/focus-controller
    (ui/center
      (ui/with-context
        {:hui.text-field/padding-top    10
         :hui.text-field/padding-bottom 10
         :hui.text-field/padding-left   5
         :hui.text-field/padding-right  5}
        (ui/row
          (ui/width 50
            (ui/text-field {:focused (core/now)} *celsius))
          (ui/gap 5 0)
          (ui/valign 0.5
            (ui/label "Celsius = "))
          (ui/width 50
            (ui/text-field *fahrenheit))  
          (ui/gap 5 0)
          (ui/valign 0.5
            (ui/label "Fahrenheit")))))))
