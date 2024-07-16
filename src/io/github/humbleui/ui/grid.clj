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
  
  (-draw-impl [_ ctx bounds ^Canvas canvas]
    (let [{:keys [cols rows]}      (grid-opts element children)
          cs                       (util/ipoint (:width bounds) (:height bounds))
          {:keys [widths heights]} (grid-measure rows cols children cs ctx)]
      (util/loopr [x (:x bounds)
                   y (:y bounds)]
        [row (range rows)
         col (range cols)]
        (let [height (aget ^floats heights row)
              width  (aget ^floats widths col)]
          (when-some [child (nth children (+ col (* row cols)) nil)]
            (let [child-bounds (util/irect-xywh x y width height)]
              (draw-child child ctx child-bounds canvas)))
          (let [[x' y'] (if (< col (dec cols))
                          [(+ x width) y]
                          [(:x bounds) (+ y height)])]
            (recur x' y')))))))

(defn- grid-ctor
  ([opts]
   (grid-ctor opts []))
  ([opts & children]
   (map->Grid {})))
