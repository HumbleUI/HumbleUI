(ns io.github.humbleui.ui.key-listener
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [java.lang AutoCloseable]))

(core/deftype+ KeyListener [on-key-down on-key-up child ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (core/draw-child child ctx rect canvas))
  
  (-event [_ ctx event]
    (or
      (core/event-child child ctx event)
      (when (= :key (:event event))
        (if (:pressed? event)
          (when on-key-down
            (on-key-down event))
          (when on-key-up
            (on-key-up event))))))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn key-listener [{:keys [on-key-down on-key-up]} child]
  (->KeyListener on-key-down on-key-up child nil))
