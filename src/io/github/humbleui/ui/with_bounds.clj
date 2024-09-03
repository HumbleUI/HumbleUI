(in-ns 'io.github.humbleui.ui)

(util/deftype+ WithBounds []
  :extends AWrapperNode
  
  (-set-container-size-impl [this ctx container-size]
    (set! dirty? true))

  (-draw-impl [this ctx bounds container-size viewport canvas]
    (draw child ctx bounds container-size viewport canvas))
  
  (-child-elements [this ctx new-element]
    (let [[_ _ [child-ctor-or-el]] (parse-element new-element)
          scale  (:scale ctx)
          width  (/ (:width container-size 0) scale)
          height (/ (:height container-size 0) scale)]
      [[child-ctor-or-el (util/ipoint width height)]])))

(defn with-bounds-ctor [child-ctor-or-el]
  (map->WithBounds {}))
