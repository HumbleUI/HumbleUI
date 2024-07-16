(ns user
  (:require
    [clojure.core.server :as server]
    [clojure.java.io :as io]
    [clojure.test :as t]
    [clj-reload.core :as reload]
    [duti.core :as duti]))

(reload/init
  {:dirs ["src" "dev" "test"]
   :no-reload '#{user
                 io.github.humbleui.protocols
                 io.github.humbleui.signal}})

(def ^:dynamic *t0*)

(defn log [& args]
  (let [dt    (- (System/currentTimeMillis) *t0*)
        mins  (quot dt 60000)
        secs  (mod (quot dt 1000) 60)
        msecs (mod dt 1000)]
    (apply println (format "%02d:%02d.%03d" mins secs msecs) args))
  (flush))

(defn reload []
  (binding [*t0*                     (System/currentTimeMillis)
            clj-reload.util/*log-fn* log]
    (or
      (try
        (when-some [window @@(requiring-resolve 'examples.shared/*window)]
          ;; do not reload in the middle of the frame
          (locking window
            (duti/reload)))
        (catch Exception e
          nil))
      (duti/reload))))

(defn -main [& args]
  (let [args (apply array-map args)
        ;; starting app
        _    (set! *warn-on-reflection* true)
        _    (@(requiring-resolve 'examples/-main))
        ;; starting socket repl
        port (some-> (get args "--port") parse-long)
        _    (duti/start-socket-repl {:port port})]))

(defn test-all []
  (duti/test #"io\.github\.humbleui\..*-test"))

(defn -test-main [_]
  (duti/test-exit #"io\.github\.humbleui\..*-test"))
