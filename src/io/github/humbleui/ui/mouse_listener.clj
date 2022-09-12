(ns io.github.humbleui.ui.mouse-listener
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [io.github.humbleui.types IPoint IRect]
    [java.lang AutoCloseable]))

(core/deftype+ MouseListener [on-move
                              on-scroll
                              on-button
                              on-over
                              on-out
                              child
                              ^:mut over?
                              ^:mut ^IRect child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (set! over? (.contains child-rect ^IPoint (:mouse-pos ctx)))
    (core/draw-child child ctx rect canvas))
  
  (-event [_ ctx event]
    (core/when-every [{:keys [x y]} event]
      (let [over?' (.contains child-rect (IPoint. x y))]
        (when (and (not over?) over?' on-over)
          (on-over event))
        (when (and over? (not over?') on-out)
          (on-out event))
        (set! over? over?')))
        
    (core/eager-or
      (when (and on-move
              over?
              (= :mouse-move (:event event)))
        (on-move event))
      (when (and on-scroll
              over?
              (= :mouse-scroll (:event event)))
        (on-scroll event))
      (when (and on-button
              over?
              (= :mouse-button (:event event)))
        (on-button event))
      (core/event-child child ctx event)))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn mouse-listener [{:keys [on-move on-scroll on-button on-over on-out]} child]
  (->MouseListener on-move on-scroll on-button on-over on-out child false nil))
