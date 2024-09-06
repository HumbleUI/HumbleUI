(ns examples.link
  (:require
    [io.github.humbleui.util :as util]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  [ui/align {:y :center}
   [ui/vscroll
    [ui/align {:x :center}
     [ui/padding {:padding 20}
      [ui/column {:gap 10}
       [ui/link #() "This is a link"]
       [ui/with-context {:font-cap-height 20}
        [ui/link #() "This is a bigger link"]]]]]]])
