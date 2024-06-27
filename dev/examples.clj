(ns examples
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    ; [examples.7guis-converter]
    [examples.align]
    [examples.animation]
    [examples.backdrop]
    [examples.blur]
    [examples.bmi-calculator]
    [examples.button]
    [examples.calculator]
    [examples.canvas]
    [examples.canvas-shapes]
    [examples.checkbox]
    [examples.container]
    [examples.effects]
    [examples.errors]
    [examples.framerate]
    [examples.grid]
    [examples.image]
    [examples.image-snapshot]
    [examples.label]
    [examples.oklch]
    [examples.paragraph]
    [examples.scroll]
    [examples.settings]
    [examples.slider]
    [examples.stack]
    [examples.svg]
    [examples.switch]
    ; [examples.text-field]
    ; [examples.text-field-debug]
    ; [examples.todomvc]
    [examples.tooltip]
    ; [examples.treemap]
    [examples.util :as util]
    [examples.wordle]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.debug :as debug]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.window :as window]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.jwm.skija LayerMetalSkija]
    [io.github.humbleui.skija ColorSpace]))

; TODO https://www.egui.rs/

(def examples
  (sorted-map
    ; "7 GUIs: Converter" examples.7guis-converter/ui
    "Align" examples.align/ui
    "Animation" examples.animation/ui
    "Backdrop" examples.backdrop/ui
    "Blur" examples.blur/ui
    "BMI Calculator" examples.bmi-calculator/ui
    "Button" examples.button/ui
    "Calculator" examples.calculator/ui
    "Canvas" examples.canvas/ui
    "Canvas Shapes" examples.canvas-shapes/ui
    "Checkbox" examples.checkbox/ui
    "Container" examples.container/ui
    "Effects" examples.effects/ui
    "Errors" examples.errors/ui
    "Framerate" examples.framerate/ui
    "Grid" examples.grid/ui
    "Image" examples.image/ui
    "Image Snapshot" examples.image-snapshot/ui
    "Label" examples.label/ui
    "OkLCH" examples.oklch/ui
    "Paragraph" examples.paragraph/ui
    "Scroll" examples.scroll/ui
    "Settings" examples.settings/ui
    "Slider" examples.slider/ui
    "Stack" examples.stack/ui
    "SVG" examples.svg/ui
    ; "Text Field" examples.text-field/ui
    ; "Text Field Debug" examples.text-field-debug/ui
    ; "Todo MVC" examples.todomvc/ui
    "Switch" examples.switch/ui
    "Tooltip" examples.tooltip/ui
    ; "Treemap" examples.treemap/ui
    "Wordle" examples.wordle/ui
    ))

^:clj-reload/keep
(util/def-durable-signal *example
  (first (keys examples)))

(ui/defcomp example-label [name]
  (let [fill-selected (paint/fill 0xFFB2D7FE)
        fill-active   (paint/fill 0xFFA2C7EE)
        fill-hovered  (paint/fill 0xFFE1EFFA)]
    (fn [name]
      [ui/clickable
       {:on-click (fn [_]
                    (signal/reset! *example name))}
       (fn [state]
         (let [label [ui/padding {:horizontal 20 :vertical 10}
                      [ui/label name]]
               selected? (= name @*example)]
           (cond
             selected?        [ui/rect {:paint fill-selected} label]
             (:pressed state) [ui/rect {:paint fill-active} label]
             (:hovered state) [ui/rect {:paint fill-hovered} label]
             :else            label)))])))

(ui/defcomp app-impl []
  [ui/row
   [ui/vscrollbar
    [ui/column
     (for [[name _] (sort-by first examples)]
       [example-label name])]]
    
   [ui/rect {:paint (paint/fill 0xFFEEEEEE)}
    [ui/gap {:width 1}]]
    
   ^{:stretch 1}
   [ui/clip
    [(examples @*example)]]])

(defonce *app
  (atom nil))

(reset! *app
  (ui/default-theme {}
    (ui/make [app-impl])))

; (defn before-ns-unload []
;   (reset! *app nil))

(defn maybe-save-window-rect [window event]
  (when (#{:window-move :window-resize} (:event event))
    (let [rect (window/window-rect window)
          {:keys [scale work-area]} (window/screen window)
          x (-> rect :x (- (:x work-area)) (/ scale) int)
          y (-> rect :y (- (:y work-area)) (/ scale) int)
          w (-> rect :width (/ scale) int)
          h (-> rect :height (/ scale) int)]
      (util/save-state {:x x, :y y, :width w, :height h}))))

(defn restore-window-rect [screen]
  (let [{:keys [work-area]} screen]
    (core/when-some+ [{:keys [x y width height]} (util/load-state)]
      (let [x      (min (- (:right work-area) 500) x)
            y      (min (- (:bottom work-area) 500) y)
            width  (min (- (:right work-area) x) width)
            height (min (- (:bottom work-area) y) height)]
        {:x x, :y y, :width width, :height height}))))

(defn -main [& args]
  ;; setup window
  (ui/start-app!
    (let [screen (first (app/screens))
          rect   (restore-window-rect screen)
          opts   {:title    "Humble 🐝 UI"
                  :mac-icon "dev/images/icon.icns"
                  :screen   (:id screen)
                  :width    800
                  :height   800
                  :x        :center
                  :y        :center
                  :on-event #'maybe-save-window-rect}
          window (ui/window (merge opts rect) *app)]
      ;; TODO load real monitor profile
      (when (= :macos app/platform)
        (set! (.-_colorSpace ^LayerMetalSkija (.getLayer window)) (ColorSpace/getDisplayP3)))
      (util/set-floating! window @util/*floating?)
      (deliver util/*window window)))
  @util/*window)
