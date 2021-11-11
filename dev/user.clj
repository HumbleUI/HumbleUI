(ns user
  (:require
   [io.github.humbleui.core :as hui]
   [io.github.humbleui.window :as window]
   [nrepl.cmdline :as nrepl])
  (:import
   [io.github.humbleui.jwm App EventFrame]
   [io.github.humbleui.skija Canvas Paint Rect]
   [io.github.humbleui.window Window]))

(def *window (atom nil))

(defn on-paint [^Canvas canvas]
  (when-some [^Window window @*window]
    (.clear canvas (unchecked-int 0xFFEEEEEE))
    (with-open [paint (Paint.)]
      (.setColor paint (unchecked-int 0xFFCC3333))
      (let [bounds  (.getContentRect (.-jwm-window window))
            scale   (.getScale (.getScreen (.-jwm-window window)))
            rect    (Rect/makeXYWH (* -5 scale) (* -5 scale) (* 10 scale) (* 10 scale))
            angle   (mod (/ (System/currentTimeMillis) 5) 360)]
        (doseq [x [(* 15 scale) (- (.getWidth bounds) (* 15 scale))]
                y [(* 15 scale) (- (.getHeight bounds) (* 15 scale))]]
          (.save canvas)
          (.translate canvas x y)
          (.rotate canvas angle)
          (.drawRect canvas rect paint)
          (.restore canvas))))
    (window/request-frame window)))

(defn make-window []
  (doto
    (window/make
      {:on-close #(reset! *window nil)
       :on-paint #(on-paint %)})
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