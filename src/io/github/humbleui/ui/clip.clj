(in-ns 'io.github.humbleui.ui)

(core/deftype+ Clip []
  :extends AWrapperNode
  protocols/IComponent
  (-draw-impl [_ ctx bounds ^Canvas canvas]
    (canvas/with-canvas canvas
      (canvas/clip-rect canvas bounds)
      (draw child ctx bounds canvas))))

(defn- clip-ctor [child]
  (map->Clip {}))

(core/deftype+ ClipRRect [radii]
  :extends AWrapperNode
  protocols/IComponent
  (-draw-impl [_ ctx bounds ^Canvas canvas]
    (let [{:keys [scale]} ctx
          [_ opts _] (parse-element element)
          radii      (:radii opts)
          rrect      (core/rrect-complex-xywh (:x bounds) (:y bounds) (:width bounds) (:height bounds) (map #(* scale %) radii))]
      (canvas/with-canvas canvas
        (.clipRRect canvas rrect true)
        (draw child ctx bounds canvas)))))

(defn- clip-rrect-ctor [opts child]
  (map->ClipRRect {}))
