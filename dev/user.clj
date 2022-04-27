(ns user
  (:require
    [clojure.string :as str]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.profile :as profile]
    [io.github.humbleui.window :as window]
    [io.github.humbleui.ui :as ui]
    [nrepl.cmdline :as nrepl])
  (:import
    [io.github.humbleui.skija FontMgr FontStyle Typeface Font]
    [io.github.humbleui.types IRect]))

(set! *warn-on-reflection* true)

(defonce *window (atom nil))

(def ^Typeface face-default
  #_(.matchFamiliesStyle (FontMgr/getDefault) (into-array String [".SF NS", "Helvetica Neue", "Arial"]) FontStyle/NORMAL)
  (Typeface/makeFromFile "dev/fonts/Inter-Regular.ttf"))

(defonce *floating (atom false))

(add-watch *floating ::window
  (fn [_ _ _ floating]
    (when-some [window @*window]
      (if floating
        (window/set-z-order window :floating)
        (window/set-z-order window :normal)))))

(def examples
  ["align"
   "button"
   "calculator"
   "canvas"
   "checkbox"
   "container"
   "event-bubbling"
   "label"
   "scroll"
   "svg"
   "text-field"
   "tree"
   "wordle"])

(defonce *example (atom "text-field"))

(def app
  (ui/dynamic ctx [scale (:scale ctx)]
    (let [font-ui   (Font. face-default (float (* 13 scale)))
          leading   (-> font-ui .getMetrics .getCapHeight Math/ceil (/ scale))
          fill-text (paint/fill 0xFF000000)]
      (ui/with-context {:face-ui   face-default
                        :font-ui   font-ui
                        :leading   leading
                        :fill-text fill-text}
        (ui/row
          (ui/column
            [:stretch 1
             (ui/vscrollbar
               (ui/vscroll
                 (ui/column
                   (for [name (sort examples)]
                     (ui/clickable
                       #(reset! *example name)
                       (ui/dynamic ctx [selected? (= name @*example)
                                        hovered?  (:hui/hovered? ctx)]
                         (let [label (ui/padding 20 leading
                                       (ui/label (-> name
                                                   (str/split #"-")
                                                   (->> (map str/capitalize)
                                                     (str/join " ")))))]
                           (cond
                             selected? (ui/fill (paint/fill 0xFFB2D7FE) label)
                             hovered?  (ui/fill (paint/fill 0xFFE1EFFA) label)
                             :else     label))))))))]
            (ui/padding 10 10
              (ui/checkbox *floating (ui/label "On top"))))
          [:stretch 1
           (ui/clip
             (ui/dynamic _ [ui @(requiring-resolve (symbol (str "examples." @*example) "ui"))]
               ui))])))))

(defn on-paint [window canvas]
  (canvas/clear canvas 0xFFF6F6F6)
  (let [bounds (window/content-rect window)
        ctx    {:scale (window/scale window)}]
    (profile/reset)
    ; (profile/measure "frame"
    (core/draw app ctx (IRect/makeXYWH 0 0 (:width bounds) (:height bounds)) canvas)
    (profile/log)
    #_(window/request-frame window)))

(defn redraw []
  (some-> @*window window/request-frame))

(add-watch *example ::redraw
  (fn [_ _ _ _]
    (redraw)))

(redraw)

(defn on-event [window event]
  (when-let [changed? (core/event app event)]
    (window/request-frame window)))

(defn make-window []
  (let [screen (last (app/screens))
        scale  (:scale screen)
        width  (* 600 scale)
        height (* 400 scale)
        area   (:work-area screen)
        x      (:x area)
        y      (-> (:height area) (- height) (/ 2) (+ (:y area)))]
    (doto
      (window/make
        {:on-close #(reset! *window nil)
         :on-paint #'on-paint
         :on-event #'on-event})
      (window/set-title "Humble UI 👋")
      (window/set-window-size width height)
      (window/set-window-position x y)
      (window/set-visible true))))

(defn -main [& args]
  (future (apply nrepl/-main args))
  (app/start #(reset! *window (make-window))))

(comment  
  (do
    (app/doui (some-> @*window window/close))
    (reset! *floating false)
    (reset! *window (app/doui (make-window))))
  
  (app/doui (window/set-z-order @*window :normal))
  (app/doui (window/set-z-order @*window :floating))
  )