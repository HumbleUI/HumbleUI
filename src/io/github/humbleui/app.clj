(ns io.github.humbleui.app
  (:import
    [io.github.humbleui.jwm App Platform Screen]
    [io.github.humbleui.jwm.impl Library]))

(defonce *started?
  (atom false))

(def platform
  (condp = Platform/CURRENT
    Platform/WINDOWS :windows
    Platform/X11     :x11
    Platform/MACOS   :macos))

(defmacro doui-async [& forms]
  `(let [p# (promise)]
     (App/runOnUIThread #(deliver p# (try ~@forms (catch Throwable t# t#))))
     p#))

(defmacro doui [& forms]
  `(let [res# (deref (doui-async ~@forms))]
     (if (instance? Throwable res#)
       (throw res#)
       res#)))

(defn init []
  (Library/load))

(defn start [^Runnable cb]
  (if @*started?
    (doui
      (cb))
    (do
      (reset! *started? true)
      (App/start cb))))

(defn terminate []
  (reset! *started? false)
  (App/terminate))

(defn- screen->clj [^Screen screen]
  {:id        (.getId screen)
   :primary?  (.isPrimary screen)
   :bounds    (.getBounds screen)
   :work-area (.getWorkArea screen)
   :scale     (.getScale screen)})

(defn primary-screen []
  (screen->clj (App/getPrimaryScreen)))

(defn screens []
  (mapv screen->clj (App/getScreens)))

(defn open-symbols-palette []
  (App/openSymbolsPalette))
