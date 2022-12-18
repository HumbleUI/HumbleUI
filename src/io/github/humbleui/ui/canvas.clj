(ns io.github.humbleui.ui.canvas
  (:require
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [io.github.humbleui.skija Canvas]))

(core/deftype+ ACanvas [on-paint on-event ^:mut my-rect]
  :extends core/ATerminal
  
  protocols/IComponent
  (-draw [_ ctx rect ^Canvas canvas]
    (set! my-rect rect)
    (when on-paint
      (canvas/with-canvas canvas
        (.clipRect canvas (core/rect rect))
        (.translate canvas (:x rect) (:y rect))
        (on-paint ctx canvas (core/ipoint (:width rect) (:height rect))))))
  
  (-event [_ ctx event]
    (when on-event
      (let [event' (if (every? event [:x :y])
                     (-> event
                       (update :x - (:x my-rect))
                       (update :y - (:y my-rect)))
                     event)]
        (on-event ctx event')))))

(def canvas map->ACanvas)
