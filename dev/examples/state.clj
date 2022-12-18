(ns ^{:clojure.tools.namespace.repl/load false}
  examples.state
  (:require
    [clojure.tools.namespace.repl :as ns]))

(def *window
  (atom nil))

(def *app
  (atom nil))

(def *example
  (atom "Text Field"))

(def *todomvc-state
  (atom
    {:new-todo {:placeholder "What needs to be done?"}
     :mode     :all
     :next-id  3
     :todos    (sorted-map
                 0 {:label "first"
                    :completed? false}
                 1 {:label "second"
                    :completed? true}
                 2 {:label "third"
                    :completed? false})}))

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
