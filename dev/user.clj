(ns user
  (:require
    [io.github.humbleui.core :as hui]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.profile :as profile]
    [io.github.humbleui.window :as window]
    [io.github.humbleui.ui :as ui]
    [nrepl.cmdline :as nrepl]
    [examples.align]
    [examples.button]
    [examples.calculator]
    [examples.container]
    [examples.label]
    [examples.scroll]
    [examples.tree]
    [examples.wordle])
  (:import
    [io.github.humbleui.jwm App EventFrame EventMouseButton EventMouseMove EventMouseScroll EventKey Window]
    [io.github.humbleui.skija Canvas FontMgr FontStyle Typeface Font Paint PaintMode]
    [io.github.humbleui.types IPoint IRect]))

(set! *warn-on-reflection* true)

(defonce *window (atom nil))

(def ^Typeface face-default
  #_(.matchFamiliesStyle (FontMgr/getDefault) (into-array String [".SF NS", "Helvetica Neue", "Arial"]) FontStyle/NORMAL)
  (Typeface/makeFromFile "dev/fonts/Inter-Regular.ttf"))

(defonce *example (atom "Wordle"))

(defonce *floating (atom false))

(add-watch *floating ::window
  (fn [_ _ _ floating]
    (when-some [window @*window]
      (if floating
        (window/set-z-order window :floating)
        (window/set-z-order window :normal)))))

(def examples
  {"Align"      examples.align/ui
   "Button"     examples.button/ui
   "Calculator" examples.calculator/ui
   "Container"  examples.container/ui
   "Label"      examples.label/ui
   "Scroll"     examples.scroll/ui
   "Tree"       examples.tree/ui
   "Wordle"     examples.wordle/ui})

(defn checkbox [*checked text]
  (ui/clickable
    #(swap! *checked not)
    (ui/dynamic ctx [checked @*checked
                     {:keys [font-ui fill-text leading scale]} ctx]
      (let [border (doto (Paint.)
                     (.setColor (unchecked-int 0xFF000000))
                     (.setMode PaintMode/STROKE)
                     (.setStrokeWidth (* 1 scale)))]
        (ui/row
          (ui/fill border
            (if checked
              (ui/padding 1 1
                (ui/fill fill-text
                  (ui/gap (- leading 2) (- leading 2))))
              (ui/gap leading leading)))
          (ui/gap 5 0)
          (ui/label text font-ui fill-text))))))

(def app
  (ui/dynamic ctx [scale (:scale ctx)]
    (let [font-ui   (Font. face-default (float (* 13 scale)))
          leading   (-> font-ui .getMetrics .getCapHeight Math/ceil (/ scale))
          fill-text (doto (Paint.) (.setColor (unchecked-int 0xFF000000)))]
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
                   (for [[name ui] (sort-by first examples)]
                     (ui/clickable
                       #(reset! *example name)
                       (ui/dynamic ctx [selected? (= name @*example)
                                        hovered?  (:hui/hovered? ctx)]
                         (let [label (ui/padding 20 leading
                                       (ui/label name font-ui fill-text))]
                           (cond
                             selected? (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFFB2D7FE))) label)
                             hovered?  (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFFE1EFFA))) label)
                             :else     label))))))))]
            (ui/padding 10 10
              (checkbox *floating "On top")))
          [:stretch 1
           (ui/dynamic _ [name @*example]
             (examples name))])))))

(defn on-paint [window ^Canvas canvas]
  (.clear canvas (unchecked-int 0xFFF6F6F6))
  (let [bounds (window/content-rect window)
        ctx    {:scale (window/scale window)}]
    (profile/reset)
    ; (profile/measure "frame"
    (ui/draw app ctx (IRect/makeXYWH 0 0 (:width bounds) (:height bounds)) canvas)
    (profile/log)
    #_(window/request-frame window)))

(some-> @*window window/request-frame)

(defn on-event [window event]
  (when-let [changed? (ui/event app event)]
    (window/request-frame window)))

(defn make-window []
  (let [screen (last (hui/screens))
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
  (hui/start #(reset! *window (make-window))))

(comment  
  (do
    (hui/doui (some-> @*window window/close))
    (reset! *floating false)
    (reset! *window (hui/doui (make-window))))
  
  (hui/doui (window/set-z-order @*window :normal))
  (hui/doui (window/set-z-order @*window :floating))
  )