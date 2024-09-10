(in-ns 'io.github.humbleui.ui)

(defn- grid-maybe-measure [this cs ctx]
  (let [{:keys [cols rows col-gap row-gap children]} this
        col-gap-px  (scaled col-gap ctx)
        row-gap-px  (scaled row-gap ctx)
        widths      (float-array (count cols) 0)
        heights     (float-array (count rows) 0)
        indices     (for [row (range (count rows))
                          col (range (count cols))]
                      [row col])
        sizes       (loop [row      0
                           col      0
                           children children]
                      (when-not (empty? children)
                        (let [child    (first children)
                              col-span (-> child :element meta :col-span)
                              size     (measure child ctx cs)
                              _        (when-not col-span
                                         (aset-float widths col (max (aget widths  col) (:width size))))
                              _        (aset-float heights row (max (aget heights row) (:height size)))
                              col'     (+ col (or col-span 1))]
                          (if (>= col' (count cols))
                            (recur (inc row) 0 (next children))
                            (recur row (long col') (next children))))))
        hug-width   (reduce + 0 (for [i (range (count cols))
                                      :let [col (nth cols i)]
                                      :when (= :hug col)]
                                  (nth widths i)))
        col-stretch (->> cols
                      (remove #(= :hug %))
                      (map :stretch)
                      (reduce + 0))
        col-space   (-> (:width cs)
                      (- hug-width)
                      (- (* col-gap-px (dec (count cols))))
                      (max 0))
        _           (doseq [[i col] (util/zip (range) cols)
                            :when (not= :hug col)
                            :let [stretch (:stretch col)]]
                      (aset-float widths i (-> col-space (/ col-stretch) (* stretch) math/round)))
        
        hug-height   (reduce + 0 (for [i (range (count rows))
                                       :let [row (nth rows i)]
                                       :when (= :hug row)]
                                   (nth heights i)))
        row-stretch (->> rows
                      (remove #(= :hug %))
                      (map :stretch)
                      (reduce + 0))
        row-space   (-> (:height cs)
                      (- hug-height)
                      (- (* row-gap-px (dec (count rows))))
                      (max 0))
        _           (doseq [[i row] (util/zip (range) rows)
                            :when (not= :hug row)
                            :let [stretch (:stretch row)]]
                      (aset-float heights i (-> row-space (/ row-stretch) (* stretch) math/round)))]
    (util/set!! this
      :widths widths
      :heights heights)))

(util/deftype+ Grid [cols
                     rows
                     col-gap
                     row-gap
                     widths
                     heights]
  :extends AContainerNode
  
  (-measure-impl [this ctx cs]
    (grid-maybe-measure this cs ctx)
    (util/ipoint
      (+
        (areduce ^floats widths  i res (float 0) (+ res (aget ^floats widths i)))
        (* col-gap (dec (count cols))))
      (+
        (areduce ^floats heights i res (float 0) (+ res (aget ^floats heights i)))
        (* row-gap (dec (count rows))))))
  
  (-draw-impl [this ctx bounds container-size viewport ^Canvas canvas]
    (when-not this-size
      (grid-maybe-measure this container-size ctx))
    (let [cols-count (count cols)
          rows-count (count rows)
          col-gap-px (scaled col-gap ctx)
          row-gap-px (scaled row-gap ctx)]
      (loop [x         (:x bounds)
             y         (:y bounds)
             row       0
             col       0
             child-idx 0]
        (let [child    (nth children child-idx nil)
              col-span (-> child :element meta :col-span (or 1))
              height   (aget ^floats heights row)
              width    (+
                         (reduce + 0
                           (map #(aget ^floats widths %) (range col (+ col col-span))))
                         (* col-gap-px (dec col-span)))
              col'     (+ col col-span)]
          (when child
            (let [child-bounds (util/irect-xywh x y width height)]
              (when (util/irect-intersect child-bounds viewport)
                (draw child ctx child-bounds container-size viewport canvas))))
          (cond
            (and (>= col' cols-count) (>= row (dec rows-count)))
            :done
            
            (and (>= col' cols-count) (> (+ y height) (:bottom viewport)))
            :done ;; skip rest of cols
            
            (>= col' cols-count)
            (recur (:x bounds) (+ y (+ height row-gap-px)) (inc row) 0 (inc child-idx))
            
            (>= (+ x width) (:right viewport)) ;; skip rest of current row
            (recur (:x bounds) (+ y (+ height row-gap-px)) (inc row) 0 (inc child-idx))
            
            :else
            (recur (+ x (+ width col-gap-px)) y row (long col') (inc child-idx)))))))
  
  (-reconcile-opts [this ctx new-element]
    (let [opts           (parse-opts new-element)
          cols'          (:cols opts)
          cols'          (cond
                           (int? cols')        (repeat cols' :hug)
                           (sequential? cols') cols'
                           :else               (throw (ex-info (str "Expected :cols to be int? or sequential?, got: " cols') {:cols cols'})))
          children-count (->> children
                           (map #(-> % :element meta :col-span (or 1)))
                           (reduce + 0))
          rows-count     (-> children-count dec (quot (count cols')) inc)
          rows'          (:rows opts)
          rows'          (cond
                           (int? rows')        (repeat rows' :hug)
                           (sequential? rows') rows'
                           (nil? rows)         (repeat rows-count :hug)
                           :else               (throw (ex-info (str "Expected :rows to be int? or sequential?, got: " rows') {:rows rows'})))
          rows'          (if (< (* (count rows') (count cols')) children-count)
                           (concat rows' (repeat (- rows-count (count rows')) :hug))
                           rows')
          row-gap'       (or (:row-gap opts) (:gap opts) 0)
          col-gap'       (or (:col-gap opts) (:gap opts) 0)]
      (when (or
              (not= cols cols')
              (not= rows rows')
              (not= row-gap row-gap')
              (not= col-gap col-gap'))
        (set! cols (vec cols'))
        (set! rows (vec rows'))
        (set! col-gap col-gap')
        (set! row-gap row-gap')
        (set! widths nil)
        (set! heights nil)
        (invalidate-size this)))))

(defn- grid-ctor
  ([opts]
   (grid-ctor opts []))
  ([opts & children]
   (map->Grid {})))
