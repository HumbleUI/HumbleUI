(ns io.github.humbleui.docs.button
  (:require
    [clojure.string :as str]
    [io.github.humbleui.docs.shared :as shared]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.signal :as signal]
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
        "Default button"
        [ui/button
         {:on-click
          (fn [e]
            (swap! *counter inc))}
         [ui/label
          "Increment: " *counter]]
      
        "Styles"
        [ui/column {:gap 10}
         (for [style [:basic :default :flat :outlined]]
           [ui/button
            {:style style}
            (str style)])]
      
        "With icon"
        [ui/column {:gap 10}
         (for [icon [[ui/image {:src (.getResource cl "io/github/humbleui/docs/button/add.png")}]
                     [ui/svg   {:src (.getResource cl "io/github/humbleui/docs/button/add.svg")}]]]
           [ui/button
            [ui/row {:gap 4 :align :center}
             [ui/size {:width 14 :height 14}
              icon]
             "Increment"]])]
      
        "State signal"
        [ui/button
         {:*state *state}
         [ui/label *state]]
      
        "Inline signal"
        [ui/clickable
         (fn [state]
           [((resolve 'io.github.humbleui.ui/button-look-ctor) :basic) state
            [ui/label state]])]
      
        "Nesting with :on-click"
        [ui/button {:on-click (fn [_] (swap! *outer inc))}
         [ui/column {:gap 10 :align :center}
          [ui/label "Outer: " *outer]
          [ui/button  {:on-click (fn [_] (swap! *inner inc))}
           [ui/label "Inner: " *inner]]]]
       
        "Nesting with :on-click-capture and short-circuiting"
        [ui/button
         {:on-click-capture
          (fn [_]
            (swap! *outer inc)
            true)}
         [ui/column {:gap 10 :align :center}
          [ui/label "Outer: " *outer]
          [ui/button
           {:on-click-capture
            (fn [_]
              (swap! *inner inc)
              true)}
           [ui/label "Inner: " *inner]]]]
       
        "Nesting with :on-click-capture and pass-through"
        [ui/button
         {:on-click-capture
          (fn [_]
            (swap! *outer inc)
            false)}
         [ui/column {:gap 10 :align :center}
          [ui/label "Outer: " *outer]
          [ui/button
           {:on-click-capture
            (fn [_] 
              (swap! *inner inc)
              false)}
           [ui/label "Inner: " *inner]]]]
      
        "Custom look"
        [ui/clickable
         (fn [state]
           [ui/translate {:dy (if (:pressed state) 2 0)}
            [ui/rect {:radius 15
                      :paint  {:fill "007BFF"}}
             [ui/padding {:padding 10}
              [ui/label {:paint {:fill "FFF"}} "Custom look"]]]])]))))
