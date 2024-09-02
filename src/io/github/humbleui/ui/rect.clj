(in-ns 'io.github.humbleui.ui)

(util/deftype+ RectNode [^:mut paint
                         ^:mut radii]
  :extends AWrapperNode

  (-draw-impl [_ ctx bounds container-size viewport canvas]
    (if radii
      (canvas/draw-rect canvas (util/rrect-complex-xywh (:x bounds) (:y bounds) (:width bounds) (:height bounds) (map #(scaled % ctx) radii)) paint)
      (canvas/draw-rect canvas bounds paint))
    (draw child ctx bounds container-size viewport canvas))
  
  (-reconcile-opts [_this ctx new-element]
    (let [opts (parse-opts new-element)
          r    (get opts :radius)]
      (set! paint (util/checked-get opts :paint #(instance? Paint %)))
      (cond
        (nil? r)
        (set! radii nil)
        
        (number? r)
        (set! radii [r])
        
        (and (sequential? r) (every? number? r))
        (set! radii r)
        
        :else
        (throw (ex-info (str "Getting (:radius opts), expected: nil | number? | [number? ...], got: " (pr-str r)) {}))))))

(defn- rect-ctor [opts child]
  (map->RectNode {}))
