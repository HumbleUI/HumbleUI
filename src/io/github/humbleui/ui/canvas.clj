(in-ns 'io.github.humbleui.ui)

(util/deftype+ ACanvas [on-paint on-event]
  :extends ATerminalNode
  
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (util/ipoint 0 0))
  
  (-draw-impl [_ ctx bounds ^Canvas canvas]
    (when on-paint
      (canvas/with-canvas canvas
        (.clipRect canvas (util/rect bounds))
        (.translate canvas (:x bounds) (:y bounds))
        (on-paint ctx canvas (util/ipoint (:width bounds) (:height bounds))))))
  
  (-event-impl [_ ctx event]
    (when on-event
      (let [event' (if (every? event [:x :y])
                     (-> event
                       (update :x - (:x bounds))
                       (update :y - (:y bounds)))
                     event)]
        (on-event ctx event'))))
  
  (-should-reconcile? [_this _ctx new-element]
    (opts-match? [:on-paint :on-event] element new-element)))

(def ^:private canvas-ctor
  map->ACanvas)
