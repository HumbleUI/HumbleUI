(ns io.github.humbleui.ui.clickable
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [java.lang AutoCloseable]
    [io.github.humbleui.types IPoint IRect]))

(set! *warn-on-reflection* true)

(core/deftype+ Clickable [on-click
                          on-click-capture
                          child
                          ^:mut ^IRect child-rect
                          ^:mut hovered?
                          ^:mut pressed?]
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
    (set! hovered? (.contains child-rect ^IPoint (:mouse-pos ctx)))
    (core/draw-child child (protocols/-context this ctx) child-rect canvas))
  
  (-event [this ctx event]
    (core/eager-or
      (core/when-every [{:keys [x y]} event]
        (let [hovered?' (.contains ^IRect child-rect (IPoint. x y))]
          (when (not= hovered? hovered?')
            (set! hovered? hovered?')
            true)))
      (let [pressed?' (if (= :mouse-button (:event event))
                        (if (:pressed? event)
                          hovered?
                          false)
                        pressed?)
            clicked? (and pressed? (not pressed?') hovered?)]
        (core/eager-or
          (when (and clicked? on-click-capture)
            (on-click-capture event)
            false)
          (or
            (core/event-child child (protocols/-context this ctx) event)
            (when (and clicked? on-click)
              (on-click event)
              false))
          (when (not= pressed? pressed?')
            (set! pressed? pressed?')
            true)))))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child (protocols/-context this ctx) cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn clickable [{:keys [on-click on-click-capture]} child]
  (->Clickable on-click on-click-capture child nil false false))
