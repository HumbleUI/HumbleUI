(ns examples.vscroll
  (:require
    [clojure.string :as str]
    [examples.shared :as shared]
    [io.github.humbleui.util :as util]
        [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp box
  ([child]
   (box {} child))
  ([opts child]
   (let [{:keys [height] :or {height 100}} opts]
     [ui/rect {:paint {:stroke 0x40000000}}
      [ui/size {:height height}
       child]])))

(ui/defcomp label [& texts]
  (let [[_ opts texts] (ui/parse-element (util/consv nil texts))
        {:keys [height] :or {height 30}} opts]
    (ui/with-resources [bg       {:fill 0x00CFE8FC}
                        bg-hover {:fill 0xFFCFE8FC}]
      (fn [& texts]
        [ui/hoverable
         (fn [state]
           [ui/rect {:paint (if (:hovered state) bg-hover bg)}
            [ui/padding
             {:horizontal 20
              :vertical   (-> height (- 10) (/ 2))}
             [ui/align {:x :center}
              [ui/label {:font-features ["tnum"]} (str/join texts)]]]])]))))

(ui/defcomp ui []
  (shared/table
    "Lots of items (min-thumb-h check)"
    [box
     [ui/vscroll
      [ui/column
       (for [i (range 0 100)]
         [label i])]]]
    
    "Medium items"
    [box
     [ui/vscroll
      [ui/column
       (for [i (range 0 5)]
         [label i])]]]
    
    "Not enough items"
    [box
     [ui/align {:y :center}
      [ui/vscroll
       [ui/column
        (for [i (range 0 2)]
          [label i])]]]]
    
    "Without scrollbar"
    [box
     [ui/vscrollable
      [ui/column
       (for [i (range 0 100)]
         [label i])]]]
    
    "Offset state"
    (let [offset (signal/signal 0)]
      [ui/column {:gap 10}
       [box {:height 200}
        [ui/vscroll {:offset offset}
         [ui/column
          (for [i (range 0 100)]
            [label i])]]]
       [ui/size {:width 100}
        [ui/align {:x :center}
         [ui/label {:font-features ["tnum"]}
          offset " dip"]]]])
    
    "Synced scrolls"
    (let [offset (signal/signal 0)]
      [box {:height 200}
       [ui/row
        [ui/vscroll {:offset offset}
         [ui/column
          (for [i (range 0 100)]
            [label i])]]
        [ui/vscroll {:offset offset}
         [ui/column
          (for [i (range 0 100)]
            [label i])]]]])

    "Nested scrolls"
    [box {:height 200}
     [ui/vscroll
      [ui/column
       (for [i (range 0 5)]
         [label "Outer " i])
       [box {:height 100}
        [ui/vscroll
         [ui/column
          (for [i (range 5 15)]
            [label "Inner " i])]]]
       (for [i (range 15 20)]
         [label "Outer" i])]]]))
