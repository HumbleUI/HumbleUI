(ns examples.button
  (:require
    [clojure.string :as str]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(defonce *clicks (atom 0))

(def ui
  (ui/valign 0.5
    (ui/halign 0.5
      (ui/dynamic ctx [{:keys [leading font-ui fill-text]} ctx]
        (ui/column
          (ui/halign 0
            (ui/button #(swap! *clicks inc)
              (ui/label "Increment")))
          
          (ui/gap 0 leading)
          
          (ui/halign 0
            (ui/button #(swap! *clicks inc)
              (ui/row
                (ui/image "dev/images/add.png")
                (ui/gap 5 0)
                (ui/valign 0.5
                  (ui/label "With PNG icon")))))
          
          (ui/gap 0 leading)
          
          (ui/halign 0
            (ui/button #(swap! *clicks inc)
              (ui/row
                (ui/width 14
                  (ui/height 14
                    (ui/svg "dev/images/add.svg")))
                (ui/gap 5 0)
                (ui/valign 0.5
                  (ui/label "With SVG icon")))))
          
          (ui/gap 0 leading)
          
          (ui/halign 0
            (ui/button #(swap! *clicks inc)
              (ui/dynamic _ [clicks @*clicks]
                (ui/label (str "Dynamic label: " clicks)))))
          
          (ui/gap 0 leading)
          
          (ui/halign 0
            (ui/button #(reset! *clicks 0)
              (ui/label "Reset"))))))))

; (require 'user :reload)
; (reset! user/*example "Button")