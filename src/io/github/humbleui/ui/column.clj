(in-ns 'io.github.humbleui.ui)

(util/deftype+ Column []
  :extends AContainerNode
    
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[_ opts _] (parse-element element)
          gap        (-> (:gap opts 0)
                       (* (:scale ctx))
                       (util/iceil))]
      (util/loopr
        [width  0
         height 0]
        [child children]
        (let [child-size (measure child ctx cs)]
          (recur
            (max width (:width child-size))
            (if (= 0 height)
              (+ height (:height child-size))
              (+ height gap (:height child-size)))))
        (util/ipoint width height))))
  
  (-draw-impl [_ ctx bounds viewport ^Canvas canvas]
    (let [[_ opts _]    (parse-element element)
          gap           (-> (:gap opts 0)
                          (* (:scale ctx))
                          (util/iceil))
          cs            (util/ipoint (:width bounds) (:height bounds))
          known         (for [child children]
                          (let [meta (meta (:element child))]
                            (when (= :hug (:stretch meta :hug))
                              (measure child ctx cs))))
          space         (-> (:height bounds)
                          (- (transduce (keep :height) + 0 known))
                          (- (* gap (dec (count children))))
                          (max 0))
          total-stretch (transduce (keep #(:stretch (meta (:element %)))) + 0 children)]
      (loop [known    known
             children children
             height   0]
        (when-not (empty? children)
          (let [[size & known']     known
                [child & children'] children
                child-height        (long
                                      (or
                                        (:height size)
                                        (let [stretch (:stretch (meta (:element child)))]
                                          (-> space (/ total-stretch) (* stretch) (math/round)))))
                child-bounds        (util/irect-xywh
                                      (:x bounds)
                                      (+ (:y bounds) height)
                                      (max 0 (:width bounds))
                                      (max 0 child-height))]
            (when (util/irect-intersect child-bounds viewport)
              (draw child ctx child-bounds viewport canvas))
            (when (< (:y child-bounds) (:bottom viewport))
              (recur known' children' (+ height gap child-height)))))))))

(defn- column-ctor [& children]
  (map->Column {}))

