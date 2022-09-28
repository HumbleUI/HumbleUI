(ns io.github.humbleui.ui.backdrop
  (:require
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [io.github.humbleui.types IRect]
    [io.github.humbleui.skija Canvas ImageFilter SaveLayerRec]
    [java.lang AutoCloseable]))

(core/deftype+ Backdrop [^ImageFilter filter child ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))
  
  (-draw [_ ctx ^IRect rect ^Canvas canvas]
    (set! child-rect rect)
    (canvas/with-canvas canvas
      (canvas/clip-rect canvas rect)
      (.saveLayer canvas (SaveLayerRec. (.toRect rect) nil filter)))
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

(defn backdrop [filter child]
  (->Backdrop filter child nil))
