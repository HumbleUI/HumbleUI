(ns examples.svg
  (:require
    [clojure.string :as str]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(defn svg [width height xpos ypos scale]
  (ui/clip
    (ui/rect (paint/fill 0xFF90DC48)
      (ui/width width
        (ui/height height
          (ui/svg {:xpos xpos :ypos ypos :scale scale} "dev/images/ratio.svg"))))))

(def ui
  (ui/with-bounds ::bounds
    (ui/dynamic ctx [step (min
                            (quot (:width (::bounds ctx)) 44)
                            (quot (:height (::bounds ctx)) 21))]
      (ui/center
        (ui/column
          (ui/row
            (ui/column
              (svg (* 7 step) (* 3 step) :mid :mid :meet)
              (ui/gap 0 step)
              (svg (* 7 step) (* 3 step) :mid :min :slice))
            (ui/gap step 0)
            (ui/column
              (svg (* 7 step) (* 3 step) :min :mid :meet)
              (ui/gap 0 step)
              (svg (* 7 step) (* 3 step) :mid :mid :slice))
            (ui/gap step 0)
            (ui/column
              (svg (* 7 step) (* 3 step) :max :mid :meet)
              (ui/gap 0 step)
              (svg (* 7 step) (* 3 step) :mid :max :slice))
            (ui/gap step 0)
            (svg (* 3 step) (* 7 step) :mid :min :meet)
            (ui/gap step 0)
            (svg (* 3 step) (* 7 step) :mid :mid :meet)
            (ui/gap step 0)
            (svg (* 3 step) (* 7 step) :mid :max :meet)
            (ui/gap step 0)
            (svg (* 3 step) (* 7 step) :min :mid :slice)
            (ui/gap step 0)
            (svg (* 3 step) (* 7 step) :mid :mid :slice)
            (ui/gap step 0)
            (svg (* 3 step) (* 7 step) :max :mid :slice))
          (ui/gap 0 step)
          (ui/clip
            (ui/rect (paint/fill 0xFF90DC48)
              (ui/width (* 44 step)
                (ui/height (* 7 step)
                  (ui/svg {:preserve-aspect-ratio false} "dev/images/ratio.svg"))))))))))

; (reset! user/*example "svg")