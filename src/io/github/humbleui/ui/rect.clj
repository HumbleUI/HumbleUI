(ns io.github.humbleui.ui.rect
  (:require
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [io.github.humbleui.types RRect]
    [java.lang AutoCloseable]))

(core/deftype+ Rect [paint child ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (canvas/draw-rect canvas rect paint)
    (core/draw-child child ctx child-rect canvas))
  
  (-event [_ ctx event]
    (core/event-child child ctx event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn rect [paint child]
  (->Rect paint child nil))

(core/deftype+ RoundedRect [radius paint child ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (canvas/draw-rect canvas (RRect/makeXYWH (:x rect) (:y rect) (:width rect) (:height rect) (* radius (:scale ctx))) paint)
    (core/draw-child child ctx child-rect canvas))
  
  (-event [_ ctx event]
    (core/event-child child ctx event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn rounded-rect [opts paint child]
  (->RoundedRect (:radius opts) paint child nil))
