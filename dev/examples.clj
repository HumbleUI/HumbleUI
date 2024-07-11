(ns examples
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [examples.7guis-converter]
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
    [examples.link]
    [examples.oklch]
    [examples.padding]
    [examples.paragraph]
    [examples.scroll]
    [examples.settings]
    [examples.size]
    [examples.slider]
    [examples.stack]
    [examples.svg]
    [examples.switch]
    [examples.testbed]
    [examples.text-field]
    [examples.text-field-debug]
    [examples.todomvc]
    [examples.tooltip]
    [examples.treemap]
    [examples.util :as util]
    [examples.wordle]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.window :as window]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.jwm.skija LayerMetalSkija]
    [io.github.humbleui.skija ColorSpace]))

; TODO https://www.egui.rs/

(def examples
  [["Documented"
    [["Align" examples.align/ui]
     ["Label" examples.label/ui]
     ["Padding" examples.padding/ui]
     ["Size" examples.size/ui]]]
   ["Components"
    [["Animation" examples.animation/ui]
     ["Backdrop" examples.backdrop/ui]
     ["Button" examples.button/ui]
     ["Canvas" examples.canvas/ui]
     ["Canvas Shapes" examples.canvas-shapes/ui]
     ["Checkbox" examples.checkbox/ui]
     ["Container" examples.container/ui]
     ["Grid" examples.grid/ui]
     ["Image" examples.image/ui]
     ["Image Snapshot" examples.image-snapshot/ui]
     ["Link" examples.link/ui]
     ["Paragraph" examples.paragraph/ui]
     ["Scroll" examples.scroll/ui]
     ["Slider" examples.slider/ui]
     ["Stack" examples.stack/ui]
     ["SVG" examples.svg/ui]
     ["Text Field" examples.text-field/ui]
     ["Text Field Debug" examples.text-field-debug/ui]
     ["Switch" examples.switch/ui]
     ["Tooltip" examples.tooltip/ui]]]
   ["Demos"
    [["7 GUIs: Converter" examples.7guis-converter/ui]
     ["Blur" examples.blur/ui]
     ["BMI Calculator" examples.bmi-calculator/ui]
     ["Calculator" examples.calculator/ui]
     ["Effects" examples.effects/ui]
     ["Framerate" examples.framerate/ui]
     ["OkLCH" examples.oklch/ui]
     ["Todo MVC" examples.todomvc/ui]
     ["Treemap" examples.treemap/ui]
     ["Wordle" examples.wordle/ui]]]
   ["Other"
    [["Error Handling" examples.errors/ui]
     ["Settings" examples.settings/ui]
     ["Testbed" examples.testbed/ui]]]])

^:clj-reload/keep
(util/def-durable-signal *example
  (-> examples first second first first))

(ui/defcomp example-header [name]
  [ui/padding {:horizontal 20 :vertical 10}
   [ui/label {:font-weight :bold} name]])

(ui/defcomp example-label [name]
  (let [fill-selected (paint/fill 0xFFB2D7FE)
        fill-active   (paint/fill 0xFFA2C7EE)
        fill-hovered  (paint/fill 0xFFE1EFFA)]
    (fn [name]
      [ui/hoverable
       {:on-hover
        (fn [e]
          (when
            (if (= :macos app/platform)
              (:mac-command (:modifiers e))
              (:control (:modifiers e)))
            (reset! *example name)))}
       [ui/clickable
        {:on-click
         (fn [_]
           (reset! *example name))}
        (fn [state]
          (let [label [ui/padding {:horizontal 20 :vertical 10}
                       [ui/label name]]
                selected? (= name @*example)]
            (cond
              selected?        [ui/rect {:paint fill-selected} label]
              (:pressed state) [ui/rect {:paint fill-active} label]
              (:hovered state) [ui/rect {:paint fill-hovered} label]
              :else            label)))]])))

(ui/defcomp app-impl []
  (let [*profiling?  (signal/signal false)
        examples-map (->> examples
                       (mapcat second)
                       (into {}))]
    (fn []
      [ui/row
       [ui/vscrollbar
        [ui/column
         (for [[section examples] examples]
           (list
             [ui/gap {:height 10}]
             [example-header section]
             (for [[name _] (sort-by first examples)]
               [example-label name])))
         [ui/gap {:height 10}]
         [ui/padding {:horizontal 20}
          [ui/button {:on-click (fn [_] (reset! *profiling? true))} "Profile"]]
         [ui/gap {:height 10}]]]
    
       [ui/rect {:paint (paint/fill 0xFFEEEEEE)}
        [ui/gap {:width 1}]]
    
       ^{:stretch 1}
       [ui/clip
        [ui/profile {:value *profiling?}
         [(examples-map @*example)]]]])))

(defn app-wrapper []
  [app-impl])

(defonce *app
  (atom nil))

(reset! *app
  (ui/default-theme {}
    (ui/make [app-wrapper])))

(defn maybe-save-window-rect [window event]
  (when (#{:window-move :window-resize} (:event event))
    (let [rect (window/window-rect window)
          {:keys [id scale work-area]} (window/screen window)
          x (-> rect :x (- (:x work-area)) (/ scale) int)
          y (-> rect :y (- (:y work-area)) (/ scale) int)
          w (-> rect :width (/ scale) int)
          h (-> rect :height (/ scale) int)]
      (util/save-state {:screen-id id, :x x, :y y, :width w, :height h}))))

(defn restore-window-rect []
  (core/when-some+ [{:keys [screen-id x y width height]} (util/load-state)]
    (when-some [screen (core/find-by :id screen-id (app/screens))]
      (let [{:keys [scale work-area]} screen
            right  (-> (:right work-area) (/ scale) int)
            bottom (-> (:bottom work-area) (/ scale) int)
            x      (min (- right 500) x)
            y      (min (- bottom 500) y)
            width  (min (- right x) width)
            height (min (- bottom y) height)]
        {:screen screen-id, :x x, :y y, :width width, :height height}))))

(defn -main [& args]
  ;; setup window
  (ui/start-app!
    (let [opts   (merge
                   {:title    "Humble 🐝 UI"
                    :mac-icon "dev/images/icon.icns"
                    :screen   (:id (first (app/screens)))
                    :width    800
                    :height   800
                    :x        :center
                    :y        :center
                    :on-event #'maybe-save-window-rect}
                   (restore-window-rect))
          window (ui/window opts *app)]
      ;; TODO load real monitor profile
      (when (= :macos app/platform)
        (set! (.-_colorSpace ^LayerMetalSkija (.getLayer window)) (ColorSpace/getDisplayP3)))
      (util/set-floating! window @util/*floating?)
      (deliver util/*window window)))
  @util/*window)
