(ns examples.testbed
  (:require
    [io.github.humbleui.util :as util]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  (let [*open (ui/signal false)]
    (fn []
      [ui/center
       [ui/column
        [ui/button
         {:on-click (fn [_]
                      (swap! *open not))}
         [ui/label "Toggle: " @*open]]
        (when @*open
          [ui/overlay
           [ui/size {:width #(:width %)
                     :height #(:height %)}
            [ui/padding {:top 10 :right 10 :bottom 10 :left 10}
             [ui/shadow {:dy 2, :blur 10, :color 0x20000000}
              [ui/shadow {:dy 0, :blur 4, :color 0x10000000}
              
               [ui/rect {:paint {:fill "FD2"}}
                [ui/gap]]]
              #_[ui/clip {:radius 4}
                 [ui/rect {:paint {:fill "FFF"}}
                  [ui/vscroll
                   [ui/padding {:vertical 4}
                    [ui/column
                     (for [i (range 40)]
                       [ui/clickable {}
                        (fn [state]
                          [ui/rect {:paint {:fill (cond
                                                    (:hovered state) "B9DCFD"
                                                    :else            "FFF")}}
                           [ui/padding {:padding 10}
                            [ui/label "Item " i]]])])]]]]]]]]])]])))
