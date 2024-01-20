(ns examples.scroll
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp label
  ([text]
   (label 10 text))
  ([height text]
   (let [*hovered? (signal/signal false)]
     (fn render
       ([text]
        (render 10 text))
       ([height text]
        (let [label [ui/padding
                     {:horizontal 20
                      :vertical height}
                     [ui/label text]]]
          [ui/hoverable {:*hovered? *hovered?}
           (if @*hovered?
             [ui/rect {:paint (paint/fill 0xFFCFE8FC)} label]
             label)]))))))

(ui/defcomp ui []
  [ui/halign {:position 0.5}
   [ui/row {:gap 10}
    ;; 100 elements
    [ui/vscrollbar
     [ui/column
      (for [i (range 0 100)]
        [label i])]]
        
    ;; 50 elements
    [ui/padding {:vertical 50}
     [ui/vscrollbar
      [ui/column
       (for [i (range 0 50)]
         [label i])]]]
        
    ;; 10 elements
    [ui/vscrollbar
     [ui/column
      (for [i (range 0 10)]
        [label i])]]
        
    ;; variable height
    [ui/vscrollbar
     [ui/column
      (for [i (range 0 10)]
        [label (+ 10 (* i 2)) i])]]]])
