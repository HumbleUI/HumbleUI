(in-ns 'io.github.humbleui.ui)

(core/deftype+ VScrollable [^:mut offset-px
                            ^:mut offset
                            ^:mut child-size]
  :extends AWrapperNode
  
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [child-cs (assoc cs :height Integer/MAX_VALUE)]
      (set! child-size (protocols/-measure child ctx child-cs))
      (core/ipoint
        (:width child-size)
        (min
          (:height child-size)
          (:height cs)))))
  
  (-draw-impl [_ ctx bounds canvas]
    (set! child-size (protocols/-measure child ctx (core/ipoint (:width bounds) Integer/MAX_VALUE)))
    (set! offset-px (core/clamp (scaled (or @offset 0)) 0 (- (:height child-size) (:height bounds))))
    (canvas/with-canvas canvas
      (canvas/clip-rect canvas bounds)
      (let [child-bounds (-> bounds
                         (update :y - offset-px)
                         (assoc :height (:height child-size)))]
        (draw child ctx child-bounds canvas))))
  
  (-event-impl [_ ctx event]
    (case (:event event)
      :mouse-scroll
      (when (core/rect-contains? bounds (core/ipoint (:x event) (:y event)))
        (or
          (event-child child ctx event)
          (let [offset-px' (-> offset-px
                             (- (:delta-y event))
                             (core/clamp 0 (- (:height child-size) (:height bounds))))]
            (when (not= offset-px offset-px')
              (set! offset-px offset-px')
              (reset! offset (descaled offset-px'))
              (window/request-frame (:window ctx))))))
      
      :mouse-button
      (when (core/rect-contains? bounds (core/ipoint (:x event) (:y event)))
        (event-child child ctx event))
      
      #_:else
      (event-child child ctx event))))

(defn- vscrollable-ctor
  ([child]
   (vscrollable-ctor {} child))
  ([opts child]
   (map->VScrollable
     {:offset-px 0
      :offset    (or
                   (:offset opts)
                   (signal/signal 0))})))
