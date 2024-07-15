(in-ns 'io.github.humbleui.ui)

(core/deftype+ RectNode []
  :extends AWrapperNode
  protocols/IComponent
  (-draw-impl [_ ctx bounds canvas]
    (let [[_ opts _ ] (parse-element element)
          paint       (core/checked-get opts :paint #(instance? Paint %))]
      (canvas/draw-rect canvas bounds paint)
      (draw-child child ctx bounds canvas))))

(defn- rect-ctor [opts child]
  (map->RectNode {}))

(core/deftype+ RoundedRect []
  :extends AWrapperNode
  protocols/IComponent
  (-draw-impl [_ ctx bounds canvas]
    (let [[_ opts _ ] (parse-element element)
          radius      (core/checked-get opts :radius number?)
          paint       (core/checked-get opts :paint #(instance? Paint %))          
          rrect       (RRect/makeXYWH (:x bounds) (:y bounds) (:width bounds) (:height bounds) (* radius (:scale ctx)))]
      (canvas/draw-rect canvas rrect paint)
      (draw-child child ctx bounds canvas))))

(defn- rounded-rect-ctor [opts child]
  (map->RoundedRect {}))
