(ns examples.split
  (:require
    [examples.shared :as shared]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  (shared/table
    "Horizontal split, default 50/50 start"
    [ui/size {:width #(:width %)}
     [ui/hsplit
      [ui/rect {:paint {:fill "E0C626"}}
       [ui/gap {:height 100}]]
      [ui/rect {:paint {:fill "FDE442"}}
       [ui/gap {:height 100}]]]]
    
    "Opts"
    [ui/size {:width #(:width %)}
     [ui/hsplit {:width 100
                 :gap 20}
      [ui/rect {:paint {:fill "E0C626"}}
       [ui/gap {:height 100}]]
      [ui/rect {:paint {:fill "FDE442"}}
       [ui/gap {:height 100}]]]]
    
    "Dynamic starting width"
    [ui/size {:width #(:width %)}
     [ui/hsplit {:width #(* 0.33 (:width %))}
      [ui/rect {:paint {:fill "E0C626"}}
       [ui/gap {:height 100}]]
      [ui/rect {:paint {:fill "FDE442"}}
       [ui/gap {:height 100}]]]]
    
    "Custom gap" 
    [ui/size {:width #(:width %)}
     [ui/hsplit {:gap [ui/clickable {}
                       (fn [state]
                         [ui/rect
                          {:paint
                           {:fill
                            (cond
                              (:held state)    "00000040"
                              (:hovered state) "00000020"
                              :else            "00000010")}}
                          [ui/gap {:width 8}]])]}
      [ui/rect {:paint {:fill "E0C626"}}
       [ui/gap {:height 100}]]
      [ui/rect {:paint {:fill "FDE442"}}
       [ui/gap {:height 100}]]]]))
