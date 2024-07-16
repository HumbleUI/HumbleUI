(in-ns 'io.github.humbleui.ui)

(util/deftype+ Size []
  :extends AWrapperNode
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[_ opts _] (parse-element element)
          width      (some-> opts :width (dimension cs ctx))
          height     (some-> opts :height (dimension cs ctx))]
      (cond
        (and width height)
        (util/ipoint width height)
        
        (and width child)
        (assoc (measure child ctx (assoc cs :width width)) :width width)
        
        (and height child)
        (assoc (measure child ctx (assoc cs :height height)) :height height)
        
        width
        (util/ipoint width 0)
        
        height
        (util/ipoint 0 height)
        
        :else
        (util/ipoint 0 0)))))

(defn size
  ([opts]
   (size opts nil))
  ([opts child]
   (map->Size {})))
