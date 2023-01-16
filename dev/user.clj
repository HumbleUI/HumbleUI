(ns ^{:clojure.tools.namespace.repl/load false}
  user
  (:require
    [clojure.tools.namespace.repl :as ns]))

(ns/set-refresh-dirs "src" "dev")

(defn reload []
  (set! *warn-on-reflection* true)
  ; (set! *unchecked-math* :warn-on-boxed)
  (let [res (ns/refresh)]
    (if (instance? Throwable res)
      (throw res)
      res)))

