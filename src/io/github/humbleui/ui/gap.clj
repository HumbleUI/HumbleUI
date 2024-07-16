(in-ns 'io.github.humbleui.ui)

(util/deftype+ Gap []
  :extends ATerminalNode
  protocols/IComponent
  (-measure-impl [_this ctx _cs]
    (let [[_ opts] element
          scale    (:scale ctx)]
      (util/ipoint
        (util/iceil (* scale (or (:width opts) 0)))
        (util/iceil (* scale (or (:height opts) 0))))))
  
  (-draw-impl [_this _ctx _bounds _viewport _canvas]))

(defn- gap-ctor
  ([]
   (gap-ctor {}))
  ([opts]
   (map->Gap {})))
