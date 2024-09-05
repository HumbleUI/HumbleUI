(ns examples.label
  (:require
    [examples.shared :as shared]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.font :as font]
        [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(defn use-timer []
  (let [*state (ui/signal 0)
        cancel (util/schedule
                 #(swap! *state inc) 0 1000)]
    {:value *state
     :after-unmount cancel}))

(ui/defcomp ui []
  (ui/with [*timer (use-timer)]
    (fn []
      (shared/table
        "With a string"
        [ui/label "String"]
        
        "Multiple strings are joined"
        [ui/label "One" "Two" "Three"]
        
        "Non-strings are coerced to strings"
        [ui/label 1 :kw #"re"]
          
        "Plain strings are automatically converted to labels"
        "Plain string"
        
        "Custom paint"
        [ui/label {:paint {:fill 0xFFCC3333}}
         "Red"]
        
        "Custom font properties"
        [ui/column {:gap 10}
         [ui/label {:font-family "Fira Code"} "Monospace"]
         [ui/label {:font-weight :bold} "Bold"]
         [ui/label {:font-slant :italic} "Italic"]
         [ui/label {:font-cap-height 14} "Cap-height * 1.5"]
         [ui/label {:font-size 20} "Size"]
         [ui/label {:font-features ["tnum" "zero"]}
          "1234567890"]]
        
        "Set properties through context"
        [ui/with-context
         {:font-family "Fira Code"
          :font-weight :bold
          :font-cap-height 14
          :font-features ["cv09" "zero"]}
         [ui/label "Hell0"]]
        
        "Font stack"
        [ui/column {:gap 10}
         [ui/label {:font-family "Cascadia Code, SF Pro Text, Inter"}
          "12345 Hamburgevons"]
         [ui/label {:font-family "SF Pro Text, Inter"}
          "12345 Hamburgevons"]
         [ui/label {:font-family "Inter"}
          "12345 Hamburgevons"]]
        
        "Alias font families"
        [ui/with-font-family-aliases
         {"default"    "Inter"
          "sans-serif" "SF Pro Text, Segoe UI, default"}
         [ui/label {:font-family "sans-serif"}
          "12345 Hamburgevons"]]
        
        "Pass signals directly for fine-grained reactivity"
        [ui/label "Time: " *timer]))))
