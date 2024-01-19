(ns examples
  (:require
    [clojure.core.server :as server]
    ; [examples.7guis-converter]
    [examples.align]
    ; [examples.animation]
    ; [examples.backdrop]
    ; [examples.blur]
    ; [examples.bmi-calculator]
    [examples.button]
    ; [examples.calculator]
    ; [examples.canvas]
    ; [examples.canvas-shapes]
    ; [examples.checkbox]
    [examples.container]
    ; [examples.effects]
    ; [examples.errors]
    ; [examples.event-bubbling]
    ; [examples.framerate]
    ; [examples.grid]
    ; [examples.image-snapshot]
    [examples.label]
    ; [examples.oklch]
    ; [examples.paragraph]
    ; [examples.scroll]
    ; [examples.settings]
    ; [examples.slider]
    ; [examples.stack]
    [examples.state :as state]
    [examples.svg]
    ; [examples.text-field]
    ; [examples.text-field-debug]
    ; [examples.todomvc]
    ; [examples.toggle]
    ; [examples.tooltip]
    ; [examples.tree]
    ; [examples.treemap]
    ; [examples.wordle]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.debug :as debug]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.window :as window]
    [io.github.humbleui.ui :as ui]))

(def examples
  (sorted-map
    ; "7 GUIs: Converter" examples.7guis-converter/ui
    "Align" examples.align/ui
    ; "Animation" examples.animation/ui
    ; "Backdrop" examples.backdrop/ui
    ; "Blur" examples.blur/ui
    ; "BMI Calculator" examples.bmi-calculator/ui
    "Button" examples.button/ui
    ; "Calculator" examples.calculator/ui
    ; "Canvas" examples.canvas/ui
    ; "Canvas Shapes" examples.canvas-shapes/ui
    ; "Checkbox" examples.checkbox/ui
    "Container" examples.container/ui
    ; "Effects" examples.effects/ui
    ; "Errors" examples.errors/ui
    ; "Event Bubbling" examples.event-bubbling/ui
    ; "Framerate" examples.framerate/ui
    ; "Grid" examples.grid/ui
    ; "Image Snapshot" examples.image-snapshot/ui
    "Label" examples.label/ui
    ; "OkLCH" examples.oklch/ui
    ; "Paragraph" examples.paragraph/ui
    ; "Scroll" examples.scroll/ui
    ; "Settings" examples.settings/ui
    ; "Slider" examples.slider/ui
    ; "Stack" examples.stack/ui
    "SVG" examples.svg/ui
    ; "Text Field" examples.text-field/ui
    ; "Text Field Debug" examples.text-field-debug/ui
    ; "Todo MVC" examples.todomvc/ui
    ; "Toggle" examples.toggle/ui
    ; "Tooltip" examples.tooltip/ui
    ; "Tree" examples.tree/ui
    ; "Treemap" examples.treemap/ui
    ; "Wordle" examples.wordle/ui
    ))

(let [fill-selected (paint/fill 0xFFB2D7FE)
      fill-active   (paint/fill 0xFFA2C7EE)
      fill-hovered  (paint/fill 0xFFE1EFFA)]
  (ui/defcomp example-label [name]
    (let [*hovered? (signal/signal false)
          *active?  (signal/signal false)]
      (fn [name]
        [ui/clickable {:on-click  (fn [_]
                                    (signal/reset! examples.state/*example name))
                       :*hovered? *hovered?
                       :*active?  *active?}
         (let [label     [ui/padding {:horizontal 20 :vertical 10}
                          [ui/label name]]
               selected? (= name @examples.state/*example)]
           (cond
             selected?  [ui/rect {:paint fill-selected} label]
             @*active?  [ui/rect {:paint fill-active} label]
             @*hovered? [ui/rect {:paint fill-hovered} label]
             :else      label))]))))

(ui/defcomp app-impl []
  [ui/row {:gap 10}
   [ui/column
    (for [[name _] (sort-by first examples)]
      [example-label name])]
    
   [ui/rect {:paint (paint/fill 0xFFEEEEEE)}
    [ui/gap {:width 1}]]
    
   ^{:stretch 1}
   [(examples @examples.state/*example)]])

(def app
  (ui/default-theme {}
    (ui/make [app-impl])))

(alter-meta! *ns* assoc :clojure.tools.namespace.repl/before-unload
  #(alter-var-root #'app (constantly nil)))
