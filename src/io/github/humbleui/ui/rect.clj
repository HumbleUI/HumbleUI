(in-ns 'io.github.humbleui.ui)

(core/deftype+ RectNode []
  :extends AWrapperNode
  protocols/IComponent
  (-draw-impl [_ ctx bounds canvas]
    (let [opts  (parse-opts element)
          paint (core/checked-get opts :paint #(instance? Paint %))
          radii (some->>
                  (core/checked-get opts :radius #(or
                                                    (nil? %)
                                                    (number? %) 
                                                    (and (sequential? %) (every? number? %))))
                  (#(if (sequential? %) % [%]))
                  (map #(scaled % ctx)))]
      (if radii
        (canvas/draw-rect canvas (core/rrect-complex-xywh (:x bounds) (:y bounds) (:width bounds) (:height bounds) radii) paint)
        (canvas/draw-rect canvas bounds paint))
      (draw-child child ctx bounds canvas))))

(defn- rect-ctor [opts child]
  (map->RectNode {}))
