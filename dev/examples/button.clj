(ns examples.button
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(defonce *clicks
  (signal/signal 0))

(defonce *selected
  (signal/signal nil))

(ui/defcomp external-state []
  (let [*state  (signal/signal :default)
        *clicks (signal/signal 0)]
    (fn []
      [ui/row {:gap 10}
       [ui/button {:*state *state
                   :on-click (fn [e] (reset! *clicks (:clicks e)))}
        "External state"]
       [ui/valign {:position 0.5}
        [ui/column {:gap 10}
         [ui/label "State: " *state]
         [ui/label "Last clicks: " *clicks]]]])))

(ui/defcomp nested-bubble []
  (let [*outer (signal/signal 0)
        *inner (signal/signal 0)]
    (fn []
      [ui/row {:gap 10}
       [ui/button {:name "outer" :on-click (fn [_] (swap! *outer inc))}
        [ui/button {:name "inner" :on-click (fn [_] (swap! *inner inc))}
         "Nested / bubble"]]
       [ui/valign {:position 0.5}
        [ui/column {:gap 10}
         [ui/label "Outer: " *outer]
         [ui/label "Inner: " *inner]]]])))

(ui/defcomp nested-capture []
  (let [*outer (signal/signal 0)
        *inner (signal/signal 0)]
    (fn []
      [ui/row {:gap 10}
       [ui/button {:on-click-capture (fn [_] (swap! *outer inc))}
        [ui/button {:on-click-capture (fn [_] (swap! *inner inc))}
         "Nested / capture"]]
       [ui/valign {:position 0.5}
        [ui/column {:gap 10}
         [ui/label "Outer: " *outer]
         [ui/label "Inner: " *inner]]]])))

(ui/defcomp toggle []
  (let [*value (signal/signal nil)]
    (fn []
      [ui/row {:gap 10}
       [ui/toggle-button {:*value *value} "Toggle"]
       [ui/valign {:position 0.5}
        [ui/label (if @*value "ON" "OFF")]]])))

(ui/defcomp radio []
  (let [*value (signal/signal :one)]
    (fn []
      [ui/row {:gap 10}
       [ui/toggle-button {:*value *value
                          :value-pressed :one}
        "One"]
       [ui/toggle-button {:*value *value
                          :value-pressed :two}
        "Two"]
       [ui/toggle-button {:*value *value
                          :value-pressed :three}
        "Three"]
       [ui/valign {:position 0.5}
        [ui/label "Radio " *value]]])))

(ui/defcomp custom-laf [state child]
  [ui/padding {:padding (if (= :pressed state) 1 0)}
   [ui/rounded-rect {:radius 15
                     :paint  (paint/fill 0xFF007BFF)}
    [ui/padding {:padding (if (= :pressed state) 9 10)}
     (if (vector? child)
       child
       [ui/label {:paint (paint/fill 0xFFFFFFFF)} child])]]])

(ui/defcomp custom []
  [ui/button {:laf custom-laf
              :on-click (fn [_] (swap! *clicks inc))}
   "Custom LAF"])

(ui/defcomp ui []
  [ui/center
   [ui/column {:gap (:leading ui/*ctx*)}
        
    [ui/halign {:position 0}
     [ui/label "Clicks: " *clicks]]
        
    [ui/halign {:position 0}
     [ui/button {:on-click (fn [_] (signal/swap! *clicks inc))}
      "Increment"]]
          
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
      [ui/label "Dynamic label: " *clicks]]]
        
    [ui/halign {:position 0}
     [external-state]]
        
    [ui/halign {:position 0}
     [nested-bubble]]
        
    [ui/halign {:position 0}
     [nested-capture]]
        
    [ui/halign {:position 0}
     [toggle]]
        
    [ui/halign {:position 0}
     [radio]]
    
    [ui/halign {:position 0}
     [custom]]
    ]])