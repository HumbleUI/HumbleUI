(ns io.github.humbleui.ui.with-cursor
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.ui.dynamic :as dynamic]
    [io.github.humbleui.window :as window])
  (:import
    [java.lang AutoCloseable]
    [io.github.humbleui.types IPoint IRect]))

(core/deftype+ WithCursor [window cursor child ^:mut ^IPoint mouse-pos ^IRect ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))
  
  (-draw [_ ctx ^IRect rect ^Canvas canvas]
    (set! child-rect rect)
    (core/draw-child child ctx child-rect canvas))
  
  (-event [_ ctx event]
    (when (= :mouse-move (:event event))
      (let [was-inside? (.contains child-rect mouse-pos)
            mouse-pos'  (IPoint. (:x event) (:y event))
            is-inside?  (.contains child-rect mouse-pos')]
        ;; mouse over
        (when (and (not was-inside?) is-inside?)
          (window/set-cursor window cursor))
        ;; mouse out
        (when (and was-inside? (not is-inside?))
          (window/set-cursor window :arrow))
        (set! mouse-pos mouse-pos')))
    (core/event-child child ctx event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn with-cursor [cursor child]
  (dynamic/dynamic ctx [window (:window ctx)]
    (->WithCursor window cursor child (IPoint. 0 0) nil)))
