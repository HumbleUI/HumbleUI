(ns examples.testbed
  (:require
    [io.github.humbleui.util :as util]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(def *atom
  (atom []))

(defn label [s]
  {:before-draw
   (fn []
     (swap! *atom conj s))
   :render
   (fn [s]
     [ui/rect {:paint (paint/fill 0x80FFDB2C)}
      [ui/padding {:horizontal 30 :vertical 10}
       (str s)]])})

(ui/defcomp ui []
  (let [*drawn (signal/signal [])]
    {:before-draw
     #(reset! *atom [])
     :after-draw
     #(signal/reset-changed! *drawn @*atom)
     :render
     (fn []
       [ui/center
        [ui/column {:gap 10}
         [ui/align {:x :left}
          [ui/size {:height 100}
           [ui/vscroll
            [ui/column {:gap 2}
             (for [i (range 0 10)]
               [label i])]]]]
         [ui/align {:x :left}
          [ui/label *drawn]]]])}))
