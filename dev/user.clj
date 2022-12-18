(ns user
  (:require
    [examples.7guis-converter]
    [examples.align]
    [examples.animation]
    [examples.backdrop]
    [examples.bmi-calculator]
    [examples.button]
    [examples.calculator]
    [examples.canvas]
    [examples.checkbox]
    [examples.container]
    [examples.errors]
    [examples.event-bubbling]
    [examples.grid]
    [examples.image-snapshot]
    [examples.label]
    [examples.scroll]
    [examples.settings]
    [examples.state :as state]
    [examples.slider]
    [examples.stack]
    [examples.svg]
    [examples.text-field]
    [examples.text-field-debug]
    [examples.todomvc]
    [examples.toggle]
    [examples.tooltip]
    [examples.tree]
    [examples.wordle]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.debug :as debug]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.window :as window]
    [io.github.humbleui.ui :as ui]
    [nrepl.cmdline :as nrepl]))

(defn set-floating! [window floating]
  (when window
    (if floating
      (window/set-z-order window :floating)
      (window/set-z-order window :normal))))

(add-watch state/*floating ::window
  (fn [_ _ _ floating]
    (set-floating! @state/*window floating)))

(def examples
  (sorted-map
    "7 GUIs: Converter" examples.7guis-converter/ui
    "Align" examples.align/ui
    "Animation" examples.animation/ui
    "Backdrop" examples.backdrop/ui
    "BMI Calculator" examples.bmi-calculator/ui
    "Button" examples.button/ui
    "Calculator" examples.calculator/ui
    "Canvas" examples.canvas/ui
    "Checkbox" examples.checkbox/ui
    "Container" examples.container/ui
    "Errors" examples.errors/ui
    "Event Bubbling" examples.event-bubbling/ui
    "Grid" examples.grid/ui
    "Image Snapshot" examples.image-snapshot/ui
    "Label" examples.label/ui
    "Scroll" examples.scroll/ui
    "Settings" examples.settings/ui
    "Slider" examples.slider/ui
    "Stack" examples.stack/ui
    "SVG" examples.svg/ui
    "Text Field" examples.text-field/ui
    "Text Field Debug" examples.text-field-debug/ui
    "Todo MVC" examples.todomvc/ui
    "Toggle" examples.toggle/ui
    "Tooltip" examples.tooltip/ui
    "Tree" examples.tree/ui
    "Wordle" examples.wordle/ui))

(def light-grey
  0xffeeeeee)   

(def border-line
  (ui/rect (paint/fill light-grey)
    (ui/gap 1 0)))
      
(def app
  (ui/default-theme {}; :font-size 13
    ; :cap-height 10
    ; :leading 100
    ; :fill-text (paint/fill 0xFFCC3333)
    ; :hui.text-field/fill-text (paint/fill 0xFFCC3333)
    (ui/row
      (ui/vscrollbar
        (ui/column
          (for [[name _] (sort-by first examples)]
            (ui/clickable
              {:on-click (fn [_] (reset! state/*example name))}
              (ui/dynamic ctx [selected? (= name @state/*example)
                               hovered?  (:hui/hovered? ctx)]
                (let [label (ui/padding 20 10
                              (ui/label name))]
                  (cond
                    selected? (ui/rect (paint/fill 0xFFB2D7FE) label)
                    hovered?  (ui/rect (paint/fill 0xFFE1EFFA) label)
                    :else     label)))))))
      border-line
      [:stretch 1
       (ui/clip
         (ui/dynamic _ [name @state/*example]
           (examples name)))])))

(reset! state/*app app)

(defn -main [& args]
  (ui/start-app!
    (let [screen (last (app/screens))]
      (reset! state/*window 
        (ui/window
          {:title    "Humble 🐝 UI"
           :mac-icon "dev/images/icon.icns"
           :screen   (:id screen)
           :width    800
           :height   600
           :x        :center
           :y        :center}
          state/*app))))
  (set-floating! @state/*window @state/*floating)
  (reset! debug/*enabled? true)
  (apply nrepl/-main args))
