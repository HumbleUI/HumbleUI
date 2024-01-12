(in-ns 'io.github.humbleui.ui)

(core/deftype+ RectNode []
  :extends AWrapperNode
  protocols/IComponent
  (-draw-impl [_ ctx rect canvas]
    (let [[_ opts _ ] (parse-element element)
          {:keys [paint]} opts]
      (canvas/draw-rect canvas rect paint)
      (draw-child child ctx rect canvas))))

(defn rect [opts child]
  (map->RectNode {}))

(core/deftype+ RoundedRect []
  :extends AWrapperNode
  protocols/IComponent
  (-draw-impl [_ ctx rect canvas]
    (let [[_ opts _ ] (parse-element element)
          {:keys [radius paint]} opts
          rrect (RRect/makeXYWH (:x rect) (:y rect) (:width rect) (:height rect) (* radius (:scale ctx)))]
      (canvas/draw-rect canvas rrect paint)
      (draw-child child ctx rect canvas))))

(defn rounded-rect [opts child]
  (map->RoundedRect {}))
