(ns examples.animation
  (:require
    [io.github.humbleui.ui :as ui]))

(defn ui []
  [ui/align {:y :center}
   [ui/vscroll
    [ui/align {:x :center}
     [ui/padding {:padding 20}
      [ui/row {:gap 10}
       [ui/column {:gap 10}
        [ui/animation {:scale :content} "dev/images/animated.gif"]
        [ui/label "GIF"]]
       [ui/column {:gap 10}
        [ui/animation {:scale :content} "dev/images/animated.webp"]
        [ui/label "WebP"]]]]]]])
