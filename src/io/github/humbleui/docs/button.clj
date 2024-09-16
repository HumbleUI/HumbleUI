(ns io.github.humbleui.docs.button
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
      
        "Custom look"
        [ui/clickable
         (fn [state]
           [ui/translate {:dy (if (:pressed state) 2 0)}
            [ui/rect {:radius 15
                      :paint  {:fill "007BFF"}}
             [ui/padding {:padding 10}
              [ui/label {:paint {:fill "FFF"}} "Custom look"]]]])]))))
