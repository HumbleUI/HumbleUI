(ns io.github.humbleui.ui.event-listener
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [java.lang AutoCloseable]))

(core/deftype+ EventListener [event-type callback capture? child]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))
  
  (-draw [_ ctx rect canvas]
    (core/draw-child child ctx rect canvas))
  
  (-event [_ ctx event]
    (or
      (when (and capture?
              (= event-type (:event event)))
        (callback event ctx))
      (core/event-child child ctx event)
      (when (and (not capture?)
              (= event-type (:event event)))
        (callback event ctx))))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn event-listener
  ([event-type callback child]
   (->EventListener event-type callback false child))
  ([opts event-type callback child]
   (->EventListener event-type callback (:capture? opts) child)))
