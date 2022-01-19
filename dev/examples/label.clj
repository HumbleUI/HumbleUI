(ns examples.label
  (:require
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(def ui
  (ui/dynamic ctx [{:keys [font-ui leading fill-text]} ctx]
    (ui/label "Hello from Humble UI! 👋" font-ui fill-text)))