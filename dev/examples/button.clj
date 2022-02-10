(ns examples.button
  (:require
    [clojure.string :as str]
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
          [:hug nil (ui/clickable
                      #(swap! *clicks inc)
                      (ui/clip-rrect 4
                        (ui/dynamic ctx [{:keys [hui/active? hui/hovered? scale font-ui leading fill-text]} ctx]
                          (let [color (cond
                                        active?  0xFFA2C7EE
                                        hovered? 0xFFCFE8FC
                                        :else    0xFFB2D7FE)]
                            (ui/fill (doto (Paint.) (.setColor (unchecked-int color)))
                              (ui/halign 0.5
                                (ui/padding 20 leading
                                  (ui/label "Increment" font-ui fill-text))))))))]
          [:hug nil (ui/gap 0 leading)]
          [:hug nil (ui/clickable
                      #(reset! *clicks 0)
                      (ui/halign 0.5
                        (ui/clip-rrect 4
                          (ui/dynamic ctx [{:keys [hui/active? hui/hovered? scale font-ui leading fill-text]} ctx]
                            (let [color (cond
                                          active?  0xFFA2C7EE
                                          hovered? 0xFFCFE8FC
                                          :else    0xFFB2D7FE)]
                              (ui/fill (doto (Paint.) (.setColor (unchecked-int color)))
                                (ui/padding 20 leading
                                  (ui/label "Reset" font-ui fill-text))))))))]
          [:hug nil (ui/gap 0 leading)]
          [:hug nil (ui/halign 0.5
                      (ui/dynamic _ [clicks @*clicks]
                        (let [s (if (pos? clicks) (str/join "+" (repeat clicks "1")) "None")]
                          (ui/label (str "Clicked: " s) font-ui fill-text))))])))))