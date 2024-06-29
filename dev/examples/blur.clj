(ns examples.blur
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija FilterTileMode ImageFilter]))

(defn blur [radius]
  (ImageFilter/makeBlur radius radius FilterTileMode/CLAMP))

(defn ui []
  [ui/stack
   [ui/vscrollbar
    [ui/image {:scale :fit} "dev/images/blur.webp"]]
    
   [ui/valign {:position 0}
    [ui/column
     [ui/backdrop {:filter (blur 30)}
      [ui/rect {:paint (paint/fill 0x20FFFFFF)}
       [ui/gap {:height 80}]]]
        
     [ui/backdrop {:filter (blur 90)}
      [ui/rect {:paint (paint/fill 0x20FFFFFF)}
       [ui/gap {:height 2}]]]]]])
