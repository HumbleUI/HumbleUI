(ns examples.blur
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija FilterTileMode ImageFilter]))

(defn blur [radius]
  (ImageFilter/makeBlur radius radius FilterTileMode/CLAMP))

(def ui
  (ui/stack
    (ui/vscrollbar
      (ui/image "dev/images/blur.webp"))
    
    (ui/valign 0
      (ui/column
        (ui/backdrop (blur 30)
          (ui/rect (paint/fill 0x20FFFFFF)
            (ui/gap 0 80)))
        
        (ui/backdrop (blur 90)
          (ui/rect (paint/fill 0x20FFFFFF)
            (ui/gap 0 2)))))))
