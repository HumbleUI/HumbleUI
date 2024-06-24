(ns examples.settings
  (:require
    [io.github.humbleui.debug :as debug]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]))

; (defn set-floating! [window floating]
;   (when window
;     (app/doui
;       (if floating
;         (window/set-z-order window :floating)
;         (window/set-z-order window :normal)))))

; (add-watch common/*floating ::window
;   (fn [_ _ _ floating]
;     (set-floating! @common/*window floating)))

(def ui
  (ui/with-scale scale
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
                  ; (ui/row
                  ;   (ui/valign 0.5
                  ;     (ui/label "On top"))
                  ;   [:stretch 1 nil]
                  ;   (ui/toggle common/*floating))
                  ; (ui/gap 0 padding-inner)
                  ; (ui/rect fill-delimiter
                  ;   (ui/gap 0 1))
                  ; (ui/gap 0 padding-inner)
                  (ui/row
                    (ui/valign 0.5
                      (ui/label "Debug"))
                    [:stretch 1 nil]
                    (ui/toggle debug/*debug?)))))))))))
