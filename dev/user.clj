(ns user
  (:require
   [io.github.humbleui.core :as hui]
   [io.github.humbleui.window :as window]
   [io.github.humbleui.ui :as ui]
   [nrepl.cmdline :as nrepl])
  (:import
   [io.github.humbleui.jwm App EventFrame]
   [io.github.humbleui.skija Canvas FontMgr FontStyle Typeface Font Paint Rect]
   [io.github.humbleui.window Window]))

(defonce font-mgr (FontMgr/getDefault))

(defonce *window (atom nil))

(defonce *face-default (atom (.matchFamiliesStyle font-mgr (into-array String [".SF NS", "Helvetica Neue", "Arial"]) FontStyle/NORMAL)))

(defonce *font-default (atom nil))

(defonce *paint-fg (atom (doto (Paint.) (.setColor (unchecked-int 0xFF000000)))))

(def t0 (System/currentTimeMillis))

(defn on-paint [window ^Canvas canvas]
  (.clear canvas (unchecked-int 0xFFF0F0F0))
  (let [bounds (.getContentRect (window/jwm-window window))
        dt     (- (System/currentTimeMillis) t0)
        ms     (mod dt 1000)
        sec    (-> dt (quot 1000) (mod 60) int)
        min    (-> dt (quot 1000) (quot 60) (mod 60) int)
        hrs    (-> dt (quot 1000) (quot 60) (quot 60) (mod 60))
        time   (format "%02d:%02d:%02d.%03d" hrs min sec ms)]
    (with-open [ui (ui/valign 0.5
                     (ui/halign 0.5
                       (ui/column
                         (ui/label "Hello, Humble UI!" @*font-default @*paint-fg)
                         (let []
                          (ui/label time @*font-default @*paint-fg)))))]
      (ui/-draw ui canvas (ui/->Size (.getWidth bounds) (.getHeight bounds)))))
  (window/request-frame window))

(defn make-window []
  (doto
    (window/make
      {:on-screen-change
       (fn [window]
         (let [scale (.getScale (.getScreen (window/jwm-window window)))]
           (when-some [font @*font-default]
             (.close font))
           (reset! *font-default (Font. @*face-default (float (* 13 scale))))))
       :on-close (fn [_] (reset! *window nil))
       :on-paint (fn [window canvas] ((resolve 'on-paint) window canvas))})
    (window/set-title "Hello from Humble UI")
    (window/set-visible true)
    (window/set-z-order :floating)
    (window/request-frame)))

(defn -main [& args]
  (future (apply nrepl/-main args))
  (hui/init)
  (reset! *window (make-window))
  (hui/start))

(comment
  (reset! *window (hui/doui (make-window)))
  (hui/doui (window/close @*window))

  @*window

  (hui/doui (window/set-title @*window "Look, another title!"))

  (hui/doui (window/set-z-order @*window :normal))
  (hui/doui (window/set-z-order @*window :floating))
)