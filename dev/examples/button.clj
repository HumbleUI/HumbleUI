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
                  (ui/label "With icon")))))
          
          (ui/gap 0 leading)
          
          (ui/halign 0
            (ui/button #(swap! *clicks inc)
              (ui/dynamic _ [clicks @*clicks]
                (ui/label (str "Dynamic label: " clicks)))))
          
          (ui/gap 0 leading)
          
          (ui/halign 0
            (ui/button #(reset! *clicks 0)
              (ui/label "Reset")))
          
          (ui/gap 0 leading)
          
          (ui/halign 0
            (ui/padding 0 leading
              (ui/dynamic _ [clicks @*clicks]
                (let [s (if (pos? clicks) (str/join "+" (repeat clicks "1")) "None")]
                  (ui/label (str "Clicked: " s)))))))))))

; (require 'user :reload)
; (reset! user/*example "Button")