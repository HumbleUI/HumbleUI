(ns examples.scroll
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(def ui
  (ui/dynamic ctx [{:keys [font-ui leading fill-text]} ctx]
    (ui/halign 0.5
      (ui/row
        ;; 100 elements
        (ui/vscrollbar
          (ui/vscroll
            (apply ui/column
              (mapv
                #(let [label (ui/padding 20 leading
                               (ui/label %))]
                   (ui/hoverable
                     (ui/dynamic ctx [hovered? (:hui/hovered? ctx)]
                       (if hovered?
                         (ui/rect (paint/fill 0xFFCFE8FC) label)
                         label))))
                (range 0 100)))))
        
        (ui/gap 10 0)
        
        ;; 50 elements
        (ui/padding 0 50
          (ui/vscrollbar
            (ui/vscroll
              (apply ui/column
                (mapv
                  #(let [label (ui/padding 20 leading
                                 (ui/label %))]
                     (ui/hoverable
                       (ui/dynamic ctx [hovered? (:hui/hovered? ctx)]
                         (if hovered?
                           (ui/rect (paint/fill 0xFFCFE8FC) label)
                           label))))
                  (range 0 50))))))
        
        (ui/gap 10 0)
        
        ;; 10 elements
        (ui/vscrollbar
          (ui/vscroll
            (apply ui/column
              (mapv
                #(let [label (ui/padding 20 leading
                               (ui/label %))]
                   (ui/hoverable
                     (ui/dynamic ctx [hovered? (:hui/hovered? ctx)]
                       (if hovered?
                         (ui/rect (paint/fill 0xFFCFE8FC) label)
                         label))))
                (range 0 10)))))
        
        (ui/gap 10 0)
        
        ;; variable height
        (ui/vscrollbar
          (ui/vscroll
            (apply ui/column
              (mapv
                #(let [label (ui/padding 20 (+ leading (* % 2))
                               (ui/label %))]
                   (ui/hoverable
                     (ui/dynamic ctx [hovered? (:hui/hovered? ctx)]
                       (if hovered?
                         (ui/rect (paint/fill 0xFFCFE8FC) label)
                         label))))
                (range 0 10)))))
        
        ))))

; (reset! user/*example "scroll")