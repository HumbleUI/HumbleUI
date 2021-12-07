(ns user
  (:require
   [io.github.humbleui.core :as hui]
   [io.github.humbleui.window :as window]
   [io.github.humbleui.ui :as ui]
   [nrepl.cmdline :as nrepl])
  (:import
   [io.github.humbleui.jwm App EventFrame EventMouseButton EventMouseMove]
   [io.github.humbleui.skija Canvas FontMgr FontStyle Typeface Font Paint Rect]
   [io.github.humbleui.window Window]))

(defonce font-mgr (FontMgr/getDefault))

(defonce *window (atom nil))

(defonce face-default
  (.matchFamiliesStyle font-mgr (into-array String [".SF NS", "Helvetica Neue", "Arial"]) FontStyle/NORMAL))

(def *clicks (atom 0))

(hui/defn-memoized-last app [scale]
  (let [font-default        (Font. face-default (float (* 13 scale)))
        leading             (.getCapHeight (.getMetrics font-default))
        fill-text           (doto (Paint.) (.setColor (unchecked-int 0xFF000000)))
        fill-button-normal  (doto (Paint.) (.setColor (unchecked-int 0xFFade8f4)))
        fill-button-hovered (doto (Paint.) (.setColor (unchecked-int 0xFFcaf0f8)))
        fill-button-active  (doto (Paint.) (.setColor (unchecked-int 0xFF48cae4)))]
    (ui/valign 0.5
      (ui/halign 0.5
        (ui/column
          (ui/label "Hello from Humble UI! 👋" font-default fill-text)
          (ui/gap 0 leading)
          (ui/contextual (fn [_] (ui/label (str "Clicked: " @*clicks) font-default fill-text)))
          (ui/gap 0 leading)
          (ui/clickable
            #(swap! *clicks inc)
            (ui/clip-rrect (* scale 4)
              (ui/contextual
                (fn [ctx]
                  (let [[label fill] (cond
                                       (:hui/active? ctx)  ["Active"    fill-button-active]
                                       (:hui/hovered? ctx) ["Hovered"   fill-button-hovered]
                                       :else               ["Unpressed" fill-button-normal])]
                    (ui/fill-solid fill
                      (ui/padding (* scale 20) leading
                        (ui/label label font-default fill-text)))))))))))))

(defn on-paint [window ^Canvas canvas]
  (.clear canvas (unchecked-int 0xFFF0F0F0))
  (let [app    (app (window/scale window))
        bounds (.getContentRect (window/jwm-window window))
        ctx    {}]
    (ui/-layout app ctx (hui/->Size (.getWidth bounds) (.getHeight bounds)))
    (ui/-draw app ctx canvas))
  (window/request-frame window))

(defn on-event [window event]
  (let [app (app (window/scale window))]
    (condp instance? event
      EventMouseMove
      (let [pos   (hui/->Point (.getX event) (.getY event))
            event {:hui/event :hui/mouse-move
                   :hui.event/pos pos}]
        (ui/-event app event))

      EventMouseButton
      (let [event {:hui/event :hui/mouse-button
                   :hui.event.mouse-button/is-pressed (.isPressed event)}]
        (ui/-event app event))

      nil)))

(comment
  (window/request-frame @*window))

(defn make-window []
  (doto
    (window/make
      {:on-close (fn [_] (reset! *window nil))
       :on-paint #'on-paint
       :on-event #'on-event})
    (window/set-title "Humble UI 👋")
    (window/set-content-size 810 650)
    (window/set-window-position 2994 630)
    (window/set-visible true)
    (window/set-z-order :floating)
    (window/request-frame)))

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