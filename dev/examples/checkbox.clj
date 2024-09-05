(ns examples.checkbox
  (:require
    [io.github.humbleui.font :as font]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(def *state-group
  (ui/signal false))

(def *state-first
  (ui/signal false))

(def *state-second
  (ui/signal false))

(add-watch *state-group :watch
  (fn [_ _ old new]
    (when (and (not= old new)
            (or (true? new) (false? new)))
      (reset! *state-first new)
      (reset! *state-second new))))

(add-watch *state-first :watch
  (fn [_ _ old new]
    (when (not= old new)
      (reset! *state-group
        (cond
          (every? true? [new @*state-second])  true
          (every? false? [new @*state-second]) false
          :else                                :mixed)))))

(defn ui []
  [ui/align {:y :center}
   [ui/vscroll
    [ui/align {:x :center}
     [ui/padding {:padding 20}
      [ui/column {:gap 10}
       [ui/with-context {:font-cap-height 20}
        [ui/checkbox {:*value *state-group} [ui/label "Group state"]]]
       [ui/checkbox {:*value *state-first} [ui/label "First state"]]
       ;; on-change
       ;; string label
       [ui/checkbox
        {:*value *state-second
         :on-change
         (fn [state-second]
           (condp = [@*state-first state-second]
             [true true]   (reset! *state-group true)
             [false false] (reset! *state-group false)
             #_else        (reset! *state-group :mixed)))}
        "Second state"]]]]]])
