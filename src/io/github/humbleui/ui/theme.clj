(ns io.github.humbleui.ui.theme
  (:require
    [clojure.java.io :as io]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.typeface :as typeface]
    [io.github.humbleui.ui.dynamic :as dynamic]
    [io.github.humbleui.ui.with-context :as with-context])
  (:import
    [io.github.humbleui.skija Data Font Typeface]))

(defn default-theme
  ([comp] (default-theme {} comp))
  ([opts comp]
   (let [face-ui (or (:face-ui opts)
                   (typeface/make-from-resource "io/github/humbleui/fonts/Inter-Regular.ttf"))]
     ; face-italic  (typeface/make-from-resource "io/github/humbleui/fonts/Inter-Italic.ttf")
     ; face-bold    (typeface/make-from-resource "io/github/humbleui/fonts/Inter-Bold.ttf")
     (dynamic/dynamic ctx [scale (:scale ctx)]
       (let [font-ui    (if-some [size (:font-size opts)]
                          (font/make-with-size face-ui (* scale size))
                          (font/make-with-cap-height face-ui (* scale (or (:cap-height opts) 9))))
             cap-height (:cap-height (font/metrics font-ui))
             font-size  (font/size font-ui)
             leading    (or (:leading opts) (-> cap-height Math/round (/ scale) float))
             fill-text  (or (:fill-text opts) (paint/fill 0xFF000000))]
         (with-context/with-context
           (merge
             {:face-ui        face-ui
              :font-ui        font-ui
              :leading        leading
              :fill-text      fill-text
              :hui.text-field/font                  font-ui
              :hui.text-field/cursor-blink-interval 500
              :hui.text-field/fill-text             fill-text
              :hui.text-field/fill-cursor           fill-text
              :hui.text-field/fill-selection        (paint/fill 0xFFB1D7FF)
              :hui.text-field/cursor-width          (float 1)
              :hui.text-field/padding-top           (-> cap-height (/ 3) Math/round (/ scale) float)
              :hui.text-field/padding-bottom        (-> cap-height (/ 3) Math/round (/ scale) float)
              :hui.text-field/padding-left          (float 0)
              :hui.text-field/padding-right         (float 0)}
             opts)
           comp))))))

; (require 'user :reload)