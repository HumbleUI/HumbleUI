(in-ns 'io.github.humbleui.ui)

(util/deftype+ Padding []
  :extends AWrapperNode  
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[_ opts _] (parse-element element)
          left       (dimension (or (:left opts)   (:horizontal opts) (:padding opts) 0) cs ctx)
          right      (dimension (or (:right opts)  (:horizontal opts) (:padding opts) 0) cs ctx)
          top        (dimension (or (:top opts)    (:vertical opts)   (:padding opts) 0) cs ctx)
          bottom     (dimension (or (:bottom opts) (:vertical opts)   (:padding opts) 0) cs ctx)
          child-cs   (util/ipoint
                       (-> (:width cs) (- left) (- right) (max 0))
                       (-> (:height cs) (- top) (- bottom) (max 0)))
          child-size (measure child ctx child-cs)]
      (util/ipoint
        (-> (:width child-size) (+ left) (+ right))
        (-> (:height child-size) (+ top) (+ bottom)))))
  
  (-draw-impl [_ ctx bounds viewport ^Canvas canvas]
    (let [[_ opts _]   (parse-element element)
          left         (dimension (or (:left opts)   (:horizontal opts) (:padding opts) 0) bounds ctx)
          right        (dimension (or (:right opts)  (:horizontal opts) (:padding opts) 0) bounds ctx)
          top          (dimension (or (:top opts)    (:vertical opts)   (:padding opts) 0) bounds ctx)
          bottom       (dimension (or (:bottom opts) (:vertical opts)   (:padding opts) 0) bounds ctx)
          width        (-> (:width bounds) (- left) (- right) (max 0))
          height       (-> (:height bounds) (- top) (- bottom) (max 0))
          child-bounds (util/irect-xywh
                         (+ (:x bounds) left)
                         (+ (:y bounds) top)
                         width
                         height)]
      (draw child ctx child-bounds viewport canvas))))

(defn- padding-ctor [opts child]
  (map->Padding {}))
