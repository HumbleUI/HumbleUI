(in-ns 'io.github.humbleui.ui)

(util/deftype+ ACanvas [^:mut on-paint
                        ^:mut on-event]
  :extends ATerminalNode
  
  (-measure-impl [_ ctx cs]
    (util/ipoint 0 0))
  
  (-draw-impl [_ ctx bounds viewport ^Canvas canvas]
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
  
    (-reconcile-opts [this _ctx new-element]
    (let [opts (parse-opts new-element)]
      (set! on-paint (:on-paint opts))
      (set! on-event (:on-event opts)))))

(defn canvas-ctor [opts]
  (map->ACanvas {}))
