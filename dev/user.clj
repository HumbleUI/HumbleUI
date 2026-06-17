(ns user
  (:require
    [clj-reload.core :as reload]
    [clojure+.core.server :as server]
    [clojure+.test :as test]))

(reload/init
  {:dirs ["src" "dev" "test"]
   :no-reload '#{user
                 io.github.humbleui.protocols
                 io.github.humbleui.signal}})

(test/install!)

(def test-re
  #"io\.github\.humbleui\..*-test")

(def ^:dynamic *t0*)

(def monitor)

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
    ;; do not reload in the middle of the frame
    (locking monitor
      (reload/reload))))

(defn -main [& args]
  (let [args (apply array-map args)
        ;; starting app
        _      (set! *warn-on-reflection* true)
        window (@(requiring-resolve 'examples/-main))
        _      (alter-var-root #'monitor (constantly window)) 
        ;; starting socket repl
        port   (some-> (get args "--port") parse-long)
        _      (server/start-server {:port port})]))

(defn test-all []
  (reload/reload {:only test-re})
  (test/run test-re))

(defn -test-main [_]
  (let [{:keys [fail error]} (test-all)]
    (System/exit (+ fail error))))
