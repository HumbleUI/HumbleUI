(in-ns 'io.github.humbleui.ui)

(core/deftype+ WithBounds [^:mut last-bounds]
  :extends AWrapperNode
  protocols/IComponent  
  (-child-elements [this ctx new-element]
    (let [[_ _ [child-ctor]] (parse-element new-element)
          scale  (:scale ctx)
          width  (/ (:width rect) scale)
          height (/ (:height rect) scale)]
      [(child-ctor (core/ipoint width height))]))
  
  (-draw-impl [this ctx rect canvas]
    (let [bounds (core/ipoint (:width rect) (:height rect))]
      (when (not= last-bounds bounds)
        (set! last-bounds bounds)
        (force-render this (:window ctx))) ;; TODO better way?
      (draw-child (:child this) ctx rect canvas))))

(defn with-bounds-ctor [child-ctor]
  (map->WithBounds {}))
