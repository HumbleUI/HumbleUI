(in-ns 'io.github.humbleui.ui)

(core/deftype+ Translate []
  :extends AWrapperNode 
  protocols/IComponent  
  (-draw-impl [_ ctx rect ^Canvas canvas]
    (let [[_ opts _] (parse-element element)
          dx         (dimension (or (:dx opts) 0) rect ctx)
          dy         (dimension (or (:dy opts) 0) rect ctx)
          child-rect (core/irect-xywh
                       (+ (:x rect) dx)
                       (+ (:y rect) dy)
                       (:width rect)
                       (:height rect))]
      (draw-child child ctx child-rect canvas))))

(defn- translate-ctor [opts child]
  (map->Translate {}))
