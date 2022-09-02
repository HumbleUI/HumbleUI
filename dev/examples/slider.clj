(ns examples.slider
  (:require
    [io.github.humbleui.ui :as ui]))

(set! *warn-on-reflection* true)

(def *state0
  (atom {:value 500
         :max   1000}))

(def *state1
  (atom {:value 12
         :min   -66
         :max   66
         :step  3}))

(def *state2
  (atom {:value 2
         :min 0
         :max 10}))

(def ui
  (ui/padding 20 20
    (ui/valign 0.5
      (ui/column
        (ui/row
          [:stretch 1 (ui/slider *state0)]
          (ui/valign 0.5
            (ui/width 100
              (ui/halign 1
              (ui/dynamic _ [value (:value @*state0)]
                (ui/label (str value " / 1000")))))))
        
        (ui/gap 0 10)
        
        (ui/row
          [:stretch 1 (ui/slider *state1)]
          (ui/valign 0.5
            (ui/width 100
              (ui/halign 1
              (ui/dynamic _ [value (:value @*state1)]
                (ui/label (str value " / 66")))))))
        
        (ui/gap 0 10)
        
        (ui/row
          [:stretch 1 (ui/slider *state2)]
          (ui/valign 0.5
          (ui/width 100
            (ui/dynamic _ [value (:value @*state2)]
              (ui/halign 1
              (ui/label (str value " / 10")))))))))))

(comment
  (reset! user/*example "slider"))