(ns io.github.humbleui.docs.clickable
  (:require
    [io.github.humbleui.docs.shared :as shared]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  (let [*counter (ui/signal 0)
        *state   (ui/signal #{})
        *outer   (ui/signal 0)
        *inner   (ui/signal 0)
        *toggle  (ui/signal nil)
        cl       (-> (Thread/currentThread) .getContextClassLoader)]
    (fn []
      (shared/table
        "Simple on-click"
        [ui/clickable
         {:on-click
          (fn [e]
            (swap! *counter inc))}
         [ui/rect {:paint "FBE99C"}
          [ui/padding {:padding 10}
           [ui/label
            "Increment: " *counter]]]]
            
        "State signal"
        [ui/clickable
         {:*state *state}
         [ui/rect {:paint "FBE99C"}
          [ui/padding {:padding 10}
           [ui/label *state]]]]
      
        "Inline signal"
        [ui/clickable
         (fn [state]
           [ui/rect {:paint "FBE99C"}
            [ui/padding {:padding 10}
             [ui/label state]]])]
      
        "Making a button"
        [ui/clickable
         (fn [state]
           [ui/rect {:radius 4
                     :paint  {:fill (cond
                                      (:pressed state) "808080"
                                      (:hovered state) "B0B0B0"
                                      :else            "E0E0E0")}}
            [ui/padding {:padding 10}
             [ui/label "Custom look"]]])]
        
        "Nesting with :on-click"
        [ui/clickable {:on-click (fn [_] (swap! *outer inc))}
         [ui/rect {:paint "80808030"}
          [ui/padding {:padding 10}
           [ui/column {:gap 10 :align :center}
            [ui/label "Outer: " *outer]
            [ui/clickable  {:on-click (fn [_] (swap! *inner inc))}
             [ui/rect {:paint "80808030"}
              [ui/padding {:padding 10}
               [ui/label "Inner: " *inner]]]]]]]]
       
        "Nesting with :on-click-capture and short-circuiting"
        [ui/clickable
         {:on-click-capture
          (fn [_]
            (swap! *outer inc)
            true)}
         [ui/rect {:paint "80808030"}
          [ui/padding {:padding 10}
           [ui/column {:gap 10 :align :center}
            [ui/label "Outer: " *outer]
            [ui/clickable
             {:on-click-capture
              (fn [_]
                (swap! *inner inc)
                true)}
             [ui/rect {:paint "80808030"}
              [ui/padding {:padding 10}
               [ui/label "Inner: " *inner]]]]]]]]
       
        "Nesting with :on-click-capture and pass-through"
        [ui/clickable
         {:on-click-capture
          (fn [_]
            (swap! *outer inc)
            false)}
         [ui/rect {:paint "80808030"}
          [ui/padding {:padding 10}
           [ui/column {:gap 10 :align :center}
            [ui/label "Outer: " *outer]
            [ui/clickable
             {:on-click-capture
              (fn [_] 
                (swap! *inner inc)
                false)}
             [ui/rect {:paint "80808030"}
              [ui/padding {:padding 10}
               [ui/label "Inner: " *inner]]]]]]]]))))
