(ns ^{:clojure.tools.namespace.repl/load false}
  examples.state
  (:require
    [io.github.humbleui.signal :as signal]))

(def *example
  (signal/signal "Container"))

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

(def *treemap-state
  (atom nil))

