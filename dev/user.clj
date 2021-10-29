(ns user
  (:require
   [io.github.humbleui.core :as hui]
   [io.github.humbleui.window :as window]
   [nrepl.cmdline :as nrepl])
  (:import
   [io.github.humbleui.jwm App]))

(def *window (atom nil))

(defn make-window []
  (doto
    (window/make
      {:on-close #(reset! *window nil)})
    (window/set-title "Hello from Humble UI")
    (window/set-visible true)
    (window/set-z-order :floating)))

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