(in-ns 'io.github.humbleui.ui)

(import '[io.github.humbleui.skija ImageFilter SaveLayerRec])

(util/deftype+ Backdrop []
  :extends AWrapperNode

  (-draw-impl [_ ctx ^IRect bounds viewport ^Canvas canvas]
    (let [opts (parse-opts element)
          filter ^ImageFilter (:filter opts)]
      (canvas/with-canvas canvas
        (canvas/clip-rect canvas bounds)
        (.saveLayer canvas (SaveLayerRec. (.toRect bounds) nil filter)))
      (draw child ctx bounds viewport canvas))))

(defn- backdrop-ctor [opts child]
  (map->Backdrop {}))
