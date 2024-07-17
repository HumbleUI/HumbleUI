(in-ns 'io.github.humbleui.ui)

(defn- grid-measure [rows cols children cs ctx]
  (let [heights (float-array rows 0)
        widths  (float-array cols 0)]
    (util/loopr [row 0
                 col 0]
      [child children]
      (let [row' (if (< col (dec cols)) row (inc row))
            col' (if (< col (dec cols)) (inc col) 0)]
        (if child
          (let [size (measure child ctx cs)]
            (aset-float heights row (max (aget heights row) (:height size)))
            (aset-float widths  col (max (aget widths  col) (:width size)))
            (recur row' col'))
          (recur row' col')))
      {:widths  widths
       :heights heights})))

(defn- grid-opts [element children]
  (let [[_ opts _] (parse-element element)
        cols       (util/checked-get opts :cols (every-pred integer? pos?))
        rows       (or
                     (:rows opts)
                     (-> (count children) dec (quot cols) inc))]
    {:cols cols
     :rows rows}))

(util/deftype+ Grid []
  :extends AContainerNode
  
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [{:keys [cols rows]}      (grid-opts element children)
          {:keys [widths heights]} (grid-measure rows cols children cs ctx)]
      (util/ipoint
        (areduce ^floats widths  i res (float 0) (+ res (aget ^floats widths i)))
        (areduce ^floats heights i res (float 0) (+ res (aget ^floats heights i))))))
  
  (-draw-impl [_ ctx bounds viewport ^Canvas canvas]
    (let [{:keys [cols rows]}      (grid-opts element children)
          cs                       (util/ipoint (:width bounds) (:height bounds))
          {:keys [widths heights]} (grid-measure rows cols children cs ctx)]
      (loop [x   (:x bounds)
             y   (:y bounds)
             row 0
             col 0]
        (let [height (aget ^floats heights row)
              width  (aget ^floats widths col)]
          (when-some [child (nth children (+ col (* row cols)) nil)]
            (let [child-bounds (util/irect-xywh x y width height)]
              (when (util/irect-intersect child-bounds viewport)
                (draw child ctx child-bounds viewport canvas))))
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
            (recur (+ x width) y row (inc col))))))))

(defn- grid-ctor
  ([opts]
   (grid-ctor opts []))
  ([opts & children]
   (map->Grid {})))
