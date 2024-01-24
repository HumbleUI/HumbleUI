(in-ns 'io.github.humbleui.ui)

(core/deftype+ HAlign []
  :extends AWrapperNode
  
  protocols/IComponent  
  (-draw-impl [_ ctx rect canvas]
    (let [[_ opts _]     element
          position       (:position opts)
          child-position (or (:child-position opts) position)
          child-size     (measure child ctx (core/ipoint (:width rect) (:height rect)))
          left           (+ (:x rect)
                           (* (:width rect) position)
                           (- (* (:width child-size) child-position)))
          child-rect     (core/irect-xywh left (:y rect) (:width child-size) (:height rect))]
      (draw-child child ctx child-rect canvas))))

(defn- halign-ctor [opts child]
  (map->HAlign {}))

(core/deftype+ VAlign [child-position position]
  :extends AWrapperNode
  
  protocols/IComponent  
  (-draw-impl [_ ctx rect canvas]
    (let [[_ opts _]     element
          position       (:position opts)
          child-position (or (:child-position opts) position)
          child-size     (measure child ctx (core/ipoint (:width rect) (:height rect)))
          top            (+ (:y rect)
                           (* (:height rect) position)
                           (- (* (:height child-size) child-position)))
          child-rect     (core/irect-xywh (:x rect) top (:width rect) (:height child-size))]
      (draw-child child ctx child-rect canvas))))

(defn- valign-ctor [opts child]
  (map->VAlign {}))

(defn- center-ctor [child]
  [halign {:position 0.5}
   [valign {:position 0.5}
    child]])
