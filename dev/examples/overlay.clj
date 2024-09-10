(ns examples.overlay
  (:require
    [clojure.string :as str]
    [examples.shared :as shared]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija FilterTileMode ImageFilter]))

(ui/defcomp ui []
  (let [*toggle1 (ui/signal false)
        *toggle2 (ui/signal false)
        *toggle3 (ui/signal false)
        *toggle4 (ui/signal false)
        blur     (ImageFilter/makeBlur 15 15 FilterTileMode/CLAMP)]
    (fn []
      (shared/table
        "Relative to position"
        [ui/column
         [ui/button {:on-click (fn [_] (swap! *toggle1 not))}
          "Toggle relative"]
         (when @*toggle1
           [ui/overlay
            [ui/padding {:top 5}
             [ui/rect {:paint {:fill "FD2"}}
              [ui/padding {:padding 10}
               [ui/label "Overlay"]]]]])]
        
        "Relative w/ scroll"
        [ui/column
         [ui/button {:on-click (fn [_] (swap! *toggle2 not))}
          "Toggle scroll"]
         (when @*toggle2
           [ui/overlay
            [ui/padding {:top 5}
               [ui/rect {:paint {:fill "FFF"}}
                [ui/vscroll
                 [ui/column
                  (for [i (range 0 50)]
                    [ui/padding {:padding 10}
                     [ui/label "Item #" i]])]]]]])]
                
        "Absolute positioning"
        [ui/column
         [ui/button {:on-click (fn [_] (swap! *toggle3 not))}
          "Toggle absolute"]
         (when @*toggle3
           [ui/overlay
            [ui/size {:width #(:width %)
                      :height #(:height %)}
             [ui/padding {:padding 100}
              [ui/rect {:paint {:fill "FD2"}}
               [ui/center
                [ui/button {:on-click (fn [_] (swap! *toggle3 not))}
                 "Close"]]]]]])]
        
        "Close with click outside"
        [ui/column
         [ui/button {:on-click (fn [_] (swap! *toggle4 not))}
          "Toggle w/ bg"]
         (when @*toggle4
           [ui/column
            [ui/overlay
             [ui/size {:width #(:width %)
                       :height #(:height %)}
              [ui/clickable
               {:on-click (fn [_] (swap! *toggle4 not))}
               [ui/rect {:paint {:fill "00000020"}}
                [ui/backdrop {:filter blur}
                 [ui/gap]]]]]]
            [ui/overlay
             [ui/padding {:vertical 10}
              [ui/clip {:radius 4}
               [ui/rect {:paint {:fill "FFF"}}
                [ui/vscroll
                 [ui/column
                  (for [i (range 0 50)]
                    [ui/padding {:padding 10}
                     [ui/label "Item #" i]])]]]]]]])]))))
