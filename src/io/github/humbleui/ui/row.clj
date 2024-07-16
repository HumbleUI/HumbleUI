(in-ns 'io.github.humbleui.ui)

(util/deftype+ Row []
  :extends AContainerNode
    
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[_ opts _] (parse-element element)
          gap        (-> (:gap opts 0)
                       (* (:scale ctx))
                       (util/iceil))]
      (util/loopr [width  0
                   height 0]
        [child children]
        (let [child-size (measure child ctx cs)]
          (recur
            (if (= 0 width)
              (+ width (:width child-size))
              (+ width gap (:width child-size)))
            (max height (:height child-size))))
        (util/ipoint width height))))
  
  (-draw-impl [_ ctx bounds ^Canvas canvas]
    (let [[_ opts _]    (parse-element element)
          gap           (-> (:gap opts 0)
                          (* (:scale ctx))
                          (util/iceil))
          cs            (util/ipoint (:width bounds) (:height bounds))
          known         (for [child children]
                          (let [meta (meta (:element child))]
                            (when (= :hug (:stretch meta :hug))
                              (measure child ctx cs))))
          space         (-> (:width bounds)
                          (- (transduce (keep :width) + 0 known))
                          (- (* gap (dec (count children))))
                          (max 0))
          total-stretch (transduce (keep #(:stretch (meta (:element %)))) + 0 children)]
      (loop [known    known
             children children
             width    0]
        (when-not (empty? children)
          (let [[size & known']     known
                [child & children'] children
                child-width         (long
                                      (or
                                        (:width size)
                                        (let [stretch (:stretch (meta (:element child)))]
                                          (-> space (/ total-stretch) (* stretch) (math/round)))))
                child-bounds        (util/irect-xywh
                                      (+ (:x bounds) width)
                                      (:y bounds)
                                      (max 0 child-width)
                                      (max 0 (:height bounds)))]
            (draw child ctx child-bounds canvas)
            (recur known' children' (+ width gap child-width))))))))

(defn- row-ctor [& children]
  (map->Row {}))
