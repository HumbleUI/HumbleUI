(ns examples.button
  (:require
    [clojure.string :as str]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(defonce *clicks (atom 0))

(def ui
  (ui/dynamic ctx [{:keys [leading font-ui fill-text]} ctx]
    (ui/column
      (ui/clickable
        #(swap! *clicks inc)
        (ui/clip-rrect 4
          (ui/dynamic ctx [{:keys [hui/active? hui/hovered? scale font-ui leading fill-text]} ctx]
            (let [color (cond
                          active?  0xFF48cae4
                          hovered? 0xFFcaf0f8
                          :else    0xFFade8f4)]
              (ui/fill (doto (Paint.) (.setColor (unchecked-int color)))
                (ui/halign 0.5
                  (ui/padding 20 leading
                    (ui/label "Increment" font-ui fill-text))))))))
      (ui/gap 0 leading)
      (ui/clickable
        #(reset! *clicks 0)
        (ui/halign 0.5
          (ui/clip-rrect 4
            (ui/dynamic ctx [{:keys [hui/active? hui/hovered? scale font-ui leading fill-text]} ctx]
              (let [color (cond
                            active?  0xFF48cae4
                            hovered? 0xFFcaf0f8
                            :else    0xFFade8f4)]
                (ui/fill (doto (Paint.) (.setColor (unchecked-int color)))
                  (ui/padding 20 leading
                    (ui/label "Reset" font-ui fill-text))))))))
      (ui/gap 0 leading)
      (ui/halign 0.5
        (ui/dynamic _ [clicks @*clicks]
          (let [s (if (pos? clicks) (str/join "+" (repeat clicks "1")) "None")]
            (ui/label (str "Clicked: " s) font-ui fill-text)))))))