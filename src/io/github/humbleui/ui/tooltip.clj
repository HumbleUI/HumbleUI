(in-ns 'io.github.humbleui.ui)

(core/deftype+ RelativeRect [^:mut relative]
  :extends AWrapperNode
  
  protocols/IComponent
  (-draw-impl [_ ctx rect ^Canvas canvas]
    (let [[_ opts _]    (parse-element element)
          {:keys [left up anchor shackle]
           :or {left    0 
                up      0
                anchor  :top-left
                shackle :top-right}} opts
          child-size    (measure child ctx (core/ipoint (:width rect) (:height rect)))
          child-rect    (core/irect-xywh (:x rect) (:y rect) (:width child-size) (:height child-size))
          rel-cs        (measure relative ctx (core/ipoint 0 0))
          rel-cs-width  (:width rel-cs)
          rel-cs-height (:height rel-cs)
          rel-rect      (condp = [anchor shackle]
                          [:top-left :top-left]         (core/irect-xywh (- (:x child-rect) left) (- (:y child-rect) up) rel-cs-width rel-cs-height)
                          [:top-right :top-left]        (core/irect-xywh (- (:x child-rect) rel-cs-width left) (- (:y child-rect) up) rel-cs-width rel-cs-height)
                          [:bottom-right :top-left]     (core/irect-xywh (- (:x child-rect) rel-cs-width left) (- (:y child-rect) rel-cs-height up) rel-cs-width rel-cs-height)
                          [:bottom-left :top-left]      (core/irect-xywh (- (:x child-rect) left) (- (:y child-rect) rel-cs-height up) rel-cs-width rel-cs-height)
                          [:top-left :top-right]        (core/irect-xywh (+ (:x child-rect) (- (:width child-rect) left)) (- (:y child-rect) up) rel-cs-width rel-cs-height)
                          [:top-right :top-right]       (core/irect-xywh (+ (:x child-rect) (- (:width child-rect) rel-cs-width left)) (- (:y child-rect) up) rel-cs-width rel-cs-height)
                          [:bottom-left :top-right]     (core/irect-xywh (+ (:x child-rect) (- (:width child-rect) left)) (- (:y child-rect) rel-cs-height up) rel-cs-width rel-cs-height)
                          [:bottom-right :top-right]    (core/irect-xywh (+ (:x child-rect) (- (:width child-rect) rel-cs-width left)) (- (:y child-rect) rel-cs-height up) rel-cs-width rel-cs-height)
                          [:top-left :bottom-right]     (core/irect-xywh (+ (:x child-rect) (- (:width child-rect) left)) (+ (:y child-rect) (- (:height child-rect) up)) rel-cs-width rel-cs-height)
                          [:top-right :bottom-right]    (core/irect-xywh (+ (:x child-rect) (- (:width child-rect) rel-cs-width left)) (+ (:y child-rect) (- (:height child-rect) up)) rel-cs-width rel-cs-height)
                          [:bottom-right :bottom-right] (core/irect-xywh (+ (:x child-rect) (- (:width child-rect) rel-cs-width left)) (+ (:y child-rect) (- (:height child-rect) rel-cs-height up)) rel-cs-width rel-cs-height)
                          [:bottom-left :bottom-right]  (core/irect-xywh (+ (:x child-rect) (- (:width child-rect) left)) (+ (:y child-rect) (- (:height child-rect) rel-cs-height up)) rel-cs-width rel-cs-height)
                          [:top-left :bottom-left]      (core/irect-xywh (- (:x child-rect) left) (+ (:y child-rect) (- (:height child-rect) up)) rel-cs-width rel-cs-height)
                          [:top-right :bottom-left]     (core/irect-xywh (- (:x child-rect) rel-cs-width left) (+ (:y child-rect) (- (:height child-rect) up)) rel-cs-width rel-cs-height)
                          [:bottom-left :bottom-left]   (core/irect-xywh (- (:x child-rect) left) (+ (:y child-rect) (- (:height child-rect) rel-cs-height up)) rel-cs-width rel-cs-height)
                          [:bottom-right :bottom-left]  (core/irect-xywh (- (:x child-rect) rel-cs-width left) (+ (:y child-rect) (- (:height child-rect) rel-cs-height up)) rel-cs-width rel-cs-height))]
      (draw-child child ctx child-rect canvas)
      (draw-child relative ctx rel-rect canvas))) ;; TODO draw in tooltip overlay
  
  (-reconcile-impl [this ctx el']
    (let [[_ opts [child-el]] (parse-element el')
          [relative']         (reconcile-many ctx [relative] [(:relative opts)])
          [child']            (reconcile-many ctx [child] [child-el])]
      (set! relative relative')
      (set! child child'))))

(defn relative-rect-ctor
  ([relative child]
   (relative-rect-ctor {} relative child))
  ([opts relative child]
   (map->RelativeRect {})))

(defn tooltip-ctor [opts child]
  [valign {:position 0}
   [halign {:position 0}
    [hoverable
     (fn [state]
       (let [opts (cond-> opts
                    true 
                    (clojure.set/rename-keys {:tip :relative})
                    
                    (not (:hovered state))
                    (assoc :relative [gap]))]
         [relative-rect-ctor opts child]))]]])
