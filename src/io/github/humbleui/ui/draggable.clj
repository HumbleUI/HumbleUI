(ns io.github.humbleui.ui.draggable
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.ui.dynamic :as dynamic])
  (:import
    [io.github.humbleui.types IPoint IRect]
    [io.github.humbleui.skija Canvas]
    [java.lang AutoCloseable]))

(set! *warn-on-reflection* true)

(defn ^IRect child-rect [draggable]
  (let [{:keys [my-pos child-pos child-size]} draggable]
    (IRect/makeXYWH
      (+ (:x my-pos) (:x child-pos))
      (+ (:y my-pos) (:y child-pos))
      (:width child-size)
      (:height child-size))))

(core/deftype+ Draggable [child
                          ^:mut ^IPoint my-pos
                          ^:mut ^IPoint child-pos
                          ^:mut ^IPoint child-size
                          ^:mut ^IPoint mouse-start]
  protocols/IComponent
  (-measure [_ ctx cs]
    cs)
  
  (-draw [this ctx ^IRect rect ^Canvas canvas]
    (set! my-pos (IPoint. (:x rect) (:y rect)))
    (set! child-size (core/measure child ctx (IPoint. (:width rect) (:height rect))))
    (core/draw-child child ctx (child-rect this) canvas))
  
  (-event [this ctx event]
    (when (and
            (= :mouse-button (:event event))
            (= :primary (:button event))
            (:pressed? event)
            (.contains (child-rect this) (IPoint. (:x event) (:y event))))
      (set! mouse-start
        (IPoint.
          (- (:x child-pos) (:x event))
          (- (:y child-pos) (:y event)))))
    
    (when (and
            (= :mouse-button (:event event))
            (= :primary (:button event))
            (not (:pressed? event)))
      (set! mouse-start nil))
    
    (core/eager-or
      (when (and
              (= :mouse-move (:event event))
              mouse-start)
        (set! child-pos
          (IPoint.
            (+ (:x mouse-start) (:x event))
            (+ (:y mouse-start) (:y event))))
        true)
      (core/event-child child ctx event)))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn draggable
  ([child]
   (draggable {} child))
  ([opts child]
   (dynamic/dynamic ctx [{:keys [scale]} ctx]
     (->Draggable child
       nil
       (or (some-> ^IPoint (:pos opts) (.scale scale)) IPoint/ZERO)
       nil
       nil))))
