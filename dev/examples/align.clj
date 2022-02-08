(ns examples.align
  (:require
    [clojure.string :as str]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(defn label [text]
  (ui/dynamic ctx [{:keys [leading font-ui fill-text]} ctx]
    (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xffade8f4)))
      (ui/halign 0.5
        (ui/padding 20 leading
          (ui/label text font-ui fill-text))))))

(def ui
  (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xffffffff)))
    (ui/width-ratio 0.5
      (ui/column
        (ui/halign 0 (label "Left"))
        (ui/gap 0 1)
        (ui/halign 0.5 (label "Center"))
        (ui/gap 0 1)
        (ui/halign 1 (label "Right"))
        (ui/gap 0 1)
        (label "Stretch")))))