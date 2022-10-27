(ns io.github.humbleui.ui.event-listener
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [java.lang AutoCloseable]))

(core/deftype+ EventListener [listeners child]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (core/draw-child child ctx rect canvas))
  
  (-event [_ ctx event]
    (or
      (when (:capture? listeners)
        (when-some [listener (listeners (:event event))]
          (listener event)))
      (core/event-child child ctx event)
      (when-not (:capture? listeners)
        (when-some [listener (listeners (:event event))]
          (listener event)))))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn event-listener [listeners child]
  (->EventListener listeners child))
