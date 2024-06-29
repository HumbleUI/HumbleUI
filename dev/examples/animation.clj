(ns examples.animation
  (:require
    [io.github.humbleui.ui :as ui]))

(defn ui []
  [ui/center
   [ui/row {:gap 10}
    [ui/column {:gap 10}
     [ui/animation {:scale :content} "dev/images/animated.gif"]
     [ui/label "GIF"]]
    [ui/column {:gap 10}
     [ui/animation {:scale :content} "dev/images/animated.webp"]
     [ui/label "WebP"]]]])
