(ns examples.viewport
  (:require
    [examples.shared :as shared]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(defn label [*atom s]
  {:before-draw
   (fn []
     (swap! *atom conj s))
   :render
   (fn [*atom s]
     [ui/rect {:paint (paint/fill 0x80FFDB2C)}
      [ui/padding {:horizontal 40 :vertical 10}
       (str s)]])})

(ui/defcomp ui []
  (shared/table
    "Virtualized rendering: column"
    (let [*atom  (atom [])
          *drawn (signal/signal [])]
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
                 [label *atom i])]]]]
           "Drawn:"
           [ui/align {:x :left}
            [ui/label *drawn]]]])})
    
    "Virtualized rendering: grid"
    (let [*atom  (atom [])
          *drawn (signal/signal [])]
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
              [ui/grid {:cols 2}
               (for [i (range 0 20)]
                 [label *atom i])]]]]
           "Drawn:"
           [ui/align {:x :left}
            [ui/label *drawn]]]])})))
