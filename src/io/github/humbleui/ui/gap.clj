(in-ns 'io.github.humbleui.ui)

(util/deftype+ Gap [^:mut width
                    ^:mut height]
  :extends ATerminalNode
  
  (-measure-impl [_this ctx cs]
    (util/ipoint
      (dimension width cs ctx)
      (dimension height cs ctx)))
  
  (-draw-impl [_this _ctx _bounds _viewport _canvas])
  
  (-update-element [_this _ctx new-element]
    (let [[_ opts] new-element]
      (set! width (or (util/checked-get-optional opts :width dimension?) 0))
      (set! height (or (util/checked-get-optional opts :height dimension?) 0)))))

(defn- gap-ctor
  ([]
   (gap-ctor {}))
  ([opts]
   (map->Gap {})))
