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

(defn reload []
  (or
    (try
      (when-some [window @@(requiring-resolve 'examples.util/*window)]
        ;; do not reload in the middle of the frame
        (locking window
          (duti/reload)))
      (catch Exception e
        nil))
    (duti/reload)))

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
