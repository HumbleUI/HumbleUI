(ns examples.align
  (:require
    [clojure.string :as str]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(defn label [text]
  (ui/dynamic ctx [{:keys [font-ui fill-text]} ctx]
    (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFFB2D7FE)))
      (ui/halign 0.5
        (ui/valign 0.5
          (ui/padding 10 10
            (ui/label text font-ui fill-text)))))))

(def ui
  (ui/valign 0.5
    (ui/row
      (ui/gap 1 0)
      (ui/stretch
        (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFFE1EFFA)))
          (ui/column
            (interpose (ui/gap 0 1)
              (for [i (range 0 1.1 1/10)]
                (ui/stretch
                  (ui/halign i
                    (ui/width :ratio (float 1/3)
                      (label (case i 0 "Left" 1/2 "Centre" 1 "Right" (str (float i)))))))))
            (ui/gap 0 1)
            (ui/stretch (label "Stretch")))))
      (ui/gap 1 0)
      (ui/stretch
        (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFFE1EFFA)))
          (ui/row
            (interpose (ui/gap 1 0)
              (for [i (range 0 1.1 1/10)]
                (ui/stretch
                  (ui/valign i
                    (ui/height :ratio (float 1/3)
                      (label (case i 0 "Top" 1/2 "Middle" 1 "Bottom" (str (float i)))))))))
            (ui/gap 1 0)
            (ui/stretch (label "Stretch")))))
      (ui/gap 1 0))))