(ns examples
  (:require
    [clj-reload.core :as reload]
    [clojure.edn :as edn]
    [clojure.java.io :as io]    
    [examples.7guis-converter]
    [examples.animation]
    [examples.backdrop]
    [examples.blur]
    [examples.bmi-calculator]
    [examples.calculator]
    [examples.canvas]
    [examples.canvas-shapes]
    [examples.checkbox]
    [examples.effects]
    [examples.errors]
    [examples.file-picker]
    [examples.framerate]
    [examples.image-snapshot]
    [examples.link]
    [examples.oklch]
    [examples.paragraph]
    [examples.slider]
    [examples.stack]
    [examples.switch]
    [examples.table]
    [examples.testbed]
    [examples.text-field]
    [examples.text-field-debug]
    [examples.todomvc]
    [examples.tooltip]
    [examples.treemap]
    [examples.viewport]
    [examples.whiteboard]
    [examples.wordle]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.debug :as debug]
    [io.github.humbleui.docs :as docs]
    [io.github.humbleui.docs.devtools :as devtools]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.window :as window]))

(defn load-state []
  (let [file (io/file ".state")]
    (when (.exists file)
      (edn/read-string (slurp file)))))

(defn save-state [m]
  (let [file   (io/file ".state")
        state  (or (load-state) {})
        state' (merge state m)]
    (spit file (pr-str state'))))

(defmacro restore-durable-signal [name]
  (let [key (keyword (clojure.core/name name))]
    `(do
       (when-some [val# (~key (load-state))]
         (reset! ~name val#))

       (add-watch ~name ::save-state
         (fn [_# _# _# new#]
           (save-state {~key new#}))))))

(restore-durable-signal debug/*paint?)

(restore-durable-signal debug/*pacing?)

(restore-durable-signal debug/*events?)

(restore-durable-signal debug/*outlines?)

(restore-durable-signal debug/*continuous-render?)

(defonce *example
  (ui/signal "DevTools"))

(restore-durable-signal *example)

(defonce *window
  (promise))

(def examples
  [["Documented"
    docs/examples]
   ["Components"
    [["Animation" examples.animation/ui]
     ["Backdrop" examples.backdrop/ui]
     ["Canvas" examples.canvas/ui]
     ["Canvas Shapes" examples.canvas-shapes/ui]
     ["Checkbox" examples.checkbox/ui]
     ["Image Snapshot" examples.image-snapshot/ui]
     ["Link" examples.link/ui]
     ["Paragraph" examples.paragraph/ui]
     ["Slider" examples.slider/ui]
     ["Stack" examples.stack/ui]
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
     ["DevTools" devtools/ui]
     ["Testbed" examples.testbed/ui]]]])

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
      [ui/rect {:paint {:fill 
                        (cond
                          (= name @*example) "B2D7FE"
                          (:pressed state)   "A2C7EE"
                          (:hovered state)   "E1EFFA")}}
       [ui/padding {:horizontal 20 :vertical 10}
        [ui/label name]]])]])

(ui/defcomp app-impl []
  (let [*profiling?  (ui/signal false)
        examples-map (->> examples
                       (mapcat second)
                       (into {}))]
    (fn []
      #_[(examples-map @*example)]
      [ui/hsplit {:width 150
                  :gap [ui/rect {:paint {:fill "DDD"}}
                        [ui/gap {:width 1}]]}
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
      (save-state {:screen-id id, :x x, :y y, :width w, :height h}))))

(defn restore-window-rect []
  (util/when-some+ [{:keys [screen-id x y width height]} (load-state)]
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
  (ui/start-app!
    (deliver *window
      (ui/window
        (merge
          {:title    "Humble 🐝 UI"
           :mac-icon "dev/images/icon.icns"
           :screen   (:id (first (app/screens)))
           :width    800
           :height   800
           :x        :center
           :y        :center
           :on-event #'maybe-save-window-rect}
          (restore-window-rect))
        *app)))
  @*window)
