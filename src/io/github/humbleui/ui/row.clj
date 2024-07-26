(in-ns 'io.github.humbleui.ui)

(util/deftype+ Row [^:mut gap]
  :extends AContainerNode
    
  (-measure-impl [_ ctx cs]
    (let [gap-px (scaled gap)]
      (util/loopr [width  0
                   height 0]
        [child children]
        (let [child-size (measure child ctx cs)]
          (recur
            (if (= 0 width)
              (+ width (:width child-size))
              (+ width gap-px (:width child-size)))
            (max height (:height child-size))))
        (util/ipoint width height))))
  
  (-draw-impl [_ ctx bounds viewport ^Canvas canvas]
    (let [gap-px        (scaled gap)
          cs            (util/ipoint (:width bounds) (:height bounds))
          known         (for [child children]
                          (let [meta (meta (:element child))]
                            (when (= :hug (:stretch meta :hug))
                              (measure child ctx cs))))
          space         (-> (:width bounds)
                          (- (transduce (keep :width) + 0 known))
                          (- (* gap-px (dec (count children))))
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
            (when (util/irect-intersect child-bounds viewport)
              (draw child ctx child-bounds viewport canvas))
            (when (<= (:x child-bounds) (:right viewport))
              (recur known' children' (+ width gap-px child-width))))))))
  
  (-update-element [_this ctx new-element]
    (let [opts (parse-opts new-element)]
      (set! gap (or (util/checked-get-optional opts :gap number?) 0)))))

(defn- row-ctor [& children]
  (map->Row {}))
