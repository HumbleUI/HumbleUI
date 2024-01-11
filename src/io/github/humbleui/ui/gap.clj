(in-ns 'io.github.humbleui.ui)

(core/deftype+ Gap []
  :extends ATerminalNode
  protocols/IComponent
  (-measure-impl [_this ctx _cs]
    (let [[_ opts] element
          scale    (:scale ctx)]
      (core/ipoint
        (core/iceil (* scale (or (:width opts) 0)))
        (core/iceil (* scale (or (:height opts) 0))))))
  
  (-draw-impl [_this _ctx _rect _canvas])
  
  (-should-reconcile? [this new-el]
    true))

(defn gap [opts]
  (map->Gap {}))
