(ns examples.svg
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp svg [width height xpos ypos scale]
  [ui/clip
   [ui/rect {:paint (paint/fill 0xFF90DC48)}
    [ui/size {:width width, :height height}
     [ui/svg {:xpos xpos :ypos ypos :scale scale} "dev/images/ratio.svg"]]]])

(ui/defcomp ui []
  (ui/with [*size (ui/use-size)]
    (let [*step (signal/signal
                  (min
                    (quot (:width @*size) 47)
                    (quot (:height @*size) 15)))]
      (fn []
        (let [step @*step]
          [ui/center
           [ui/column {:gap step}
            [ui/row {:gap step}
             [ui/column {:gap step}
              [svg (* 7 step) (* 3 step) 0.5 0.5 :fit]
              [svg (* 7 step) (* 3 step) 0.5 0   :fill]]
             [ui/column {:gap step}
              [svg (* 7 step) (* 3 step) 0   0.5 :fit]
              [svg (* 7 step) (* 3 step) 0.5 0.5 :fill]]
             [ui/column {:gap step}
              [svg (* 7 step) (* 3 step) 1   0.5 :fit]
              [svg (* 7 step) (* 3 step) 0.5 1 :fill]]
             [svg (* 3 step) (* 7 step) 0.5 0   :fit]
             [svg (* 3 step) (* 7 step) 0.5 0.5 :fit]
             [svg (* 3 step) (* 7 step) 0.5 1   :fit]
             [svg (* 3 step) (* 7 step) 0   0.5 :fill]
             [svg (* 3 step) (* 7 step) 0.5 0.5 :fill]
             [svg (* 3 step) (* 7 step) 1   0.5 :fill]]
          
            [ui/clip
             [ui/rect {:paint (paint/fill 0xFF90DC48)}
              [ui/size {:width (* 47 step), :height (* 7 step)}
               [ui/svg {:preserve-aspect-ratio false} "dev/images/ratio.svg"]]]]]])))))
