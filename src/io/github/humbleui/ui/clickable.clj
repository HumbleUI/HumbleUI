(ns io.github.humbleui.ui.clickable
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [java.lang AutoCloseable]))

(core/deftype+ Clickable [on-click
                          on-click-capture
                          child
                          ^:mut child-rect
                          ^:mut hovered?
                          ^:mut pressed?
                          ^:mut clicks
                          ^:mut last-click]
  protocols/IContext
  (-context [_ ctx]
    (cond-> ctx
      hovered?                (assoc :hui/hovered? true)
      (and pressed? hovered?) (assoc :hui/active? true)))

  protocols/IComponent
  (-measure [this ctx cs]
    (core/measure child (protocols/-context this ctx) cs))

  (-draw [this ctx rect canvas]
    (set! child-rect rect)
    (set! hovered? (core/rect-contains? child-rect (:mouse-pos ctx)))
    (core/draw-child child (protocols/-context this ctx) child-rect canvas))

  (-event [this ctx event]
    (when (= :mouse-move (:event event))
      (set! clicks 0)
      (set! last-click 0))
          
    (core/eager-or
      (core/when-every [{:keys [x y]} event]
        (let [hovered?' (core/rect-contains? child-rect (core/ipoint x y))]
          (when (not= hovered? hovered?')
            (set! hovered? hovered?')
            true)))
      (let [pressed?' (if (= :mouse-button (:event event))
                        (if (:pressed? event)
                          hovered?
                          false)
                        pressed?)
            clicked? (and pressed? (not pressed?') hovered?)
            now      (core/now)
            _        (when clicked?
                       (when (> (- now last-click) core/double-click-threshold-ms)
                         (set! clicks 0))
                       (set! clicks (inc clicks))
                       (set! last-click now))
            event'   (cond-> event
                       clicked? (assoc :clicks clicks))]
        (core/eager-or
          (when (and clicked? on-click-capture)
            (on-click-capture event')
            false)
          (or
            (core/event-child child (protocols/-context this ctx) event')
            (core/eager-or
              (when (and clicked? on-click)
                (on-click event')
                true)
              (when (not= pressed? pressed?')
                (set! pressed? pressed?')
                true)))))))

  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child (protocols/-context this ctx) cb)))

  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn clickable [opts child]
  (when-not (map? opts)
    (throw (ex-info (str "Expected: map, got: " opts) {:arg opts})))
  (let [{:keys [on-click on-click-capture]} opts]
    (->Clickable on-click on-click-capture child nil false false 0 0)))
