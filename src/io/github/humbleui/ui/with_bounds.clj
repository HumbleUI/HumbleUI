(in-ns 'io.github.humbleui.ui)

(util/deftype+ WithBounds [^:mut cs]
  :extends AWrapperNode

  (-draw-impl [this ctx bounds container-size viewport canvas]
    (let [cs' (util/irect-size bounds)]
      (when (not= cs cs')
        (set! cs cs')
        (force-render this (:window ctx))) ;; TODO better way?
      (draw child ctx bounds container-size viewport canvas)))
  
  (-child-elements [this ctx new-element]
    (let [[_ _ [child-ctor-or-el]] (parse-element new-element)
          scale  (:scale ctx)
          width  (/ (:width bounds 0) scale)
          height (/ (:height bounds 0) scale)]
      [[child-ctor-or-el (util/ipoint width height)]])))

(defn with-bounds-ctor [child-ctor-or-el]
  (map->WithBounds {}))
