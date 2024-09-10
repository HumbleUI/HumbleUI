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
    [examples.clip]
    [examples.column]
    [examples.color]
    [examples.effects]
    [examples.errors]
    [examples.file-picker]
    [examples.framerate]
    [examples.grid]
    [examples.image]
    [examples.image-snapshot]
    [examples.label]
    [examples.link]
    [examples.oklch]
    [examples.overlay]
    [examples.padding]
    [examples.paint]
    [examples.paragraph]
    [examples.rect]    
    [examples.row]    
    [examples.settings]
    [examples.size]
    [examples.slider]
    [examples.split]
    [examples.stack]
    [examples.svg]
    [examples.switch]
    [examples.table]
    [examples.testbed]
    [examples.text-field]
    [examples.text-field-debug]
    [examples.todomvc]
    [examples.tooltip]
    [examples.treemap]
    [examples.shared :as shared]
    [examples.viewport]
    [examples.vscroll]
    [examples.whiteboard]
    [examples.wordle]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.font :as font]
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
     ["Clip" examples.clip/ui]
     ["Color" examples.color/ui]
     ["Column" examples.column/ui]
     ["Grid" examples.grid/ui]
     ["Label" examples.label/ui]
     ["Overlay" examples.overlay/ui]
     ["Padding" examples.padding/ui]
     ["Paint" examples.paint/ui]
     ["Rect" examples.rect/ui]
     ["Row" examples.row/ui]
     ["Size" examples.size/ui]
     ["Split" examples.split/ui]
     ["VScroll" examples.vscroll/ui]]]
   ["Components"
    [["Animation" examples.animation/ui]
     ["Backdrop" examples.backdrop/ui]
     ["Button" examples.button/ui]
     ["Canvas" examples.canvas/ui]
     ["Canvas Shapes" examples.canvas-shapes/ui]
     ["Checkbox" examples.checkbox/ui]
     ["Image" examples.image/ui]
     ["Image Snapshot" examples.image-snapshot/ui]
     ["Link" examples.link/ui]
     ["Paragraph" examples.paragraph/ui]
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
     ["File Picker" examples.file-picker/ui]
     ["Framerate" examples.framerate/ui]
     ["OkLCH" examples.oklch/ui]
     ["Table" examples.table/ui]
     ["Todo MVC" examples.todomvc/ui]
     ["Treemap" examples.treemap/ui]
     ["Viewport" examples.viewport/ui]
     ["Whiteboard" examples.whiteboard/ui]
     ["Wordle" examples.wordle/ui]]]
   ["Other"
    [["Error Handling" examples.errors/ui]
     ["Settings" examples.settings/ui]
     ["Testbed" examples.testbed/ui]]]])

^:clj-reload/keep
(shared/def-durable-signal *example
  (-> examples first second first first))

(ui/defcomp example-header [name]
  [ui/padding {:horizontal 20 :vertical 10}
   [ui/label {:font-weight :bold} name]])

(ui/defcomp example-label [name]
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
          selected?        [ui/rect {:paint {:fill "B2D7FE"}} label]
          (:pressed state) [ui/rect {:paint {:fill "A2C7EE"}} label]
          (:hovered state) [ui/rect {:paint {:fill "E1EFFA"}} label]
          :else            label)))]])

(ui/defcomp app-impl []
  (let [*profiling?  (ui/signal false)
        examples-map (->> examples
                       (mapcat second)
                       (into {}))]
    (fn []
      #_[(examples-map @*example)]
      [ui/row
       [ui/align {:y :top}
        [ui/vscroll
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
          [ui/gap {:height 10}]]]]
    
       [ui/rect {:paint {:fill "EEE"}}
        [ui/gap {:width 1}]]
    
       ^{:stretch 1}
       [ui/clip
        [ui/profile {:value *profiling?}
         [(examples-map @*example)]]]])))

(defonce *app
  (atom nil))

(reset! *app
  app-impl)

(defn maybe-save-window-rect [window event]
  (when (#{:window-move :window-resize} (:event event))
    (let [rect (window/window-rect window)
          {:keys [id scale work-area]} (window/screen window)
          x (-> rect :x (- (:x work-area)) (/ scale) int)
          y (-> rect :y (- (:y work-area)) (/ scale) int)
          w (-> rect :width (/ scale) int)
          h (-> rect :height (/ scale) int)]
      (shared/save-state {:screen-id id, :x x, :y y, :width w, :height h}))))

(defn restore-window-rect []
  (util/when-some+ [{:keys [screen-id x y width height]} (shared/load-state)]
    (when-some [screen (util/find-by :id screen-id (app/screens))]
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
      (shared/set-floating! window @shared/*floating?)
      (deliver shared/*window window)))
  @shared/*window)
