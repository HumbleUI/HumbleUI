(in-ns 'io.github.humbleui.ui)

(util/deftype+ Clip []
  :extends AWrapperNode
  protocols/IComponent
  (-draw-impl [_ ctx bounds viewport ^Canvas canvas]
    (let [opts  (parse-opts element)
          radii (some->>
                  (util/checked-get opts :radius
                    #(or
                       (nil? %)
                       (number? %) 
                       (and (sequential? %) (every? number? %))))
                  (#(if (sequential? %) % [%]))
                  (map #(scaled % ctx)))]
      (canvas/with-canvas canvas
        (if radii
          (.clipRRect canvas (util/rrect-complex-xywh (:x bounds) (:y bounds) (:width bounds) (:height bounds) radii) true)
          (canvas/clip-rect canvas bounds))
        (draw child ctx bounds (util/irect-intersect viewport bounds) canvas)))))

(defn- clip-ctor
  ([child]
   (map->Clip {}))
  ([opts child]
   (map->Clip {})))
