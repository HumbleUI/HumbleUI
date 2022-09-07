(ns examples.stack
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]))

(set! *warn-on-reflection* true)

(def ui
  (ui/valign 0.5
    (ui/halign 0.5
      (ui/stack
        (ui/valign 0.5
          (ui/halign 0.5
            (ui/rect (paint/fill 0xFFCCCCCC)
              (ui/gap 200 200))))
        (ui/valign 0.5
          (ui/halign 0.5
            (ui/padding 100
              (ui/label "Stack"))))
        (ui/valign 0.5
          (ui/halign 0.5
            (ui/rect (paint/fill 0x80CC3333)
              (ui/gap 300 100))))))))

; (reset! user/*example "stack")