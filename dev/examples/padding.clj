(ns examples.padding
  (:require
    [clojure.string :as str]
    [examples.shared :as shared]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  (let [fill (paint/fill 0x80FFDB2C)]
    (fn []
      (shared/table
        "Without padding"
        [ui/rect {:paint fill}
         [ui/label "P"]]
        
        "Even padding"
        [ui/rect {:paint fill}
         [ui/padding {:padding 20}
          [ui/label "P"]]]
        
        "Separate horizontal/vertical padding"
        [ui/rect {:paint fill}
         [ui/padding {:horizontal 20
                      :vertical 10}
          [ui/label "P"]]]
        
        "Separate four-way padding"
        [ui/rect {:paint fill}
         [ui/padding {:left   10
                      :top    20
                      :right  30
                      :bottom 40}
          [ui/label "P"]]]
        
        "Function accepts container’s size"
        [ui/rect {:paint fill}
         [ui/padding
          {:padding (fn [cs]
                      (* 0.2 (:width cs)))}
          [ui/rect {:paint fill}
           [ui/label "P"]]]]))))