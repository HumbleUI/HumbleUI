(ns examples.button
  (:require
    [io.github.humbleui.util :as util]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(defonce *clicks
  (ui/signal 0))

(defonce *selected
  (ui/signal nil))

(ui/defcomp external-state []
  (let [*state  (ui/signal #{})
        *clicks (ui/signal 0)]
    (fn []
      [ui/row {:gap 10}
       [ui/align {:y :top}
        [ui/button {:*state *state
                    :on-click (fn [e] (reset! *clicks (:clicks e)))}
         "External state"]]
       [ui/align {:y :center}
        [ui/column {:gap 10}
         [ui/label "State: " *state]
         [ui/label "Last clicks: " *clicks]]]])))

(ui/defcomp inline-state []
  [ui/clickable {:on-click (fn [e] (reset! *clicks (:clicks e)))}
   (fn [state]
     [ui/button-look state
      [ui/label "Inline state: " state]])])

(ui/defcomp nested-bubble []
  (let [*outer (ui/signal 0)
        *inner (ui/signal 0)]
    (fn []
      [ui/row {:gap 10}
       [ui/button {:name "outer" :on-click (fn [_] (swap! *outer inc))}
        [ui/button {:name "inner" :on-click (fn [_] (swap! *inner inc))}
         "Nested / bubble"]]
       [ui/align {:y :center}
        [ui/column {:gap 10}
         [ui/label "Outer: " *outer]
         [ui/label "Inner: " *inner]]]])))

(ui/defcomp nested-capture-prevent []
  (let [*outer (ui/signal 0)
        *inner (ui/signal 0)]
    (fn []
      [ui/row {:gap 10}
       [ui/button {:on-click-capture (fn [_] (swap! *outer inc))}
        [ui/button {:on-click-capture (fn [_] (swap! *inner inc))}
         "Nested / capture / prevent"]]
       [ui/align {:y :center}
        [ui/column {:gap 10}
         [ui/label "Outer: " *outer]
         [ui/label "Inner: " *inner]]]])))

(ui/defcomp nested-capture []
  (let [*outer (ui/signal 0)
        *inner (ui/signal 0)]
    (fn []
      [ui/row {:gap 10}
       [ui/button {:on-click-capture (fn [_] (swap! *outer inc) false)}
        [ui/button {:on-click-capture (fn [_] (swap! *inner inc) false)}
         "Nested / capture"]]
       [ui/align {:y :center}
        [ui/column {:gap 10}
         [ui/label "Outer: " *outer]
         [ui/label "Inner: " *inner]]]])))

(ui/defcomp toggle []
  (let [*value (ui/signal nil)]
    (fn []
      [ui/row {:gap 10}
       [ui/toggle-button {:*value *value} "Toggle"]
       [ui/align {:y :center}
        [ui/label (if @*value "ON" "OFF")]]])))

(ui/defcomp radio []
  (let [*value (ui/signal :one)]
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
  {:fill 0xFF007BFF})

(def custom-button-text
  {:fill 0xFFFFFFFF})

(ui/defcomp custom-look [state child]
  [ui/translate {:dy (if (:pressed state) 2 0)}
   [ui/rect {:radius 15
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
       [ui/rect {:radius 15
                 :paint  custom-button-bg}
        [ui/padding {:padding 10}
         [ui/label {:paint custom-button-text} "Inline custom look"]]]])]])

(ui/defcomp with-shadow []
  [ui/row {:gap 10}
   [ui/align {:x :left}
    [ui/shadow {:blur @*clicks}
     [ui/button {:on-click (fn [_] (swap! *clicks inc))} "Outset shadow"]]]
   [ui/align {:x :left}
    [ui/shadow-inset {:blur @*clicks}
     [ui/button {:on-click (fn [_] (swap! *clicks inc))} "Inset shadow"]]]])

(ui/defcomp ui []
  [ui/align {:y :center}
   [ui/vscroll
    [ui/align {:x :center}
     [ui/padding {:padding 20}
      [ui/with-context {:font-features ["tnum"]}
       [ui/column {:gap 10}
        
        [ui/align {:x :left}
         [ui/label "Clicks: " *clicks]]
        
        [ui/align {:x :left}
         [ui/row {:gap 10}
          [ui/button {:on-click (fn [_] (swap! *clicks inc))}
           "Increment"]
          [ui/button {:on-click (fn [_] (reset! *clicks 0))}
           "Reset"]]]
       
        [ui/align {:x :left}
         [ui/with-context {:font-cap-height 15}
          [ui/button {:on-click (fn [_] (swap! *clicks + 2))}
           "Big increment"]]]
    
        [ui/align {:x :left}
         [with-shadow]]
          
        [ui/align {:x :left}
         [ui/button {:on-click (fn [_] (swap! *clicks inc))}
          [ui/row {:gap 5}
           [ui/size {:width 14, :height 14}
            [ui/image {:src "dev/images/add.png"}]]
           [ui/align {:y :center}
            [ui/label "With PNG icon"]]]]]

        [ui/align {:x :left}
         [ui/button {:on-click (fn [_] (swap! *clicks inc))}
          [ui/row {:gap 5}
           [ui/size {:width 14, :height 14}
            [ui/svg {:src "dev/images/add.svg"}]]
           [ui/align {:y :center}
            [ui/label "With SVG icon"]]]]]
                    
        [ui/align {:x :left}
         [ui/button {:on-click (fn [_] (swap! *clicks inc))}
          [ui/label "Dynamic label: " *clicks]]]
        
        [ui/align {:x :left}
         [external-state]]
        
        [ui/align {:x :left}
         [inline-state]]
        
        [ui/align {:x :left}
         [nested-bubble]]
       
        [ui/align {:x :left}
         [nested-capture]]
       
        [ui/align {:x :left}
         [nested-capture-prevent]]
        
        [ui/align {:x :left}
         [toggle]]
        
        [ui/align {:x :left}
         [radio]]
    
        [ui/align {:x :left}
         [custom]]]]]]]])