(in-ns 'io.github.humbleui.ui)

(defn- grid-measure [rows cols children cs ctx]
  (core/loopr [heights (vec (repeat rows 0))
               widths  (vec (repeat cols 0))
               row     0
               col     0]
    [child children]
    (let [[row' col'] (if (< col (dec cols))
                        [row (inc col)]
                        [(inc row) 0])]
      (if child
        (let [size (measure child ctx cs)]
          (recur
            (update heights row max (:height size))
            (update widths  col max (:width size))
            row' col'))
        (recur heights widths row' col')))
    {:widths  widths
     :heights heights}))

(defn- grid-opts [element children]
  (let [[_ opts _] (parse-element element)
        cols       (core/checked-get opts :cols (every-pred integer? pos?))
        rows       (or
                     (:rows opts)
                     (-> (count children) dec (quot cols) inc))
        children   (concat
                     children
                     (repeat (- (* rows cols) (count children)) nil))]
    {:cols     cols
     :rows     rows
     :children children}))

(core/deftype+ Grid []
  :extends AContainerNode
  
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [{:keys [cols rows children]} (grid-opts element children)
          {:keys [widths heights]}     (grid-measure rows cols children cs ctx)]
      (core/ipoint
        (reduce + 0 widths)
        (reduce + 0 heights))))
  
  (-draw-impl [_ ctx rect ^Canvas canvas]
    (let [{:keys [cols rows children]} (grid-opts element children)
          cs                           (core/ipoint (:width rect) (:height rect))
          {:keys [widths heights]}     (grid-measure rows cols children cs ctx)]
      (core/loopr [x (:x rect)
                   y (:y rect)]
        [row (range rows)
         col (range cols)]
        (let [height (nth heights row)
              width  (nth widths col)]
          (when-some [child (nth children (+ col (* row cols)))]
            (let [child-rect (core/irect-xywh x y width height)]
              (draw-child child ctx child-rect canvas)))
          (let [[x' y'] (if (< col (dec cols))
                          [(+ x width) y]
                          [(:x rect) (+ y height)])]
            (recur x' y')))))))

(defn- grid-ctor
  ([opts]
   (grid-ctor opts []))
  ([opts children]
   (map->Grid {})))
