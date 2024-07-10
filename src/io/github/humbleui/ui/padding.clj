(in-ns 'io.github.humbleui.ui)

(core/deftype+ Padding []
  :extends AWrapperNode  
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[_ opts _] (parse-element element)
          left       (dimension (or (:left opts)   (:horizontal opts) (:padding opts) 0) cs ctx)
          right      (dimension (or (:right opts)  (:horizontal opts) (:padding opts) 0) cs ctx)
          top        (dimension (or (:top opts)    (:vertical opts)   (:padding opts) 0) cs ctx)
          bottom     (dimension (or (:bottom opts) (:vertical opts)   (:padding opts) 0) cs ctx)
          child-cs   (core/ipoint
                       (-> (:width cs) (- left) (- right) (max 0))
                       (-> (:height cs) (- top) (- bottom) (max 0)))
          child-size (measure child ctx child-cs)]
      (core/ipoint
        (-> (:width child-size) (+ left) (+ right))
        (-> (:height child-size) (+ top) (+ bottom)))))
  
  (-draw-impl [_ ctx rect ^Canvas canvas]
    (let [[_ opts _] (parse-element element)
          left       (dimension (or (:left opts)   (:horizontal opts) (:padding opts) 0) rect ctx)
          right      (dimension (or (:right opts)  (:horizontal opts) (:padding opts) 0) rect ctx)
          top        (dimension (or (:top opts)    (:vertical opts)   (:padding opts) 0) rect ctx)
          bottom     (dimension (or (:bottom opts) (:vertical opts)   (:padding opts) 0) rect ctx)
          width      (-> (:width rect) (- left) (- right) (max 0))
          height     (-> (:height rect) (- top) (- bottom) (max 0))
          child-rect (core/irect-xywh
                       (+ (:x rect) left)
                       (+ (:y rect) top)
                       width
                       height)]
      (draw-child child ctx child-rect canvas))))

(defn- padding-ctor [opts child]
  (map->Padding {}))
