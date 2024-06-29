(ns examples.scroll
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp label
  ([text]
   (label 10 text))
  ([height text]
   (let [*state (signal/signal false)]
     (fn render
       ([text]
        (render 10 text))
       ([height text]
        (let [label [ui/padding
                     {:horizontal 20
                      :vertical height}
                     [ui/label text]]]
          [ui/hoverable {:*state *state}
           (if (= :hovered @*state)
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
    
    ;; 100 elements, no scrollbar
    [ui/vscroll
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
        [label (+ 10 (* i 2)) i])]]
    
    ;; nested
    [ui/vscrollbar
     [ui/column
      (for [i (range 1 10)]
        [ui/padding {:horizontal 20 :vertical 10}
         [ui/label (str "Item " i)]])
              
      [ui/height {:height 130}
       [ui/padding {:bottom 12}
        [ui/rect {:paint (paint/stroke 0xFF000000 1)}
         [ui/vscrollbar
          [ui/column
           (for [ch (map str "ABCDEFGHIJKLMN")]
             [ui/padding {:horizontal 20 :vertical 10}
              [ui/label (str "Nested " ch)]])]]]]]

      (for [i (range 10 20)]
        [ui/padding {:horizontal 20 :vertical 10}
         [ui/label (str "Item " i)]])]]]])
