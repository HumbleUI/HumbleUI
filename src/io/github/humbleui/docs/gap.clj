(ns io.github.humbleui.docs.gap
  (:require
    [clojure.string :as str]
    [io.github.humbleui.docs.shared :as shared]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  (shared/table
    "An empty terminal component"
    [ui/rect {:paint {:fill "FD2"}}
     [ui/gap {:width  50
              :height 50}]]
    
    "Can be used only in one direction. Horizontal"
    [ui/row
     "A"
     [ui/rect {:paint {:fill "FD2"}}
      [ui/gap {:width 100}]]
     "B"]
    
    "Vertical"
    [ui/column
     "A"
     [ui/rect {:paint {:fill "FD2"}}
      [ui/gap {:height 50}]]
     "B"]
    
    "Useful if you need to put ^:stretch on something"
    [ui/size {:width #(:width %)}
     [ui/row "A" ^:stretch [ui/gap] "B"]]
    
    "Has no size on its own"
    [ui/rect {:paint {:fill "FD2"}}
     [ui/gap]]))
