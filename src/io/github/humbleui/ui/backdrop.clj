(in-ns 'io.github.humbleui.ui)

(import '[io.github.humbleui.skija ImageFilter SaveLayerRec])

(core/deftype+ Backdrop []
  :extends AWrapperNode
  protocols/IComponent
  (-draw-impl [_ ctx ^IRect rect ^Canvas canvas]
    (let [[_ opts _] (parse-element element)
          filter ^ImageFilter (:filter opts)]
      (canvas/with-canvas canvas
        (canvas/clip-rect canvas rect)
        (.saveLayer canvas (SaveLayerRec. (.toRect rect) nil filter)))
      (draw-child child ctx rect canvas))))

(defn- backdrop-ctor [opts child]
  (map->Backdrop {}))
