(ns io.github.humbleui.ui.with-bounds
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]))

(core/deftype+ WithBounds [key]
  :extends core/AWrapper
  protocols/IContext
  (-context [_ ctx]
    (when-some [scale (:scale ctx)]
      (when-some [width (:width child-rect)]
        (when-some [height (:height child-rect)]
          (assoc ctx key (core/ipoint (/ width scale) (/ height scale)))))))
  
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [width  (-> (:width cs) (/ (:scale ctx)))
          height (-> (:height cs) (/ (:scale ctx)))]
      (core/measure child (assoc ctx key (core/ipoint width height)) cs)))
  
  (-draw [this ctx rect canvas]
    (set! child-rect rect)
    (when-some [ctx' (protocols/-context this ctx)]
      (core/draw-child child ctx' child-rect canvas)))
  
  (-event [this ctx event]
    (when-some [ctx' (protocols/-context this ctx)]
      (core/event-child child ctx' event)))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (when-some [ctx' (protocols/-context this ctx)]
        (protocols/-iterate child ctx' cb)))))

(defn with-bounds [key child]
  (map->WithBounds
    {:key   key
     :child child}))
