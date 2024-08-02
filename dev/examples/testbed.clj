(ns examples.testbed
  (:require
    [io.github.humbleui.util :as util]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  [ui/center
   [ui/vscroll
    [ui/column
     (for [i (range 0 100)]
       [ui/padding {:horizontal 50
                    :vertical 10}
        [ui/center
         [ui/label i]]])]]])
