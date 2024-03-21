(in-ns 'io.github.humbleui.ui)

(core/deftype+ ACanvas [on-paint on-event]
  :extends ATerminalNode
  
  protocols/IComponent
  (-draw-impl [_ ctx rect ^Canvas canvas]
    (when on-paint
      (canvas/with-canvas canvas
        (.clipRect canvas (core/rect rect))
        (.translate canvas (:x rect) (:y rect))
        (on-paint ctx canvas (core/ipoint (:width rect) (:height rect))))))
  
  (-event-impl [_ ctx event]
    (when on-event
      (let [event' (if (every? event [:x :y])
                     (-> event
                       (update :x - (:x rect))
                       (update :y - (:y rect)))
                     event)]
        (on-event ctx event'))))
  
  (-should-reconcile? [_this _ctx new-element]
    (opts-match? [:on-paint :on-event] element new-element)))

(def ^:private canvas-ctor
  map->ACanvas)
