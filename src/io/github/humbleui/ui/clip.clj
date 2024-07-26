(in-ns 'io.github.humbleui.ui)

(util/deftype+ Clip [^:mut radii]
  :extends AWrapperNode

  (-draw-impl [_ ctx bounds viewport ^Canvas canvas]
    (canvas/with-canvas canvas
      (if radii
        (.clipRRect canvas (util/rrect-complex-xywh (:x bounds) (:y bounds) (:width bounds) (:height bounds) (map #(scaled % ctx) radii)) true)
        (canvas/clip-rect canvas bounds))
      (draw child ctx bounds (util/irect-intersect viewport bounds) canvas)))
  
  (-update-element [_this ctx new-element]
    (let [opts (parse-opts new-element)
          r    (get opts :radius)]
      (cond
        (nil? r)
        (set! radii nil)
        
        (number? r)
        (set! radii [r])
        
        (and (sequential? r) (every? number? r))
        (set! radii r)
        
        :else
        (throw (ex-info (str "Getting (:radius opts), expected: nil | number? | [number? ...], got: " (pr-str r)) {}))))))

(defn- clip-ctor
  ([child]
   (map->Clip {}))
  ([opts child]
   (map->Clip {})))
