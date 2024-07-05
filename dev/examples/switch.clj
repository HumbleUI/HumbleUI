(ns examples.switch
  (:require
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(def *state-first
  (signal/signal true))

(def *state-second
  (signal/signal false))

(add-watch *state-first :watch
  (fn [_ _ old new]
    (when (not= old new)
      (reset! *state-second (not new)))))

(defn ui []
  (let [padding-inner   12
        fill-bg         (paint/fill 0xFFF2F2F2)
        stroke-bg       (paint/stroke 0xFFE0E0E0 (ui/scaled 0.5))
        fill-delimiter  (paint/fill 0xFFE7E7E7)]
    (fn []
      [ui/align {:y :center}
       [ui/vscrollbar
        [ui/align {:x :center}
         [ui/padding {:padding 20}
          [ui/rounded-rect {:radius 6, :paint fill-bg}
           [ui/rounded-rect {:radius 6, :paint stroke-bg}
            [ui/padding {:padding padding-inner}
             [ui/column {:gap padding-inner}
              [ui/with-context {:font-cap-height 20}
               [ui/row
                [ui/align {:y :center}
                 [ui/label "First state"]]
                ^{:stretch 1} [ui/gap {:width 20}]
                [ui/switch {:*value *state-first}]]]
              [ui/rect {:paint fill-delimiter}
               [ui/gap {:height 1}]]
              [ui/row
               [ui/align {:y :center}
                [ui/label "Second state"]]
               ^{:stretch 1} [ui/gap {:width 20}]
               [ui/switch {:*value *state-second}]]]]]]]]]])))
