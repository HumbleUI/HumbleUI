(ns examples.bmi-calculator
  (:require
    [clojure.math :as math]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.ui :as ui]))

(def ^:dynamic *editing*
  false)

(def *height
  (atom {:value 180
         :min   100
         :max   250}))

(def *weight
  (atom {:value 80
         :min   30
         :max   150}))

; 30 / 2.2 / 2.2
; 6..150
; 80 / 1.8 / 1.8

(def *bmi
  (atom {:value 25
         :min   5
         :max   150}))

(add-watch *height ::update
  (fn [_ _ old new]
    (when-not *editing*
      (binding [*editing* true]
        (when (not= (:value old) (:value new))
          (let [height (/ (:value new) 100)
                weight (:value @*weight)]
            (swap! *bmi assoc
              :value (math/round (/ weight (* height height))))))))))

(add-watch *weight ::update
  (fn [_ _ old new]
    (when-not *editing*
      (binding [*editing* true]
        (when (not= (:value old) (:value new))
          (let [height (/ (:value @*height) 100)
                weight (:value new)]
            (swap! *bmi assoc
              :value (math/round (/ weight (* height height))))))))))

(add-watch *bmi ::update
  (fn [_ _ old new]
    (when-not *editing*
      (binding [*editing* true]
        (when (not= (:value old) (:value new))
          (let [height  (/ (:value @*height) 100)
                bmi     (:value new)
                weight  (core/clamp (* bmi height height) 30 150)
                height' (* (math/sqrt (/ weight bmi)) 100)]
            (swap! *weight assoc :value (math/round weight))
            (swap! *height assoc :value (math/round height'))))))))

(defn slider [label *state unit]
  (ui/row
    (ui/valign 0.5
      (ui/width 60
        (ui/label label)))
    [:stretch 1 (ui/slider *state)]
    (ui/valign 0.5
      (ui/width 40
        (ui/halign 1
          (ui/dynamic _ [value (:value @*state)]
            (ui/label value)))))
    (ui/gap 5 0)
    (ui/valign 0.5
      (ui/width 20
        (ui/halign 0
          (ui/label unit))))))

(def ui
  (ui/padding 20 20
    (ui/valign 0.5
      (ui/column
        (slider "Height" *height "cm")
        (ui/gap 0 10)
        (slider "Weight" *weight "kg")
        (ui/gap 0 10)
        (slider "BMI" *bmi "")))))
