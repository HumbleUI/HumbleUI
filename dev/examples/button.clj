(ns examples.button
  (:require
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(defonce *clicks (atom 0))

(def ui
  (ui/dynamic ctx [{:keys [scale font-ui leading fill-text]} ctx]
    (ui/column
      (ui/clickable
        #(swap! *clicks inc)
        (ui/clip-rrect 4
          (ui/dynamic ctx [active?  (:hui/active? ctx)
                           hovered? (:hui/hovered? ctx)]
            (let [[label color] (cond
                                  active?  ["Active"    0xFF48cae4]
                                  hovered? ["Hovered"   0xFFcaf0f8]
                                  :else    ["Unpressed" 0xFFade8f4])]
              (ui/fill (doto (Paint.) (.setColor (unchecked-int color)))
                (ui/padding 20 leading
                  (ui/label label font-ui fill-text)))))))
      (ui/gap 0 leading)
      (ui/dynamic _ [clicks @*clicks]
        (ui/label (str "Clicked: " clicks) font-ui fill-text)))))