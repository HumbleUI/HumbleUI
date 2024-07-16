(in-ns 'io.github.humbleui.ui)

(util/deftype+ VScrollable [^:mut offset-px
                            ^:mut offset
                            ^:mut child-size]
  :extends AWrapperNode
  
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [child-cs (assoc cs :height Integer/MAX_VALUE)]
      (set! child-size (protocols/-measure child ctx child-cs))
      (util/ipoint
        (:width child-size)
        (min
          (:height child-size)
          (:height cs)))))
  
  (-draw-impl [_ ctx bounds canvas]
    (let [opts (parse-opts element)]
      (set! child-size (protocols/-measure child ctx (util/ipoint (:width bounds) Integer/MAX_VALUE)))
      (set! offset-px (util/clamp (scaled (or @offset 0)) 0 (- (:height child-size) (:height bounds))))
      (canvas/with-canvas canvas
        (when (:clip? opts true)
          (canvas/clip-rect canvas bounds))
        (let [child-bounds (-> bounds
                             (update :y - offset-px)
                             (update :y math/round)
                             (assoc :height (:height child-size)))]
          (draw child ctx child-bounds canvas)))))
  
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
              (reset! offset (descaled (math/round offset-px')))
              (window/request-frame (:window ctx))))))
      
      :mouse-button
      (when (util/rect-contains? bounds (util/ipoint (:x event) (:y event)))
        (ui/event child ctx event))
      
      #_:else
      (ui/event child ctx event))))

(defn- vscrollable-ctor
  ([child]
   (vscrollable-ctor {} child))
  ([opts child]
   (map->VScrollable
     {:offset-px 0
      :offset    (or
                   (:offset opts)
                   (signal/signal 0))})))
