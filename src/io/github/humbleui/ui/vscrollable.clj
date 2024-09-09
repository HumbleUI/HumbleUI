(in-ns 'io.github.humbleui.ui)

(util/deftype+ VScrollable [clip?
                            offset-px
                            offset
                            child-size]
  :extends AWrapperNode
  
  (-measure-impl [_ ctx cs]
    (let [child-cs (assoc cs :height Integer/MAX_VALUE)]
      (set! child-size (protocols/-measure child ctx child-cs))
      (util/ipoint
        (:width child-size)
        (min
          (:height child-size)
          (:height cs)))))
  
  (-draw-impl [_ ctx bounds container-size viewport canvas]
    (set! child-size (protocols/-measure child ctx (util/ipoint (:width container-size) Integer/MAX_VALUE)))
    (let [max-offset (max 0 (- (:height child-size) (:height bounds)))]
      (set! offset-px (util/clamp (scaled (or @offset 0) ctx) 0 max-offset))
      (reset-changed! offset (descaled (math/round offset-px) ctx))
      (canvas/with-canvas canvas
        (when clip?
          (canvas/clip-rect canvas bounds))
        (let [child-bounds (-> bounds
                             (update :y - offset-px)
                             (update :y math/round)
                             (assoc :height (:height child-size)))]
          (draw child ctx child-bounds (assoc container-size :height Integer/MAX_VALUE) (util/irect-intersect viewport bounds) canvas)))))
  
  (-event-impl [_ ctx event]
    (case (:event event)
      :mouse-scroll
      (when (util/rect-contains? bounds (util/ipoint (:x event) (:y event)))
        (or
          (ui/event child ctx event)
          (let [offset-px' (-> offset-px
                             (- (:delta-y event))
                             (util/clamp 0 (- (:height child-size) (:height bounds))))]
            (when (not= offset-px offset-px')
              (set! offset-px offset-px')
              (reset! offset (descaled (math/round offset-px') ctx))
              (window/request-frame (:window ctx))))))
      
      :mouse-button
      (when (util/rect-contains? bounds (util/ipoint (:x event) (:y event)))
        (ui/event child ctx event))
      
      #_:else
      (ui/event child ctx event)))
  
  (-reconcile-opts [_this _ctx new-element]
    (let [opts (parse-opts element)]
      (set! clip? (:clip? opts true))
      (when-some [offset' (:offset opts)]
        (set! offset offset')))))

(defn- vscrollable-ctor
  ([child]
   (vscrollable-ctor {} child))
  ([opts child]
   (map->VScrollable
     {:offset-px 0
      :offset    (or
                   (:offset opts)
                   (signal 0))})))
