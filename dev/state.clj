(ns ^{:clojure.tools.namespace.repl/load false}
  state
  (:require
    [io.github.humbleui.window :as window]))

(def *ns
  "Current “main” namespace that is used to render examples"
  (atom nil))

(def *window
  (promise))

(def *app
  (atom nil))

(def *floating
  (atom false))

(defn request-frame []
  (some-> @*window window/request-frame))
