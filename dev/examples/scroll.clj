(ns examples.scroll
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]))

(def ui
  (ui/dynamic ctx [{:keys [leading]} ctx]
    (ui/halign 0.5
      (ui/row
        ;; 100 elements
        (ui/vscrollbar
          (ui/column
            (for [i (range 0 100)
                  :let [label (ui/padding 20 leading
                                (ui/label i))]]
              (ui/hoverable
                (ui/dynamic ctx [hovered? (:hui/hovered? ctx)]
                  (if hovered?
                    (ui/rect (paint/fill 0xFFCFE8FC) label)
                    label))))))
        
        (ui/gap 10 0)
        
        ;; 50 elements
        (ui/padding 0 50
          (ui/vscrollbar
            (ui/column
              (for [i (range 0 50)
                    :let [label (ui/padding 20 leading
                                  (ui/label i))]]
                (ui/hoverable
                  (ui/dynamic ctx [hovered? (:hui/hovered? ctx)]
                    (if hovered?
                      (ui/rect (paint/fill 0xFFCFE8FC) label)
                      label)))))))
        
        (ui/gap 10 0)
        
        ;; 10 elements
        (ui/vscrollbar
          (ui/column
            (for [i (range 0 10)
                  :let [label (ui/padding 20 leading
                                (ui/label i))]]
              (ui/hoverable
                (ui/dynamic ctx [hovered? (:hui/hovered? ctx)]
                  (if hovered?
                    (ui/rect (paint/fill 0xFFCFE8FC) label)
                    label))))))
        
        (ui/gap 10 0)
        
        ;; variable height
        (ui/vscrollbar
          (ui/column
            (for [i (range 0 10)
                  :let [label (ui/padding 20 (+ leading (* i 2))
                                (ui/label i))]]
              (ui/hoverable
                (ui/dynamic ctx [hovered? (:hui/hovered? ctx)]
                  (if hovered?
                    (ui/rect (paint/fill 0xFFCFE8FC) label)
                    label))))))))))
