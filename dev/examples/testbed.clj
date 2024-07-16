(ns examples.testbed
  (:require
    [io.github.humbleui.util :as util]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  [ui/center
   [ui/clickable {}
    (fn [_]
      [ui/with-context {:debug? true}
       [ui/clickable {}
        (fn [state]
          ; (let [e (Exception.)]
          ;   (.printStackTrace e))
          ; (util/log "button" state)
          [ui/rect {:paint (paint/fill 0xFFB0D0FF)}
           [ui/padding {:padding 20}
            "8"]])]])]])
