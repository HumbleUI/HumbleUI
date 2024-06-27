(in-ns 'io.github.humbleui.ui)

(core/deftype+ RectNode []
  :extends AWrapperNode
  protocols/IComponent
  (-draw-impl [_ ctx rect canvas]
    (let [[_ opts _ ] (parse-element element)
          paint       (core/checked-get opts :paint #(instance? Paint %))]
      (canvas/draw-rect canvas rect paint)
      (draw-child child ctx rect canvas))))

(defn- rect-ctor [opts child]
  (map->RectNode {}))

(core/deftype+ RoundedRect []
  :extends AWrapperNode
  protocols/IComponent
  (-draw-impl [_ ctx rect canvas]
    (let [[_ opts _ ] (parse-element element)
          radius      (core/checked-get opts :radius (every-pred number? pos?))
          paint       (core/checked-get opts :paint #(instance? Paint %))          
          rrect       (RRect/makeXYWH (:x rect) (:y rect) (:width rect) (:height rect) (* radius (:scale ctx)))]
      (canvas/draw-rect canvas rrect paint)
      (draw-child child ctx rect canvas))))

(defn- rounded-rect-ctor [opts child]
  (map->RoundedRect {}))
