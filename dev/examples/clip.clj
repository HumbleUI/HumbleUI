(ns examples.clip
  (:require
    [examples.util :as util]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  (util/table
    "Without Clip"
    [ui/with-context {:font-cap-height 25}
     [ui/rect {:paint (paint/fill 0x8080E0FF)}
    
      [ui/size {:width 100 :height 25}
       [ui/label "--Üy--"]]]]
    
    "With Clip"
    [ui/clip
     [ui/with-context {:font-cap-height 25}
      [ui/rect {:paint (paint/fill 0x8080E0FF)}
       [ui/size {:width 100 :height 25}
        [ui/label "--Üy--"]]]]]
    
    "Rounded Clip"
    [ui/clip {:radius 10}
     [ui/with-context {:font-cap-height 25}
      [ui/rect {:paint (paint/fill 0x8080E0FF)}
       [ui/size {:width 100 :height 25}
        [ui/label "--Üy--"]]]]]
    
    "Rounded Clip, 4 radii"
    [ui/clip {:radius [4 8 12 16]}
     [ui/with-context {:font-cap-height 25}
      [ui/rect {:paint (paint/fill 0x8080E0FF)}
       [ui/size {:width 100 :height 25}
        [ui/label "--Üy--"]]]]]))
