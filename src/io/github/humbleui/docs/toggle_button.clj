(ns io.github.humbleui.docs.toggle-button
  (:require
    [io.github.humbleui.docs.shared :as shared]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  (let [*bool   (ui/signal nil)
        *custom (ui/signal :on)
        *radio  (ui/signal :one)]
    (fn []
      (shared/table
        "Signal state"
        [ui/toggle-button {:*value *bool}
         [ui/label (pr-str @*bool)]]

        "On change"
        [ui/toggle-button
         {:on-change
          (fn [_]
            (swap! *bool #(if-not % true)))}
         [ui/label (pr-str @*bool)]]
        
        "On/off values"
        [ui/toggle-button {:*value    *custom
                           :value-on  :on
                           :value-off :off}
         [ui/label (pr-str @*custom)]]
        
        "Styles"
        [ui/column {:gap 10}
         (for [style [:basic :default :flat :outlined]]
           [ui/toggle-button
            {:style style}
            (str style)])]
        
        "State"
        [ui/toggle-button
         (fn [state]
           [ui/label (pr-str state)])]
        
        "Radio"        
        [ui/column {:gap 10}
         [ui/toggle-button {:*value *radio
                            :value-on :one}
          ":one"]
         [ui/toggle-button {:*value *radio
                            :value-on :two}
          ":two"]
         [ui/toggle-button {:*value *radio
                            :value-on :three}
          ":three"]
         [ui/align {:x :center}
          (pr-str @*radio)]]))))
