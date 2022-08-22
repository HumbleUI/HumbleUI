(ns examples.align
  (:require
    [clojure.string :as str]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(defn label [text]
  (ui/rect (paint/fill 0xFFB2D7FE)
    (ui/halign 0.5
      (ui/valign 0.5
        (ui/padding 10 10
          (ui/label text))))))

(def ui
  (ui/valign 0.5
    (ui/row
      [:stretch 1 nil]
      [:stretch 2 (ui/rect (paint/fill 0xFFE1EFFA)
                    (ui/column
                      [:stretch 1 (ui/halign 1 0   (label "Right to left (1 0)"))]
                      (ui/gap 0 1)
                      [:stretch 1 (ui/halign 0.5 0 (label "Center to left (0.5 0)"))]
                      (ui/gap 0 1)
                      [:stretch 1 (ui/halign 0.6 0.2   (label "Arbitrary (0.6 0.2)"))]
                      (ui/gap 0 1)
                      [:stretch 1 (ui/halign 0     (label "Left to left (0 0)"))]
                      (ui/gap 0 1)
                      [:stretch 1 (ui/halign 1 0.5 (label "Right to center (1 0.5)"))]
                      (ui/gap 0 1)
                      [:stretch 1 (ui/halign 0.5   (label "Center to center (0.5 0.5)"))]
                      (ui/gap 0 1)
                      [:stretch 1 (ui/halign 0 0.5 (label "Left to center (0 0.5)"))]
                      (ui/gap 0 1)
                      [:stretch 1 (ui/halign 1 1   (label "Right to right (1 1)"))]
                      (ui/gap 0 1)
                      [:stretch 1 (ui/halign 0.5 1 (label "Center to right (0.5 1)"))]
                      (ui/gap 0 1)
                      [:stretch 1 (ui/halign 0 1   (label "Left to right (0 1)"))]
                      (ui/gap 0 1)
                      [:stretch 1 (label "Stretch")]))]
      [:stretch 1 nil]
      [:stretch 2 (ui/rect (paint/fill 0xFFE1EFFA)
                    (ui/row
                      [:stretch 1 (ui/valign 1 0   (label "Bottom to top"))]
                      (ui/gap 1 0)
                      [:stretch 1 (ui/valign 0.5 0 (label "Middle to top"))]
                      (ui/gap 1 0)
                      [:stretch 1 (ui/valign 0     (label "Top to top"))]
                      (ui/gap 1 0)
                      [:stretch 1 (ui/valign 1 0.5 (label "Bottom to middle"))]
                      (ui/gap 1 0)
                      [:stretch 1 (ui/valign 0.5   (label "Middle to middle"))]
                      (ui/gap 1 0)
                      [:stretch 1 (ui/valign 0 0.5 (label "Top to middle"))]
                      (ui/gap 1 0)
                      [:stretch 1 (ui/valign 1 1   (label "Bottom to bottom"))]
                      (ui/gap 1 0)
                      [:stretch 1 (ui/valign 0.5 1 (label "Middle to bottom"))]
                      (ui/gap 1 0)
                      [:stretch 1 (ui/valign 0 1   (label "Top to bottom"))]
                      (ui/gap 1 0)
                      [:stretch 1 (label "Stretch")]))]
      [:stretch 1 nil])))

; (reset! user/*example "align")