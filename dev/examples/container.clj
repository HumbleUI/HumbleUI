(ns examples.container
  (:require
    [clojure.string :as str]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(defn label [text]
  (ui/dynamic ctx [{:keys [leading]} ctx]
    (ui/rect (paint/fill 0xFFB2D7FE)
      (ui/center
        (ui/padding 10 leading
          (ui/label text))))))

(def ui
  (ui/dynamic ctx [{:keys [font-ui fill-text leading]} ctx]
    (ui/center
      (ui/column
        (ui/padding 0 leading
          (ui/label "Hug"))
        (ui/row
          (label "Ok")
          (ui/gap 10 0)
          (label "Cancel")
          (ui/gap 10 0)
          (label "Abort request"))
        (ui/gap 0 leading)
          
        (ui/padding 0 leading
          (ui/label "Stretch 1-1-1"))
        (ui/row
          [:stretch 1 (label "Ok")]
          (ui/gap 10 0)
          [:stretch 1 (label "Cancel")]
          (ui/gap 10 0)
          [:stretch 1 (label "Abort request")])
        (ui/gap 0 leading)
          
        (ui/padding 0 leading
          (ui/label "Stretch 3-2-1"))
        (ui/row
          [:stretch 3 (label "Ok")]
          (ui/gap 10 0)
          [:stretch 2 (label "Cancel")]
          (ui/gap 10 0)
          [:stretch 1 (label "Abort request")])
        (ui/gap 0 leading)
          
        (ui/padding 0 leading
          (ui/label "Hug 20%-30%-40%"))
        (ui/row
          (ui/width #(* 0.2 (:width %)) (label "Ok"))
          (ui/gap 10 0)
          (ui/width #(* 0.3 (:width %)) (label "Cancel"))
          (ui/gap 10 0)
          (ui/width #(* 0.4 (:width %)) (label "Abort request")))
        (ui/gap 0 leading)))))

; (reset! user/*example "container")