(ns user
  (:require
    [clojure.string :as str]
    [examples.settings :as settings]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.debug :as debug]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.profile :as profile]
    [io.github.humbleui.window :as window]
    [io.github.humbleui.ui :as ui]
    [nrepl.cmdline :as nrepl])
  (:import
    [io.github.humbleui.skija FontMgr FontStyle Typeface Font]
    [io.github.humbleui.types IRect]))

(defonce *window
  (atom nil))

(defn set-floating! [window floating]
  (when window
    (if floating
      (window/set-z-order window :floating)
      (window/set-z-order window :normal))))

(add-watch settings/*floating ::window
  (fn [_ _ _ floating]
    (set-floating! @*window floating)))

(def examples
  ["7guis-converter"
   "align"
   "animation"
   "backdrop"
   "bmi-calculator"
   "button"
   "calculator"
   "canvas"
   "checkbox"
   "container"
   "errors"
   "event-bubbling"
   "image-snapshot"
   "label"
   "scroll"
   "settings"
   "slider"
   "stack"
   "svg"
   "text-field"
   "text-field-debug"
   "toggle"
   "tooltip"
   "tree"
   "wordle"])

(def example-names
  {"7guis-converter" "7 GUIs: Converter"
   "bmi-calculator"  "BMI Calculator"})

(defonce *example
  (atom "animation"))

(defn- capitalize [s]
  (-> s
    (str/split #"-")
    (->> (map str/capitalize)
      (str/join " "))))

(defn next-example []
  (let [example @*example]
    (->>
      (drop-while #(not= % example) examples)
      (next)
      (first))))

(defn prev-example []
  (let [example @*example]
    (->>
      (drop-while #(not= % example) (reverse examples))
      (next)
      (first))))

(def light-grey 0xffeeeeee)   

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
        (ui/vscroll
          (ui/dynamic _ [examples examples
                         example-names example-names]
            (ui/column
              (for [ns examples
                    :let [name (or (example-names ns) (capitalize ns))]]
                (ui/clickable
                  {:on-click (fn [_] (reset! *example ns))}
                  (ui/dynamic ctx [selected? (= ns @*example)
                                   hovered?  (:hui/hovered? ctx)]
                    (let [label (ui/padding 20 10
                                  (ui/label name))]
                      (cond
                        selected? (ui/rect (paint/fill 0xFFB2D7FE) label)
                        hovered?  (ui/rect (paint/fill 0xFFE1EFFA) label)
                        :else     label)))))))))
      border-line
      [:stretch 1
       (ui/clip
         (ui/dynamic _ [ui @(requiring-resolve (symbol (str "examples." @*example) "ui"))]
           ui))])))

(defn redraw []
  (some-> @*window window/request-frame))

(add-watch *example ::redraw
  (fn [_ _ _ _]
    (redraw)))

(redraw)

(defn -main [& args]
  (ui/start-app!
    (let [{:keys [scale work-area]} (app/primary-screen)
          width (quot (:width work-area) 3)]
      (reset! *window 
        (ui/window
          {:title    "Humble 🐝 UI"
           :mac-icon "dev/images/icon.icns"
           :width    (/ width scale)
           :height   400
           :x        :left
           :y        :center}
          #'app))))
  (set-floating! @*window @settings/*floating)
  (reset! debug/*enabled? true)
  (redraw)
  (apply nrepl/-main args))

(comment  
  (do
    (app/doui (some-> @*window window/close))
    (reset! settings/*floating false)
    (reset! *window (app/doui (make-window))))
  
  (app/doui (window/set-z-order @*window :normal))
  (app/doui (window/set-z-order @*window :floating)))
  