(in-ns 'io.github.humbleui.ui)

(util/deftype+ RelativeRect [^:mut relative]
  :extends AWrapperNode
  
  protocols/IComponent
  (-draw-impl [_ ctx bounds viewport ^Canvas canvas]
    (let [[_ opts _]    (parse-element element)
          {:keys [left up anchor shackle]
           :or {left    0 
                up      0
                anchor  :top-left
                shackle :top-right}} opts
          child-size    (measure child ctx (util/ipoint (:width bounds) (:height bounds)))
          child-bounds    (util/irect-xywh (:x bounds) (:y bounds) (:width child-size) (:height child-size))
          rel-cs        (measure relative ctx (util/ipoint 0 0))
          rel-cs-width  (:width rel-cs)
          rel-cs-height (:height rel-cs)
          rel-bounds      (condp = [anchor shackle]
                            [:top-left :top-left]         (util/irect-xywh (- (:x child-bounds) left) (- (:y child-bounds) up) rel-cs-width rel-cs-height)
                            [:top-right :top-left]        (util/irect-xywh (- (:x child-bounds) rel-cs-width left) (- (:y child-bounds) up) rel-cs-width rel-cs-height)
                            [:bottom-right :top-left]     (util/irect-xywh (- (:x child-bounds) rel-cs-width left) (- (:y child-bounds) rel-cs-height up) rel-cs-width rel-cs-height)
                            [:bottom-left :top-left]      (util/irect-xywh (- (:x child-bounds) left) (- (:y child-bounds) rel-cs-height up) rel-cs-width rel-cs-height)
                            [:top-left :top-right]        (util/irect-xywh (+ (:x child-bounds) (- (:width child-bounds) left)) (- (:y child-bounds) up) rel-cs-width rel-cs-height)
                            [:top-right :top-right]       (util/irect-xywh (+ (:x child-bounds) (- (:width child-bounds) rel-cs-width left)) (- (:y child-bounds) up) rel-cs-width rel-cs-height)
                            [:bottom-left :top-right]     (util/irect-xywh (+ (:x child-bounds) (- (:width child-bounds) left)) (- (:y child-bounds) rel-cs-height up) rel-cs-width rel-cs-height)
                            [:bottom-right :top-right]    (util/irect-xywh (+ (:x child-bounds) (- (:width child-bounds) rel-cs-width left)) (- (:y child-bounds) rel-cs-height up) rel-cs-width rel-cs-height)
                            [:top-left :bottom-right]     (util/irect-xywh (+ (:x child-bounds) (- (:width child-bounds) left)) (+ (:y child-bounds) (- (:height child-bounds) up)) rel-cs-width rel-cs-height)
                            [:top-right :bottom-right]    (util/irect-xywh (+ (:x child-bounds) (- (:width child-bounds) rel-cs-width left)) (+ (:y child-bounds) (- (:height child-bounds) up)) rel-cs-width rel-cs-height)
                            [:bottom-right :bottom-right] (util/irect-xywh (+ (:x child-bounds) (- (:width child-bounds) rel-cs-width left)) (+ (:y child-bounds) (- (:height child-bounds) rel-cs-height up)) rel-cs-width rel-cs-height)
                            [:bottom-left :bottom-right]  (util/irect-xywh (+ (:x child-bounds) (- (:width child-bounds) left)) (+ (:y child-bounds) (- (:height child-bounds) rel-cs-height up)) rel-cs-width rel-cs-height)
                            [:top-left :bottom-left]      (util/irect-xywh (- (:x child-bounds) left) (+ (:y child-bounds) (- (:height child-bounds) up)) rel-cs-width rel-cs-height)
                            [:top-right :bottom-left]     (util/irect-xywh (- (:x child-bounds) rel-cs-width left) (+ (:y child-bounds) (- (:height child-bounds) up)) rel-cs-width rel-cs-height)
                            [:bottom-left :bottom-left]   (util/irect-xywh (- (:x child-bounds) left) (+ (:y child-bounds) (- (:height child-bounds) rel-cs-height up)) rel-cs-width rel-cs-height)
                            [:bottom-right :bottom-left]  (util/irect-xywh (- (:x child-bounds) rel-cs-width left) (+ (:y child-bounds) (- (:height child-bounds) rel-cs-height up)) rel-cs-width rel-cs-height))]
      (draw child ctx child-bounds viewport canvas)
      (draw relative ctx rel-bounds viewport canvas))) ;; TODO draw in tooltip overlay
  
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
  [align {:x :left :y :top}
   [hoverable
    (fn [state]
      (let [opts (cond-> opts
                   true 
                   (clojure.set/rename-keys {:tip :relative})
                    
                   (not (:hovered state))
                   (assoc :relative [gap]))]
        [relative-rect-ctor opts child]))]])
