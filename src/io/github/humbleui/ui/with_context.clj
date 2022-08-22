(ns io.github.humbleui.ui.with-context
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [java.lang AutoCloseable]))

(core/deftype+ WithContext [data child ^:mut child-rect]
  protocols/IContext
  (-context [_ ctx]
    (merge ctx data))
  
  protocols/IComponent
  (-measure [this ctx cs]
    (core/measure child (protocols/-context this ctx) cs))
  
  (-draw [this ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (core/draw-child child (protocols/-context this ctx) child-rect canvas))
  
  (-event [this ctx event]
    (core/event-child child (protocols/-context this ctx) event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child (protocols/-context this ctx) cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn with-context [data child]
  (->WithContext data child nil))
