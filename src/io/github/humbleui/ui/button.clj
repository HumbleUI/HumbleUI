(ns io.github.humbleui.ui.button
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.ui.align :as align]
    [io.github.humbleui.ui.clickable :as clickable]
    [io.github.humbleui.ui.clip :as clip]
    [io.github.humbleui.ui.dynamic :as dynamic]
    [io.github.humbleui.ui.padding :as padding]
    [io.github.humbleui.ui.rect :as rect]
    [io.github.humbleui.ui.with-context :as with-context]))

(defn button
  ([on-click child]
   (button on-click nil child))
  ([on-click _opts child]
   (dynamic/dynamic ctx [{:hui.button/keys [bg bg-active bg-hovered border-radius padding-left padding-top padding-right padding-bottom]} ctx]
     (clickable/clickable
       {:on-click (when on-click
                    (fn [_] (on-click)))}
       (clip/clip-rrect border-radius
         (dynamic/dynamic ctx [{:hui/keys [hovered? active?]} ctx]
           (rect/rect
             (cond
               active?  bg-active
               hovered? bg-hovered
               :else    bg)
             (padding/padding padding-left padding-top padding-right padding-bottom
               (align/center
                 (with-context/with-context
                   {:hui/active? false
                    :hui/hovered? false}
                   child))))))))))
