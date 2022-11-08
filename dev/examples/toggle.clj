(ns examples.toggle
  (:require
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]))

(def *state-first
  (atom true))

(def *state-second
  (atom false))

(add-watch *state-first :watch
  (fn [_ _ old new]
    (when (not= old new)
      (reset! *state-second (not new)))))

(def ui
  (ui/dynamic ctx [{:keys [face-ui scale]} ctx]
    (let [padding-inner  12
          fill-bg        (paint/fill 0xFFF2F2F2)
          stroke-bg      (paint/stroke 0xFFE0E0E0 (* 0.5 scale))
          fill-delimiter (paint/fill 0xFFE7E7E7)]
      (ui/padding 20 20
        (ui/valign 0
          (ui/rounded-rect {:radius 6} fill-bg
            (ui/rounded-rect {:radius 6} stroke-bg
              (ui/padding padding-inner padding-inner
                (ui/column
                  (ui/with-context
                    {:font-ui (font/make-with-cap-height face-ui (* 20 scale))}
                    (ui/row
                      (ui/valign 0.5
                        (ui/label "First state"))
                      [:stretch 1 nil]
                      (ui/toggle *state-first)))
                  (ui/gap 0 padding-inner)
                  (ui/rect fill-delimiter
                    (ui/gap 0 1))
                  (ui/gap 0 padding-inner)
                  (ui/row
                    (ui/valign 0.5
                      (ui/label "Second state"))
                    [:stretch 1 nil]
                    (ui/toggle *state-second)))))))))))
