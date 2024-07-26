(in-ns 'io.github.humbleui.ui)

(util/deftype+ Size [^:mut width
                     ^:mut height]
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
  
  (-update-element [_this _ctx new-element]
    (let [opts (parse-opts new-element)]
      (set! width  (util/checked-get-optional opts :width dimension?))
      (set! height (util/checked-get-optional opts :height dimension?)))))

(defn size
  ([opts]
   (size opts nil))
  ([opts child]
   (map->Size {})))
