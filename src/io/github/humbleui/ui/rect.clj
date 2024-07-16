(in-ns 'io.github.humbleui.ui)

(util/deftype+ RectNode []
  :extends AWrapperNode
  protocols/IComponent
  (-draw-impl [_ ctx bounds canvas]
    (let [opts  (parse-opts element)
          paint (util/checked-get opts :paint #(instance? Paint %))
          radii (some->>
                  (util/checked-get opts :radius #(or
                                                    (nil? %)
                                                    (number? %) 
                                                    (and (sequential? %) (every? number? %))))
                  (#(if (sequential? %) % [%]))
                  (map #(scaled % ctx)))]
      (if radii
        (canvas/draw-rect canvas (util/rrect-complex-xywh (:x bounds) (:y bounds) (:width bounds) (:height bounds) radii) paint)
        (canvas/draw-rect canvas bounds paint))
      (draw child ctx bounds canvas))))

(defn- rect-ctor [opts child]
  (map->RectNode {}))
