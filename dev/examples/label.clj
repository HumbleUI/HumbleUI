(ns examples.label
  (:require
    [examples.util :as util]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(defn use-timer []
  (let [*state (signal/signal 0)
        cancel (core/schedule
                 #(swap! *state inc) 0 1000)]
    {:value *state
     :after-unmount cancel}))

(ui/defcomp ui []
  (ui/with [*timer (use-timer)]
    (let [font (font/make-with-cap-height @util/*face-bold (ui/scaled 10))]
      (fn []
        (util/table
          "With a string"
          [ui/label "String"]
        
          "Multiple strings are joined"
          [ui/label "One" "Two" "Three"]
        
          "Non-strings are coerced to strings"
          [ui/label 1 :kw #"re"]
          
          "Plain strings are automatically converted to labels"
          "Plain string"
        
          "Custom paint"
          [ui/label {:paint (paint/fill 0xFFCC3333)}
           "Red"]
        
          "Custom font"
          [ui/label {:font font}
           "Bold"]
          
          "Custom font features"
          [ui/column {:gap 10}
           [ui/label "1234567890"]
           [ui/label {:features ["tnum" "zero"]}
            "1234567890"]]
        
          "Pass signals directly for fine-grained reactivity"
          [ui/label "Time: " *timer])))))
