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
                          (font/make-with-cap-height face-ui (* scale (or (:cap-height opts) 10))))
             cap-height (:cap-height (font/metrics font-ui))
             font-size  (font/size font-ui)
             leading    (or (:leading opts) (-> cap-height Math/round (/ scale) float))
             fill-text  (or (:fill-text opts) (paint/fill 0xFF000000))
             fill-gray  (or (:fill-gray opts) (paint/fill 0xFF808080))
             theme      {:face-ui        face-ui
                         :font-ui        font-ui
                         :leading        leading
                         :fill-text      fill-text
                         :fill-gray      fill-gray
                         
                         :hui.button/bg-active      (paint/fill 0xFFA2C7EE)
                         :hui.button/bg-hovered     (paint/fill 0xFFCFE8FC)
                         :hui.button/bg             (paint/fill 0xFFB2D7FE)
                         :hui.button/border-radius  4
                         :hui.button/padding-left   (* 2 leading)
                         :hui.button/padding-top    leading
                         :hui.button/padding-right  (* 2 leading)
                         :hui.button/padding-bottom leading
                         
                         :hui.toggle/fill-enabled         (paint/fill 0xFF0080FF)
                         :hui.toggle/fill-disabled        (paint/fill 0xFFD9D9D9)
                         :hui.toggle/fill-handle          (paint/fill 0xFFFFFFFF)
                         :hui.toggle/fill-enabled-active  (paint/fill 0xFF0060E0)
                         :hui.toggle/fill-disabled-active (paint/fill 0xFFBBBBBB)
                         :hui.toggle/fill-handle-active   (paint/fill 0xFFE0E0E0)
                         
                         :hui.slider/handle-size          (* 16 scale)
                         :hui.slider/shaft-height         (* 2 scale)
                         :hui.slider/fill-in              (paint/fill 0xFF0080FF)
                         :hui.slider/fill-out             (paint/fill 0xFFD9D9D9)
                         :hui.slider/fill-handle          (paint/fill 0xFFFFFFFF)
                         :hui.slider/stroke-handle        (paint/stroke 0xFF0080FF (* 2 scale))
                         :hui.slider/fill-handle-active   (paint/fill 0xFFE0E0E0)
                         :hui.slider/stroke-handle-active (paint/stroke 0xFF0060E0 (* 2 scale))
                         
                         :hui.text-field/font                    font-ui
                         :hui.text-field/font-features           []
                         :hui.text-field/cursor-blink-interval   500
                         :hui.text-field/fill-text               fill-text
                         :hui.text-field/fill-placeholder        fill-gray
                         :hui.text-field/fill-cursor             fill-text
                         :hui.text-field/fill-selection-active   (paint/fill 0xFFB1D7FF)
                         :hui.text-field/fill-selection-inactive (paint/fill 0xFFDDDDDD)
                         :hui.text-field/fill-bg-active          (paint/fill 0xFFFFFFFF)
                         :hui.text-field/fill-bg-inactive        (paint/fill 0xFFF8F8F8)
                         :hui.text-field/border-radius           4
                         :hui.text-field/border-active           (paint/stroke 0xFF749EE4 (* 1 scale))
                         :hui.text-field/border-inactive         (paint/stroke 0xFFCCCCCC (* 1 scale))
                         :hui.text-field/cursor-width            (float 1)
                         :hui.text-field/padding-top             (-> cap-height (/ 3) Math/round (/ scale) float)
                         :hui.text-field/padding-bottom          (-> cap-height (/ 3) Math/round (/ scale) float)
                         :hui.text-field/padding-left            (float 0)
                         :hui.text-field/padding-right           (float 0)}]
         (with-context/with-context (core/merge-some theme opts) comp))))))

; (require 'user :reload)