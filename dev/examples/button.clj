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
       [ui/align {:y :center}
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
       [ui/align {:y :center}
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
       [ui/align {:y :center}
        [ui/column {:gap 10}
         [ui/label "Outer: " *outer]
         [ui/label "Inner: " *inner]]]])))

(ui/defcomp toggle []
  (let [*value (signal/signal nil)]
    (fn []
      [ui/row {:gap 10}
       [ui/toggle-button {:*value *value} "Toggle"]
       [ui/align {:y :center}
        [ui/label (if @*value "ON" "OFF")]]])))

(ui/defcomp radio []
  (let [*value (signal/signal :one)]
    (fn []
      [ui/row {:gap 10}
       [ui/toggle-button {:*value *value
                          :value-on :one}
        "One"]
       [ui/toggle-button {:*value *value
                          :value-on :two}
        "Two"]
       [ui/toggle-button {:*value *value
                          :value-on :three}
        "Three"]
       [ui/align {:y :center}
        [ui/label "Radio " *value]]])))

(def custom-button-bg
  (paint/fill 0xFF007BFF))

(def custom-button-text
  (paint/fill 0xFFFFFFFF))

(ui/defcomp custom-look [state child]
  [ui/translate {:dy (if (:pressed state) 2 0)}
   [ui/rounded-rect {:radius 15
                     :paint  custom-button-bg}
    [ui/padding {:padding 10}
     (if (vector? child)
       child
       [ui/label {:paint custom-button-text} child])]]])

(ui/defcomp custom []
  [ui/row {:gap 5}
   [ui/clickable {:on-click (fn [_] (swap! *clicks inc))}
    (fn [state]
      [custom-look state "Reusable custom look"])]
   [ui/clickable {:on-click (fn [_] (swap! *clicks inc))}
    (fn [state]
      [ui/translate {:dy (if (:pressed state) 2 0)}
       [ui/rounded-rect {:radius 15
                         :paint  custom-button-bg}
        [ui/padding {:padding 10}
         [ui/label {:paint custom-button-text} "Inline custom look"]]]])]])

(ui/defcomp with-shadow []
  [ui/row {:gap 10}
   [ui/align {:x :left}
    [ui/shadow {:blur @*clicks}
     [ui/button {:on-click (fn [_] (signal/swap! *clicks inc))} "Outset shadow"]]]
   [ui/align {:x :left}
    [ui/shadow-inset {:blur @*clicks}
     [ui/button {:on-click (fn [_] (signal/swap! *clicks inc))} "Inset shadow"]]]])

(ui/defcomp ui []
  [ui/center
   [ui/column {:gap (:leading ui/*ctx*)}
        
    [ui/align {:x :left}
     [ui/label "Clicks: " *clicks]]
        
    [ui/align {:x :left}
     [ui/row {:gap 10}
      [ui/button {:on-click (fn [_] (swap! *clicks inc))}
       "Increment"]
      [ui/button {:on-click (fn [_] (reset! *clicks 0))}
       "Reset"]]]
    
    [ui/align {:x :left}
     [with-shadow]]
          
    [ui/align {:x :left}
     [ui/button {:on-click (fn [_] (signal/swap! *clicks inc))}
      [ui/row {:gap 5}
       [ui/size {:width 14, :height 14}
        [ui/image "dev/images/add.png"]]
       [ui/align {:y :center}
        [ui/label "With PNG icon"]]]]]

    [ui/align {:x :left}
     [ui/button {:on-click (fn [_] (signal/swap! *clicks inc))}
      [ui/row {:gap 5}
       [ui/size {:width 14, :height 14}
        [ui/svg "dev/images/add.svg"]]
       [ui/align {:y :center}
        [ui/label "With SVG icon"]]]]]
                    
    [ui/align {:x :left}
     [ui/button {:on-click (fn [_] (signal/swap! *clicks inc))}
      [ui/label "Dynamic label: " *clicks]]]
        
    [ui/align {:x :left}
     [external-state]]
        
    [ui/align {:x :left}
     [nested-bubble]]
        
    [ui/align {:x :left}
     [nested-capture]]
        
    [ui/align {:x :left}
     [toggle]]
        
    [ui/align {:x :left}
     [radio]]
    
    [ui/align {:x :left}
     [custom]]]])