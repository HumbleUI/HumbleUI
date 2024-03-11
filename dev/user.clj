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

(defn reload [& [opts]]
  (set! *warn-on-reflection* true)
  (let [res (reload/reload opts)
        cnt (count (:loaded res))]
    (str "Reloaded " cnt " namespace" (when (not= 1 cnt) "s"))))

(defn -main [& args]
  (let [args (apply array-map args)
        ;; starting app
        _    (set! *warn-on-reflection* true)
        _    (@(requiring-resolve 'examples/-main))
        ;; starting socket repl
        port (some-> (get args "--port") parse-long)
        _    (duti/start-socket-repl {:port port})]))

(defn test-all []
  (reload {:only #"io\.github\.humbleui\.[^\.]+-test"})
  (duti/test #"io\.github\.humbleui\..*-test"))

(defn -test-main [_]
  (reload {:only #"io\.github\.humbleui\.[^\.]+-test"})
  (duti/test-exit #"io\.github\.humbleui\..*-test"))
