(ns io.github.humbleui.ui.with-context
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [java.lang AutoCloseable]))

(core/deftype+ WithContext [data child ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child (merge ctx data) cs))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (core/draw-child child (merge ctx data) child-rect canvas))
  
  (-event [_ event]
    (core/event-child child event))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn with-context [data child]
  (->WithContext data child nil))
