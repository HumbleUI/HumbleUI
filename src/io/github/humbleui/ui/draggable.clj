(in-ns 'io.github.humbleui.ui)

(util/deftype+ Draggable [on-drag
                          mouse-start
                          mouse-last]
  :extends AWrapperNode
  
  (-event-impl [this ctx event]
    (when (and
            (= :mouse-button (:event event))
            (= :primary (:button event)))
      (cond
        (and
          (:pressed? event)
          (util/rect-contains? bounds (util/ipoint (:x event) (:y event))))
        (do
          (set! mouse-start (util/ipoint (:x event) (:y event)))
          (set! mouse-last  (util/ipoint (:x event) (:y event))))
        
        (not (:pressed? event))
        (do
          (set! mouse-start nil)
          (set! mouse-last nil))))
    
    (util/eager-or
      (when (and
              (= :mouse-move (:event event))
              mouse-start
              on-drag)
        (on-drag
          (assoc event
            :delta-start
            (util/point
              (descaled (- (:x event) (:x mouse-start)) ctx)
              (descaled (- (:y event) (:y mouse-start)) ctx))
            :delta-last
            (util/point
              (descaled (- (:x event) (:x mouse-last)) ctx)
              (descaled (- (:y event) (:y mouse-last)) ctx))))
        (set! mouse-last  (util/ipoint (:x event) (:y event)))
        true)
      (ui/event child ctx event)))
  
  (-reconcile-opts [_ _ new-element]
    (let [opts (parse-opts new-element)]
      (set! on-drag (:on-drag opts)))))

(defn draggable-ctor
  ([child]
   (map->Draggable {}))
  ([opts child]
   (map->Draggable {})))
