(in-ns 'io.github.humbleui.ui)

(util/deftype+ Size [width
                     height]
  :extends AWrapperNode
  
  (-measure-impl [_ ctx cs]
    (let [width-px  (some-> width (dimension cs ctx))
          height-px (some-> height (dimension cs ctx))]
      (cond
        (and width-px height-px)
        (util/ipoint width-px height-px)
        
        (and width-px child)
        (assoc (measure child ctx (assoc cs :width width-px)) :width width-px)
        
        (and height-px child)
        (assoc (measure child ctx (assoc cs :height height-px)) :height height-px)
        
        width-px
        (util/ipoint width-px 0)
        
        height-px
        (util/ipoint 0 height-px)
        
        :else
        (util/ipoint 0 0))))
  
  (-draw-impl [this ctx bounds container-size viewport ^Canvas canvas]
    (let [width-px  (some-> width (dimension container-size ctx))
          height-px (some-> height (dimension container-size ctx))
          child-cs  (cond-> container-size
                      width-px  (assoc :width width-px)
                      height-px (assoc :height height-px))]
      (draw child ctx bounds child-cs viewport canvas)))
  
  (-reconcile-opts [this _ctx new-element]
    (let [opts    (parse-opts new-element)
          width'  (util/checked-get-optional opts :width dimension?)
          height' (util/checked-get-optional opts :height dimension?)]
      (when (or
              (not= width width')
              (not= height height'))
        (set! width width')
        (set! height height')
        (invalidate-size this)))))

(defn size
  ([]
   (map->Size {}))
  ([opts]
   (map->Size {}))
  ([opts child]
   (map->Size {})))
