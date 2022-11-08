(ns ^{:clojure.tools.namespace.repl/load false}
  examples.state
  (:require
    [clojure.tools.namespace.repl :as ns]))

(def *window
  (atom nil))

(def *app
  (atom nil))

(def *example
  (atom "Animation"))

(def *floating
  (atom false))

(ns/set-refresh-dirs "src" "dev")

(defn reload []
  (set! *warn-on-reflection* true)
  ; (set! *unchecked-math* :warn-on-boxed)
  (let [res (ns/refresh)]
    (if (instance? Throwable res)
      (throw res)
      res)))
