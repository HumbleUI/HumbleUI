(in-ns 'io.github.humbleui.ui)

(util/deftype+ RelativeRect [relative
                             left
                             up
                             anchor
                             shackle]
  :extends AWrapperNode
  
  protocols/IComponent
  (-draw-impl [_ ctx bounds container-size viewport ^Canvas canvas]
    (let [child-size    (measure child ctx container-size)
          child-bounds  (util/irect-xywh (:x bounds) (:y bounds) (:width child-size) (:height child-size))
          rel-cs        (measure relative ctx container-size)
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
      (draw child ctx child-bounds container-size viewport canvas)
      (draw relative ctx rel-bounds container-size viewport canvas))) ;; TODO draw in tooltip overlay
  
  (-reconcile-children [this ctx el']
    (let [[_ opts [child-el]] (parse-element el')
          [relative']         (reconcile-many ctx [relative] [(:relative opts)])
          [child']            (reconcile-many ctx [child] [child-el])]
      (set! relative relative')
      (util/set!! relative' :parent this)
      (set! child child')
      (util/set!! child' :parent this)))
  
  (-reconcile-opts [this ctx new-element]
    (let [[_ opts _] (parse-element element)]
      (set! left    (or (:left opts) 0))
      (set! up      (or (:up opts) 0))
      (set! anchor  (or (:anchor opts) :top-left))
      (set! shackle (or (:shackle opts) :top-right)))))

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
