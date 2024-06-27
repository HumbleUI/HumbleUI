(ns examples.tooltip
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]))

(defn tooltip [opts child]
  [ui/tooltip
   (update opts :tip
     (fn [text]
       [ui/rect {:paint (paint/fill 0xFFE9E9E9)}
        [ui/padding {:padding 10}
         [ui/label text]]]))
   child])

(defn ui []
  [ui/center
   [ui/grid {:cols 4}
    (for [anchor [:top-left
                  :top-right
                  :bottom-left
                  :bottom-right]
          shackle [:top-left
                   :top-right
                   :bottom-left
                   :bottom-right]]
      [ui/padding {:padding 20}
       [tooltip 
        {:shackle shackle
         :anchor  anchor
         :tip     (name anchor)}
        [ui/label (name shackle)]]])]])
