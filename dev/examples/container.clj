(ns examples.container
  (:require
    [clojure.string :as str]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(defn label [text]
  (ui/dynamic ctx [{:keys [font-ui fill-text leading]} ctx]
    (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFFB2D7FE)))
      (ui/halign 0.5
        (ui/valign 0.5
          (ui/padding 10 leading
            (ui/label text font-ui fill-text)))))))

(def ui
  (ui/dynamic ctx [{:keys [font-ui fill-text leading]} ctx]
    (ui/valign 0.5
      (ui/halign 0.5
        (ui/column
          [:hug nil (ui/padding 0 leading
                      (ui/label "Hug" font-ui fill-text))]
          [:hug nil (ui/row
                      [:hug nil (label "Ok")]
                      [:hug nil (ui/gap 10 0)]
                      [:hug nil (label "Cancel")]
                      [:hug nil (ui/gap 10 0)]
                      [:hug nil (label "Abort request")])]
          [:hug nil (ui/gap 0 leading)]
          
          [:hug nil (ui/padding 0 leading
                      (ui/label "Stretch 1-1-1" font-ui fill-text))]
          [:hug nil (ui/row
                      [:stretch 1 (label "Ok")]
                      [:hug nil (ui/gap 10 0)]
                      [:stretch 1 (label "Cancel")]
                      [:hug nil (ui/gap 10 0)]
                      [:stretch 1 (label "Abort request")])]
          [:hug nil (ui/gap 0 leading)]
          
          [:hug nil (ui/padding 0 leading
                      (ui/label "Stretch 3-2-1" font-ui fill-text))]
          [:hug nil (ui/row
                      [:stretch 3 (label "Ok")]
                      [:hug nil (ui/gap 10 0)]
                      [:stretch 2 (label "Cancel")]
                      [:hug nil (ui/gap 10 0)]
                      [:stretch 1 (label "Abort request")])]
          [:hug nil (ui/gap 0 leading)]
          
          [:hug nil (ui/padding 0 leading
                      (ui/label "Hug 20%-30%-40%" font-ui fill-text))]
          [:hug nil (ui/row
                      [:hug nil (ui/width :ratio 0.2 (label "Ok"))]
                      [:hug nil (ui/gap 10 0)]
                      [:hug nil (ui/width :ratio 0.3 (label "Cancel"))]
                      [:hug nil (ui/gap 10 0)]
                      [:hug nil (ui/width :ratio 0.4 (label "Abort request"))])]
          [:hug nil (ui/gap 0 leading)]
          )))))