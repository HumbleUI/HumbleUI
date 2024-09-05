(in-ns 'io.github.humbleui.ui)

(defn- grid-maybe-measure [this cs ctx]
  (let [widths  (float-array (:cols this) 0)
        heights (float-array (:rows this) 0)
        max-col (dec (:cols this))]
    (util/loopr [row 0
                 col 0]
      [child (:children this)]
      (let [row' (if (< col max-col) row (inc row))
            col' (if (< col max-col) (inc col) 0)]
        (if child
          (let [size (measure child ctx cs)]
            (aset-float widths  col (max (aget widths  col) (:width size)))
            (aset-float heights row (max (aget heights row) (:height size)))
            (recur row' col'))
          (recur row' col'))))
    (util/set!! this
      :widths widths
      :heights heights)))

(util/deftype+ Grid [cols
                     rows
                     widths
                     heights]
  :extends AContainerNode
  
  (-measure-impl [this ctx cs]
    (grid-maybe-measure this cs ctx)
    (util/ipoint
      (areduce ^floats widths  i res (float 0) (+ res (aget ^floats widths i)))
      (areduce ^floats heights i res (float 0) (+ res (aget ^floats heights i)))))
  
  (-draw-impl [this ctx bounds container-size viewport ^Canvas canvas]
    (when-not this-size
      (grid-maybe-measure this container-size ctx))
    (loop [x   (:x bounds)
           y   (:y bounds)
           row 0
           col 0]
      (let [height (aget ^floats heights row)
            width  (aget ^floats widths col)]
        (when-some [child (nth children (+ col (* row cols)) nil)]
          (let [child-bounds (util/irect-xywh x y width height)]
            (when (util/irect-intersect child-bounds viewport)
              (draw child ctx child-bounds container-size viewport canvas))))
        (cond
          (and (>= col (dec cols)) (>= row (dec rows)))
          :done
            
          (and (>= col (dec cols)) (> (+ y height) (:bottom viewport)))
          :done ;; skip rest of cols
            
          (>= col (dec cols))
          (recur (:x bounds) (+ y height) (inc row) 0)
            
          (>= (+ x width) (:right viewport)) ;; skip rest of current row
          (recur (:x bounds) (+ y height) (inc row) 0)
            
          :else
          (recur (+ x width) y row (inc col))))))
  
  (-reconcile-opts [this ctx new-element]
    (let [opts  (parse-opts new-element)
          cols' (util/checked-get opts :cols pos-int?)
          rows' (or
                  (util/checked-get-optional opts :rows pos-int?)
                  (-> (count children) dec (quot cols') inc))]
      (when (or
              (not= cols cols')
              (not= rows rows'))
        (set! cols cols')
        (set! rows rows')
        (set! widths nil)
        (set! heights nil)
        (invalidate-size this)))))

(defn- grid-ctor
  ([opts]
   (grid-ctor opts []))
  ([opts & children]
   (map->Grid {})))
