(ns io.github.humbleui.docs.rect
  (:require
    [io.github.humbleui.docs.shared :as shared]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  (shared/table
    "Solid fill"
    [ui/rect {:paint {:fill 0x80FFDB2C}}
     [ui/padding {:padding 10}
      "Solid fill"]]

    "Stroke"
    [ui/rect {:paint {:stroke 0x80FFDB2C, :width 2}}
     [ui/padding {:padding 10}
      "Stroke"]]
    
    "Rounded: 1 radius"
    [ui/rect {:paint  {:fill 0x80FFDB2C}
              :radius 10}
     [ui/padding {:padding 10}
      "1 radius"]]

    "Rounded: 2 radii"
    [ui/rect {:paint  {:fill 0x80FFDB2C}
              :radius [5 15]}
     [ui/padding {:padding 10}
      "2 radii"]]
    
    "Rounded: 4 radii"
    [ui/rect {:paint  {:fill 0x80FFDB2C}
              :radius [5 10 15 20]}
     [ui/padding {:padding 10}
      "4 radii"]]
    
    "Rounded: 8 radii"
    [ui/rect {:paint  {:fill 0x80FFDB2C}
              :radius [0 3 6 9 12 15 18 21]}
     [ui/padding {:padding 10}
      "8 radii"]]))
