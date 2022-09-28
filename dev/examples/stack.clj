(ns examples.stack
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]))

(def ui
  (ui/center
    (ui/stack
      (ui/center
        (ui/rect (paint/fill 0xFFCCCCCC)
          (ui/gap 200 200)))
      (ui/center
        (ui/padding 100
          (ui/label "Stack")))
      (ui/center
        (ui/rect (paint/fill 0x80CC3333)
          (ui/gap 300 100))))))

; (reset! user/*example "stack")