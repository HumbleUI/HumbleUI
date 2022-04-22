(ns examples.event-bubbling
  (:require
    [clojure.string :as str]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(def button-opts
  {:bg         0x20000000
   :bg-hovered 0x40000000
   :bg-active  0x80000000})

(def ui
  (ui/dynamic ctx [{:keys [font-ui fill-text leading]} ctx]
    (ui/halign 0.5
      (ui/row
        (ui/valign 0.5
          (ui/button nil button-opts
            (ui/column
              (ui/button nil button-opts
                (ui/label "Inner button 1"))
              (ui/gap 0 leading)
              (ui/button nil button-opts
                (ui/label "Inner button 2")))))

        (ui/gap 20 0)

        (ui/vscrollbar
          (ui/vscroll
            (ui/column
              (for [i (range 1 6)]
                (ui/padding 20 leading
                  (ui/label (str "Item " i))))
              
              (ui/height 130
                (ui/padding 0 0 12 0
                  (ui/fill (paint/stroke 0xFF000000 1)
                    (ui/vscrollbar
                      (ui/vscroll
                        (ui/column
                          (for [ch (map str "ABCDEFGHIJKLMN")]
                            (ui/padding 20 leading
                              (ui/label (str "Nested " ch))))))))))

              (for [i (range 6 12)]
                (ui/padding 20 leading
                  (ui/label (str "Item " i)))))))))))

; (reset! user/*example "event_bubbling")