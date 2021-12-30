(ns user
  (:require
   [io.github.humbleui.core :as hui]
   [io.github.humbleui.window :as window]
   [io.github.humbleui.ui :as ui]
   [nrepl.cmdline :as nrepl])
  (:import
   [io.github.humbleui.jwm App EventFrame EventMouseButton EventMouseMove Window]
   [io.github.humbleui.skija Canvas FontMgr FontStyle Typeface Font Paint]
   [io.github.humbleui.types IPoint]))

(set! *warn-on-reflection* true)

(defonce font-mgr (FontMgr/getDefault))

(defonce *window (atom nil))

(defonce ^Typeface face-default
  (.matchFamiliesStyle ^FontMgr font-mgr (into-array String [".SF NS", "Helvetica Neue", "Arial"]) FontStyle/NORMAL))

(defonce *clicks (atom 0))

(def app
  (ui/dynamic ctx [scale (:scale ctx)]
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
            (ui/dynamic _ [clicks @*clicks]
              (ui/label (str "Clicked: " clicks) font-default fill-text))
            (ui/gap 0 leading)
            (ui/clickable
              #(swap! *clicks inc)
              (ui/clip-rrect (* scale 4)
                (ui/dynamic ctx [active?  (:hui/active? ctx)
                                 hovered? (:hui/hovered? ctx)]
                  (let [[label fill] (cond
                                       active?  ["Active"    fill-button-active]
                                       hovered? ["Hovered"   fill-button-hovered]
                                       :else    ["Unpressed" fill-button-normal])]
                    (ui/fill fill
                      (ui/padding (* scale 20) leading
                        (ui/label label font-default fill-text)))))))))))))

(defn random-green []
  (let [r (+ 32  (rand-int 32))
        g (+ 192 (rand-int 32))
        b (+ 32  (rand-int 32))]
    (unchecked-int
      (bit-or
        (unchecked-int 0xFF000000)
        (bit-shift-left r 16)
        (bit-shift-left g 8)
        (bit-shift-left b 0)))))

(def new-year-app
  (ui/dynamic ctx [scale (:scale ctx)]
    (let [font       (Font. face-default (float (* 13 scale)))
          cap-height (.getCapHeight (.getMetrics font))
          fill-text  (doto (Paint.) (.setColor (unchecked-int 0xFFFFFFFF)))
          labels     (cycle (map #(ui/label (str %) font fill-text) "HappyNew2022!"))]
      (ui/halign 0.5
        (ui/padding 0 (* 10 scale)
          (ui/dynamic ctx [rows (quot (- (:height (:bounds ctx)) (* 10 scale)) (+ (* 11 scale) cap-height))
                           time (quot (System/currentTimeMillis) 1000)]
            (apply ui/column
              (interpose (ui/gap 0 (* 1 scale))
                (for [y (range rows)]
                  (ui/halign 0.5
                    (apply ui/row
                      (interpose (ui/gap (* 1 scale) 0)
                        (for [x (range (inc y))]
                          (if (= x y 0)
                            (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFFCC3333)))
                              (ui/padding (* 5 scale) (* 5 scale)
                                (ui/label "★" font fill-text)))
                              (ui/fill (doto (Paint.) (.setColor (random-green)))
                                (ui/padding (* 5 scale) (* 5 scale)
                                  (let [idx (+ x (* y (+ y 1) 1/2) -1)]
                                    (nth labels idx))))))))))))))))))

(comment
  (window/request-frame @*window))

(defn on-paint [window ^Canvas canvas]
  (.clear canvas (unchecked-int 0xFFF0F0F0))
  (let [bounds (window/content-rect window)
        ctx    {:bounds bounds :scale (window/scale window)}
        app    app]
    (ui/-layout app ctx (IPoint. (:width bounds) (:height bounds)))
    (ui/-draw app ctx canvas)
    (some-> @*window window/request-frame)))

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

                   nil)]
    (when changed?
      (window/request-frame window))))

(defn make-window []
  (doto
    (window/make
      {:on-close (fn [window] (window/close window) (reset! *window nil))
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