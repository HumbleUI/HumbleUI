(ns user
  (:require
    [io.github.humbleui.core :as hui]
    [io.github.humbleui.window :as window]
    [io.github.humbleui.ui :as ui]
    [nrepl.cmdline :as nrepl]
    [examples.button]
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

(def *example (atom examples.label/ui))

(def examples
  {"Button" examples.button/ui
   "Label"  examples.label/ui
   "Scroll" examples.scroll/ui
   "Tree"   examples.tree/ui})

(def app
  (ui/dynamic ctx [scale (:scale ctx)]
    (let [font-ui   (Font. face-default (float (* 13 scale)))
          leading   (-> font-ui .getMetrics .getCapHeight (/ scale) Math/ceil)
          fill-text (doto (Paint.) (.setColor (unchecked-int 0xFF000000)))]
      (ui/row
        (ui/vscrollbar
          (ui/vscroll
            (apply ui/column
              (for [[name ui] (sort-by first examples)]
                (ui/clickable
                  #(reset! *example ui)
                  (ui/dynamic ctx [selected? (= ui @*example)
                                   hovered?  (:hui/hovered? ctx)]
                    (let [label (ui/padding 20 leading
                                  (ui/label name font-ui fill-text))]
                      (cond
                        selected? (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFF48cae4))) label)
                        hovered?  (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFFcaf0f8))) label)
                        :else     label))))))))
        (ui/with-context {:font-ui   font-ui
                          :leading   leading
                          :fill-text fill-text}
          (ui/valign 0.5
            (ui/halign 0.5
              (ui/dynamic _ [example @*example]
                example))))))))

(defn on-paint [window ^Canvas canvas]
  (.clear canvas (unchecked-int 0xFFF0F0F0))
  (let [bounds (window/content-rect window)
        ctx    {:bounds bounds
                :scale  (window/scale window)}
        app    app]
    (ui/-layout app ctx (IPoint. (:width bounds) (:height bounds)))
    (ui/-draw app ctx canvas)
    #_(window/request-frame window)))

(some-> @*window window/request-frame)

(defn on-event [window event]
  (let [app      app
        changed? (condp instance? event
                   EventMouseMove
                   (let [pos   (IPoint. (.getX ^EventMouseMove event) (.getY ^EventMouseMove event))
                         event {:hui/event :hui/mouse-move
                                :hui.event/pos pos}]
                     (ui/-event app event))
                   
                   EventMouseButton
                   (let [event {:hui/event :hui/mouse-button
                                :hui.event.mouse-button/is-pressed (.isPressed ^EventMouseButton event)}]
                     (ui/-event app event))
                   
                   EventMouseScroll
                   (ui/-event app
                     {:hui/event :hui/mouse-scroll
                      :hui.event.mouse-scroll/dx (.getDeltaX ^EventMouseScroll event)
                      :hui.event.mouse-scroll/dy (.getDeltaY ^EventMouseScroll event)})
                   
                   nil)]
    (when changed?
      (window/request-frame window))))

(defn make-window []
  (let [{:keys [work-area]} (hui/primary-screen)
        window-width  (/ (:width work-area) 4)
        window-height (/ (:height work-area) 2)
        window-left   (- (:right work-area) window-width)
        window-top    (-> (:y work-area)
                        (+ (/ (:height work-area) 2))
                        (- (/ window-height 2)))]
    (doto
      (window/make
        {:on-close #(reset! *window nil)
         :on-paint #'on-paint
         :on-event #'on-event})
      (window/set-title "Humble UI 👋")
      (window/set-window-size window-width window-height)
      (window/set-window-position window-left window-top)
      (window/set-visible true)
      (window/set-z-order :floating))))

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