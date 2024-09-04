(ns examples.clip
  (:require
    [examples.shared :as shared]
    [io.github.humbleui.util :as util]
        [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  (shared/table
    "Without Clip"
    [ui/with-context {:font-cap-height 25}
     [ui/rect {:paint {:fill 0x80FFDB2C}}
      [ui/size {:width 100 :height 25}
       [ui/label "--Üy--"]]]]
    
    "With Clip"
    [ui/clip
     [ui/with-context {:font-cap-height 25}
      [ui/rect {:paint {:fill 0x80FFDB2C}}
       [ui/size {:width 100 :height 25}
        [ui/label "--Üy--"]]]]]
    
    "Rounded Clip"
    [ui/clip {:radius 10}
     [ui/with-context {:font-cap-height 25}
      [ui/rect {:paint {:fill 0x80FFDB2C}}
       [ui/size {:width 100 :height 25}
        [ui/label "--Üy--"]]]]]
    
    "Rounded Clip, 4 radii"
    [ui/clip {:radius [4 8 12 16]}
     [ui/with-context {:font-cap-height 25}
      [ui/rect {:paint {:fill 0x80FFDB2C}}
       [ui/size {:width 100 :height 25}
        [ui/label "--Üy--"]]]]]))
