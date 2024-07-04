(ns examples.bmi-calculator
  (:require
    [clojure.math :as math]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(def ^:dynamic *editing*
  false)

(def *height
  (signal/signal 180))

(def *weight
  (signal/signal 80))

; 30 / 2.2 / 2.2
; 6..150
; 80 / 1.8 / 1.8

(def *bmi
  (signal/signal 25))

(add-watch *height ::update
  (fn [_ _ old new]
    (when (not= old new)
      (when-not *editing*
      
        (binding [*editing* true]
        
          (let [height (/ new 100)
                weight @*weight]
            (reset! *bmi (math/round (/ weight (* height height))))))))))

(add-watch *weight ::update
  (fn [_ _ old new]
    (when (not= old new)
      (when-not *editing*
        (binding [*editing* true]
          (let [height (/ @*height 100)
                weight new]
            (reset! *bmi (math/round (/ weight (* height height))))))))))

(add-watch *bmi ::update
  (fn [_ _ old new]
    (when (not= old new)
      (when-not *editing*
        (binding [*editing* true]
          (let [height  (/ @*height 100)
                bmi     new
                weight  (core/clamp (* bmi height height) 30 150)
                height' (* (math/sqrt (/ weight bmi)) 100)]
            (reset! *weight (math/round weight))
            (reset! *height (math/round height'))))))))

(defn slider [label *state min max unit]
  [ui/row
   [ui/align {:y :center}
    [ui/size {:width 60}
     [ui/label label]]]
   ^{:stretch 1} [ui/slider {:*value *state :min min :max max}]
   [ui/align {:y :center}
    [ui/size {:width 40}
     [ui/align {:x :right}
      [ui/label *state]]]]
   [ui/gap {:width 5}]
   [ui/align {:y :center}
    [ui/size {:width 20}
     [ui/align {:x :left}
      [ui/label unit]]]]])

(defn ui []
  [ui/align {:y :center}
   [ui/vscrollbar
    [ui/padding {:padding 20}
     [ui/column {:gap 10}
      [slider "Height" *height 100 250 "cm"]
      [slider "Weight" *weight  30 150 "kg"]
      [slider "BMI"    *bmi      5 150 ""]]]]])
