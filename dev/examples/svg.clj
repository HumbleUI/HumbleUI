(ns examples.svg
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp svg [width height xpos ypos scale]
  [ui/clip
   [ui/rect {:paint (paint/fill 0xFF90DC48)}
    [ui/width {:width width}
     [ui/height {:height height}
      [ui/svg {:xpos xpos :ypos ypos :scale scale} "dev/images/ratio.svg"]]]]])

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
              [svg (* 7 step) (* 3 step) :mid :mid :meet]
              [svg (* 7 step) (* 3 step) :mid :min :slice]]
             [ui/column {:gap step}
              [svg (* 7 step) (* 3 step) :min :mid :meet]
              [svg (* 7 step) (* 3 step) :mid :mid :slice]]
             [ui/column {:gap step}
              [svg (* 7 step) (* 3 step) :max :mid :meet]
              [svg (* 7 step) (* 3 step) :mid :max :slice]]
             [svg (* 3 step) (* 7 step) :mid :min :meet]
             [svg (* 3 step) (* 7 step) :mid :mid :meet]
             [svg (* 3 step) (* 7 step) :mid :max :meet]
             [svg (* 3 step) (* 7 step) :min :mid :slice]
             [svg (* 3 step) (* 7 step) :mid :mid :slice]
             [svg (* 3 step) (* 7 step) :max :mid :slice]]
          
            [ui/clip
             [ui/rect {:paint (paint/fill 0xFF90DC48)}
              [ui/width {:width (* 47 step)}
               [ui/height {:height (* 7 step)}
                [ui/svg {:preserve-aspect-ratio false} "dev/images/ratio.svg"]]]]]]])))))
