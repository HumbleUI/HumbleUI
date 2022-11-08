(ns examples.tooltip
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]))

(defn tooltip [opts text child]
  (ui/tooltip
    opts
    (ui/rect (paint/fill 0xFFE9E9E9)
      (ui/padding 10 10
        (ui/label text)))
    child))

(def ui
  (ui/center
    (ui/row
      (ui/column
        (interpose
          (ui/gap 0 20)
          (for [align [:top-left
                       :top-right
                       :bottom-left
                       :bottom-right]]
            (tooltip {:shackle align :align :top-left} "top-left"
              (ui/label (str align))))))
      (ui/gap 20 0)
      (ui/column
        (interpose
          (ui/gap 0 20)
          (for [align [:top-left
                       :top-right
                       :bottom-left
                       :bottom-right]]
            (tooltip {:shackle :top-right :anchor align} (str align)
              (ui/label ":top-right"))))))))
