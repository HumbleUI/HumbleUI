(ns user
  (:require
    [io.github.humbleui.core :as hui]
    [io.github.humbleui.window :as window]
    [io.github.humbleui.ui :as ui]
    [nrepl.cmdline :as nrepl]
    [examples.align]
    [examples.button]
    [examples.calculator]
    [examples.container]
    [examples.label]
    [examples.scroll]
    [examples.tree])
  (:import
    [io.github.humbleui.jwm App EventFrame EventMouseButton EventMouseMove EventMouseScroll Window]
    [io.github.humbleui.skija Canvas FontMgr FontStyle Typeface Font Paint]
    [io.github.humbleui.types IPoint]))

(set! *warn-on-reflection* true)

(defonce *window (atom nil))

(defonce ^Typeface face-default
  (.matchFamiliesStyle (FontMgr/getDefault) (into-array String [".SF NS", "Helvetica Neue", "Arial"]) FontStyle/NORMAL))

(def *example (atom examples.calculator/ui))

(def examples
  {"Align"      examples.align/ui
   "Button"     examples.button/ui
   "Calculator" examples.calculator/ui
   "Container"  examples.container/ui
   "Label"      examples.label/ui
   "Scroll"     examples.scroll/ui
   "Tree"       examples.tree/ui})

(def app
  (ui/dynamic ctx [scale (:scale ctx)]
    (let [font-ui   (Font. face-default (float (* 13 scale)))
          leading   (-> font-ui .getMetrics .getCapHeight (/ scale) Math/ceil)
          fill-text (doto (Paint.) (.setColor (unchecked-int 0xFF000000)))]
      (ui/row
        [:hug nil (ui/vscrollbar
                    (ui/vscroll
                      (apply ui/column
                        (for [[name ui] (sort-by first examples)]
                          [:hug nil (ui/clickable
                                      #(reset! *example ui)
                                      (ui/dynamic ctx [selected? (= ui @*example)
                                                       hovered?  (:hui/hovered? ctx)]
                                        (let [label (ui/padding 20 leading
                                                      (ui/label name font-ui fill-text))]
                                          (cond
                                            selected? (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFFB2D7FE))) label)
                                            hovered?  (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFFE1EFFA))) label)
                                            :else     label))))]))))]
        [:stretch 1 (ui/with-context {:font-ui   font-ui
                                      :leading   leading
                                      :fill-text fill-text}
                      (ui/dynamic _ [example @*example]
                        example))]))))

(defn on-paint [window ^Canvas canvas]
  (.clear canvas (unchecked-int 0xFFF6F6F6))
  (let [bounds (window/content-rect window)
        ctx    {:bounds bounds
                :scale  (window/scale window)}]
    (ui/draw app ctx bounds canvas)
    #_(window/request-frame window)))

(some-> @*window window/request-frame)

(defn on-event [window event]
  (let [changed? (condp instance? event
                   EventMouseMove
                   (let [pos   (IPoint. (.getX ^EventMouseMove event) (.getY ^EventMouseMove event))
                         event {:hui/event :hui/mouse-move
                                :hui.event/pos pos}]
                     (ui/event app event))
                   
                   EventMouseButton
                   (let [event {:hui/event :hui/mouse-button
                                :hui.event.mouse-button/is-pressed (.isPressed ^EventMouseButton event)}]
                     (ui/event app event))
                   
                   EventMouseScroll
                   (ui/event app
                     {:hui/event :hui/mouse-scroll
                      :hui.event.mouse-scroll/dx (.getDeltaX ^EventMouseScroll event)
                      :hui.event.mouse-scroll/dy (.getDeltaY ^EventMouseScroll event)})
                   
                   nil)]
    (when changed?
      (window/request-frame window))))

(defn make-window []
  (let [[x y width height] (if-some [screen (second (hui/screens))]
                             (let [area (:work-area screen)]
                               [(:x area)
                                (+ (:y area) (/ (:height area) 4))
                                (* (:width area) 0.33)
                                (* (:height area) 0.5)])
                             (let [area (:work-area (hui/primary-screen))]
                               [(+ (:x area) (* (:width area) 0.75))
                                (+ (:y area) (/ (:height area) 4))
                                (* (:width area) 0.25)
                                (* (:height area) 0.5)]))]
    (doto
      (window/make
        {:on-close #(reset! *window nil)
         :on-paint #'on-paint
         :on-event #'on-event})
      (window/set-title "Humble UI 👋")
      (window/set-window-size width height)
      (window/set-window-position x y)
      (window/set-visible true)
      #_(window/set-z-order :floating))))

(defn -main [& args]
  (future (apply nrepl/-main args))
  (hui/init)
  (reset! *window (make-window))
  (hui/start))

(comment
  (do
    (hui/doui (some-> @*window window/close))
    (reset! *window (hui/doui (make-window))))
  
  (hui/doui (window/set-z-order @*window :normal))
  (hui/doui (window/set-z-order @*window :floating))
  )