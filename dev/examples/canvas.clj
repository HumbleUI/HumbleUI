(ns examples.canvas
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.types IPoint]
    [io.github.humbleui.skija Canvas Paint]))

(set! *warn-on-reflection* true)

(def *last-event (atom nil))

(defn on-event [e]
  (when-not (#{:frame :frame-skija} (:event e))
    (reset! *last-event e)
    true))

(defn on-paint [ctx ^Canvas canvas ^IPoint size]
  (let [{:keys [font-ui fill-text leading scale]} ctx
        {:keys [width height]} size
        event @*last-event]
    (with-open [stroke (paint/stroke 0xFF000000 1)]
      (.drawLine canvas 0 0 width height stroke)
      (.drawLine canvas width 0 0 height stroke))
    (loop [y  (+ 10 (* 2 leading scale))
           kv (sort-by first event)]
      (when-some [[k v] (first kv)]
        (do
          (.drawString canvas (str k " " v) (* 10 scale) y font-ui fill-text)
          (recur (+ y (* 2 leading scale)) (next kv)))))))

(def ui
  (ui/valign 0.5
    (ui/halign 0.5
      (ui/canvas {:on-paint on-paint
                  :on-event on-event}))))

; (reset! user/*example "canvas")