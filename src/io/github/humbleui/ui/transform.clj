(in-ns 'io.github.humbleui.ui)

(core/deftype+ Translate []
  :extends AWrapperNode 
  protocols/IComponent  
  (-draw-impl [_ ctx bounds ^Canvas canvas]
    (let [[_ opts _] (parse-element element)
          dx         (dimension (or (:dx opts) 0) bounds ctx)
          dy         (dimension (or (:dy opts) 0) bounds ctx)
          child-bounds (core/irect-xywh
                       (+ (:x bounds) dx)
                       (+ (:y bounds) dy)
                       (:width bounds)
                       (:height bounds))]
      (draw-child child ctx child-bounds canvas))))

(defn- translate-ctor [opts child]
  (map->Translate {}))
