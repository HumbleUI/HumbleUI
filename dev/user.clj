(ns user
  (:require
    [clojure.core.server :as server]
    [clojure.tools.namespace.repl :as ns]
    [clojure.tools.namespace.track :as track]
    [state]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.window :as window]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija ColorSpace]))

(ns/disable-reload!)

(ns/set-refresh-dirs "src" "dev")

(def *reloaded
  (atom nil))

(add-watch #'ns/refresh-tracker ::watch
  (fn [_ _ old new]
    (when (empty? (::track/load new))
      (reset! *reloaded (::track/load old)))))

(defn after-reload []
  (reset! state/*app @(requiring-resolve (symbol @state/*ns "app")))
  (let [cnt (count @*reloaded)]
    (str "Reloaded " cnt " namespace" (when (> cnt 1) "s"))))

(defn reload []
  (set! *warn-on-reflection* true)
  ; (set! *unchecked-math* :warn-on-boxed)
  (let [res (ns/refresh :after 'user/after-reload)]
    (if (instance? Throwable res)
      (throw res)
      res)))

(def lock
  (Object.))

(defn position []
  (let [trace (->> (Thread/currentThread)
                (.getStackTrace)
                (seq))
        el    ^StackTraceElement (nth trace 4)]
    (str "[" (clojure.lang.Compiler/demunge (.getClassName el)) " " (.getFileName el) ":" (.getLineNumber el) "]")))

(defn p [form]
  `(let [res# ~form]
     (locking lock
       (println (str "#p" (position) " " '~form " => " res#)))
     res#))

(defn set-floating! [window floating]
  (when window
    (app/doui
      (if floating
        (window/set-z-order window :floating)
        (window/set-z-order window :normal)))))

(add-watch state/*floating ::window
  (fn [_ _ _ floating]
    (set-floating! @state/*window floating)))

(defn start [& args]
  ;; setup window
  (ui/start-app!
    (let [screen (last (app/screens))
          window (ui/window
                   {:title    "Humble 🐝 UI"
                    :mac-icon "dev/images/icon.icns"
                    :screen   (:id screen)
                    :width    800
                    :height   600
                    :x        :center
                    :y        :center}
                   state/*app)]
      ;; TODO load real monitor profile
      (when (= :macos app/platform)
        (set! (.-_colorSpace (.getLayer window)) (ColorSpace/getDisplayP3)))
      (set-floating! window @state/*floating)
      ; (reset! protocols/*debug? true)
      (deliver state/*window window)))
  @state/*window
  
  ;; setup app
  (let [args (apply array-map args)
        ns   (get args "--ns" "examples")]
    (reset! state/*ns ns)
    (user/after-reload)))

(defn -main [& args]
  (apply startargs)

  ;; setup repl
  (let [args (apply array-map args)
        port (parse-long (get args "--port" "5555"))]
    (println "Started Server Socket REPL on port" port)
    (server/start-server
      {:name          "repl"
       :port          port
       :accept        'clojure.core.server/repl
       :server-daemon false})))
