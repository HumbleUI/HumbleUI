(ns examples.switch
  (:require
    [io.github.humbleui.font :as font]
        [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(def *state-first
  (ui/signal true))

(def *state-second
  (ui/signal false))

(add-watch *state-first :watch
  (fn [_ _ old new]
    (when (not= old new)
      (reset! *state-second (not new)))))

(defn ui []
  (let [padding-inner   12
        fill-bg         {:fill 0xFFF2F2F2}
        stroke-bg       {:stroke 0xFFE0E0E0, :width 0.5}
        fill-delimiter  {:fill 0xFFE7E7E7}]
    (fn []
      [ui/align {:y :center}
       [ui/vscroll
        [ui/align {:x :center}
         [ui/padding {:padding 20}
          [ui/rect {:radius 6, :paint fill-bg}
           [ui/rect {:radius 6, :paint stroke-bg}
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
