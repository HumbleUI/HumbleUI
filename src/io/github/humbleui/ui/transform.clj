(in-ns 'io.github.humbleui.ui)

(util/deftype+ Translate [dx
                          dy]
  :extends AWrapperNode 

  (-draw-impl [_ ctx bounds container-size viewport ^Canvas canvas]
    (let [dx-px        (dimension dx bounds ctx)
          dy-px        (dimension dy bounds ctx)
          child-bounds (util/irect-xywh
                         (+ (:x bounds) dx-px)
                         (+ (:y bounds) dy-px)
                         (:width bounds)
                         (:height bounds))]
      (draw child ctx child-bounds container-size viewport canvas)))
  
  (-reconcile-opts [_this _ctx new-element]
    (let [opts (parse-opts new-element)]
      (set! dx (or (util/checked-get-optional opts :dx dimension?) 0))
      (set! dy (or (util/checked-get-optional opts :dy dimension?) 0)))))

(defn- translate-ctor [opts child]
  (map->Translate {}))
