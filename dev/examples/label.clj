(ns examples.label
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(defn use-timer []
  (let [*state (signal/signal 0)
        cancel (core/schedule
                 #(swap! *state inc) 0 1000)]
    {:value *state
     :after-unmount cancel}))

(ui/defcomp static []
  [ui/label "Static"])

(ui/defcomp dynamic-comp [*timer]
  [ui/label "Dynamic component: " @*timer])

(ui/defcomp dynamic-label [*timer]
  [ui/label "Dynamic label: " *timer])

(ui/defcomp ui []
  (ui/with [*timer (use-timer)]
    (fn []
      [ui/center
       [ui/column {:gap 10}
        [static]
        [dynamic-comp *timer]
        [dynamic-label *timer]]])))
