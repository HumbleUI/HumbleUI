(ns examples.button
  (:require
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(defonce *clicks
  (signal/signal 0))

(defonce *selected
  (signal/signal nil))

(ui/defcomp ui []
  (let [*active-first?  (signal/signal
                          (= :first @*selected))
        *active-second? (signal/signal
                          (= :second @*selected))
        *active-third?  (signal/signal
                          (= :third @*selected))]
    (fn []
      [ui/center
       [ui/column {:gap (:leading ui/*ctx*)}
        [ui/halign {:position 0}
         [ui/button {:on-click (fn [_] (signal/swap! *clicks inc))}
          [ui/label "Increment"]]]        
          
        [ui/halign {:position 0}
         [ui/button {:on-click (fn [_] (signal/swap! *clicks inc))}
          [ui/row {:gap 5}
           [ui/width {:width 14}
            [ui/height {:height 14}
             [ui/image "dev/images/add.png"]]]
           [ui/valign {:position 0.5}
            [ui/label "With PNG icon"]]]]]

        [ui/halign {:position 0}
         [ui/button {:on-click (fn [_] (signal/swap! *clicks inc))}
          [ui/row {:gap 5}
           [ui/width {:width 14}
            [ui/height {:height 14}
             [ui/svg "dev/images/add.svg"]]]
           [ui/valign {:position 0.5}
            [ui/label "With SVG icon"]]]]]
                    
        [ui/halign {:position 0}
         [ui/button {:on-click (fn [_] (signal/swap! *clicks inc))}
          [ui/label "Dynamic label: " @*clicks]]]
                    
        [ui/halign {:position 0}
         [ui/button {:on-click (fn [_] (signal/reset! *clicks 0))}
          [ui/label "Reset"]]]
          
        [ui/row {:gap 10}
         [ui/button {:on-click (fn [_] (signal/reset! *selected :first))
                     :*active? *active-first?}
          [ui/label "First"]]
         [ui/button {:on-click (fn [_] (signal/reset! *selected :second))
                     :*active? *active-second?}
          [ui/label "Second"]]
         [ui/button {:on-click (fn [_] (signal/reset! *selected :third))
                     :*active? *active-third?}
          [ui/label "Third"]]]
        ]])))