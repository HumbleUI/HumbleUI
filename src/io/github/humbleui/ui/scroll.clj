(in-ns 'io.github.humbleui.ui)

(core/deftype+ VScroll [^:mut offset 
                        ^:mut child-size]
  :extends AWrapperNode
  
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [child-cs   (assoc cs :height Integer/MAX_VALUE)]
      (set! child-size (protocols/-measure child ctx child-cs))
      (core/ipoint
        (:width child-size)
        (min
          (:height child-size)
          (:height cs)))))
  
  (-draw-impl [_ ctx rect canvas]
    (set! child-size (protocols/-measure child ctx (core/ipoint (:width rect) Integer/MAX_VALUE)))
    (set! offset (core/clamp offset (- (:height rect) (:height child-size)) 0))
    (canvas/with-canvas canvas
      (canvas/clip-rect canvas rect)
      (let [child-rect (-> rect
                         (update :y + offset)
                         (assoc :height (:height child-size)))]
        (draw child ctx child-rect canvas))))
  
  (-event-impl [_ ctx event]
    (cond
      (= :mouse-scroll (:event event))
      (when (core/rect-contains? rect (core/ipoint (:x event) (:y event)))
        (or
          (event-child child ctx event)
          (let [offset' (-> offset
                          (+ (:delta-y event))
                          (core/clamp (- (:height rect) (:height child-size)) 0))]
            (when (not= offset offset')
              (set! offset offset')
              true))))
      
      (= :mouse-button (:event event))
      (when (core/rect-contains? rect (core/ipoint (:x event) (:y event)))
        (event-child child ctx event))
      
      :else
      (event-child child ctx event))))

(defn vscroll
  ([child]
   (vscroll {} child))
  ([opts child]
   (map->VScroll (assoc opts :offset 0))))

(core/deftype+ VScrollbar []
  :extends AWrapperNode
  
  protocols/IComponent
  (-draw-impl [_ ctx rect ^Canvas canvas]
    (draw-child child ctx rect canvas)
    (when (> (:height (:child-size child)) (:height rect))
      (let [{:keys [scale]} ctx
            [_ opts _]      (parse-element element)
            fill-track      (or (:fill-track opts) (:hui.scroll/fill-track ctx))
            fill-thumb      (or (:fill-thumb opts) (:hui.scroll/fill-thumb ctx))
            content-y       (- (:offset child))
            content-h       (:height (:child-size child))
            scroll-y        (:y rect)
            scroll-h        (:height rect)
            
            padding         (* 4 scale)
            track-w         (* 4 scale)
            track-x         (+ (:x rect) (:width rect) (- track-w) (- padding))
            track-y         (+ scroll-y padding)
            track-h         (- scroll-h (* 2 padding))
            track           (core/rrect-xywh track-x track-y track-w track-h (* 2 scale))
            
            thumb-w         (* 4 scale)
            min-thumb-h     (* 16 scale)
            thumb-y-ratio   (/ content-y content-h)
            thumb-y         (-> (* track-h thumb-y-ratio) (core/clamp 0 (- track-h min-thumb-h)) (+ track-y))
            thumb-b-ratio   (/ (+ content-y scroll-h) content-h)
            thumb-b         (-> (* track-h thumb-b-ratio) (core/clamp min-thumb-h track-h) (+ track-y))
            thumb           (core/rrect-ltrb track-x thumb-y (+ track-x thumb-w) thumb-b (* 2 scale))]
        (canvas/draw-rect canvas track fill-track)
        (canvas/draw-rect canvas thumb fill-thumb)))))

(defn- vscrollbar-impl
  ([child]
   (vscrollbar-impl {} child))
  ([opts child]
   (map->VScrollbar {})))

(defn vscrollbar
  ([child]
   (vscrollbar {} child))
  ([opts child]
   [vscrollbar-impl opts [vscroll child]]))
