(ns examples.scroll
  (:require
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
                               (ui/label (str %) font-ui fill-text))]
                   (ui/hoverable
                     (ui/dynamic ctx [hovered? (:hui/hovered? ctx)]
                       (if hovered?
                         (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFFCFE8FC))) label)
                         label))))
                (range 0 100)))))
        
        (ui/gap 10 0)
        
        ;; 50 elements
        (ui/vscrollbar
          (ui/vscroll
            (apply ui/column
              (mapv
                #(let [label (ui/padding 20 leading
                               (ui/label (str %) font-ui fill-text))]
                   (ui/hoverable
                     (ui/dynamic ctx [hovered? (:hui/hovered? ctx)]
                       (if hovered?
                         (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFFCFE8FC))) label)
                         label))))
                (range 0 50)))))
        
        (ui/gap 10 0)
        
        ;; 10 elements
        (ui/vscrollbar
          (ui/vscroll
            (apply ui/column
              (mapv
                #(let [label (ui/padding 20 leading
                               (ui/label (str %) font-ui fill-text))]
                   (ui/hoverable
                     (ui/dynamic ctx [hovered? (:hui/hovered? ctx)]
                       (if hovered?
                         (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFFCFE8FC))) label)
                         label))))
                (range 0 10)))))
        
        (ui/gap 10 0)
        
        ;; variable height
        (ui/vscrollbar
          (ui/vscroll
            (apply ui/column
              (mapv
                #(let [label (ui/padding 20 (+ leading (* % 2))
                               (ui/label (str %) font-ui fill-text))]
                   (ui/hoverable
                     (ui/dynamic ctx [hovered? (:hui/hovered? ctx)]
                       (if hovered?
                         (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFFCFE8FC))) label)
                         label))))
                (range 0 10)))))
        
        ))))

; (require 'user :reload)
; (reset! user/*example "Scroll")