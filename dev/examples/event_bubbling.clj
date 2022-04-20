(ns examples.event-bubbling
  (:require
    [clojure.string :as str]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(defn button [child]
  (ui/clickable
    nil
    (ui/clip-rrect 4
      (ui/dynamic ctx [{:keys [hui/active? hui/hovered?]} ctx]
        (ui/fill
          (cond
            active?  (paint/fill 0x80000000)
            hovered? (paint/fill 0x80404040)
            :else    (paint/fill 0x80808080))
          (ui/padding 20 20
            (ui/with-context
              {:hui/active? false
               :hui/hovered? false}
              child)))))))

(def ui
  (ui/dynamic ctx [{:keys [font-ui fill-text leading]} ctx]
    (ui/halign 0.5
      (ui/row
        (ui/valign 0.5
          (button
            (ui/column
              (button
                (ui/label "Inner button 1" font-ui fill-text))
              (ui/gap 0 leading)
              (button
                (ui/label "Inner button 2" font-ui fill-text)))))

        (ui/gap 20 0)

        (ui/vscrollbar
          (ui/vscroll
            (ui/column
              (for [i (range 1 6)]
                (ui/padding 20 leading
                  (ui/label (str "Item " i) font-ui fill-text)))
              
              (ui/height 130
                (ui/padding 0 0 12 0
                  (ui/fill (paint/stroke 0xFF000000 1)
                    (ui/vscrollbar
                      (ui/vscroll
                        (ui/column
                          (for [ch (map str "ABCDEFGHIJKLMN")]
                            (ui/padding 20 leading
                              (ui/label (str "Nested " ch) font-ui fill-text)))))))))

              (for [i (range 6 12)]
                (ui/padding 20 leading
                  (ui/label (str "Item " i) font-ui fill-text))))))))))

(require 'user :reload)
(reset! user/*example "Event Bubbling")