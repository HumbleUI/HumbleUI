(in-ns 'io.github.humbleui.ui)

(def *overlays
  (atom {}))

(util/deftype+ OverlayRoot []
  :extends AWrapperNode  
  (-draw-impl [this ctx bounds container-size viewport canvas]
    (draw child ctx bounds container-size viewport canvas)
    (doseq [[_ comp] @*overlays
            :let [child-bounds (:bounds comp)
                  child-size   (measure comp ctx container-size)
                  width        (:width child-size)
                  height       (:height child-size)
                  left         (min (:x child-bounds) (- (:right bounds) width))
                  top          (min (:y child-bounds) (- (:bottom bounds) height))
                  child-bounds' (util/irect-xywh left top width height)]]
      (draw comp ctx child-bounds' container-size viewport canvas)))
  
  (-event-impl [this ctx event]
    (or
      (reduce-kv
        (fn [_ _ comp]
          (when (ui/event comp ctx event)
            (reduced true)))
        false @*overlays)
      (ui/event (:child this) ctx event))))

(defn overlay-root [child]
  (map->OverlayRoot {})) 

(util/deftype+ Overlay []
  :extends AWrapperNode  
  (-measure-impl [_ ctx cs]
    (util/ipoint 0 0))
  
  (-draw-impl [this ctx bounds container-size viewport canvas]
    (util/set!! child :bounds bounds)
    (swap! *overlays assoc this child))
  
  (-unmount [this]
    (swap! *overlays dissoc this)))
    

(defn overlay [child]
  (map->Overlay {}))
