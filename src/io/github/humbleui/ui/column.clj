(in-ns 'io.github.humbleui.ui)

(defn- column-children-sizes [this ctx cs]
  (let [{:keys [children]} this
        children-sizes'    (mapv
                             (fn [child]
                               [child (-> child :element meta :stretch (or :hug)) (measure child ctx cs)])
                             children)
        width'             (transduce (map #(-> % (nth 2) :width))  max 0 children-sizes')
        height'            (transduce (map #(-> % (nth 2) :height)) +   0 children-sizes')
        hug-height'        (transduce
                             (comp
                               (filter #(-> % (nth 1) (= :hug)))
                               (map #(-> % (nth 2) :height)))
                             + 0 children-sizes')
        total-stretch'     (transduce
                             (comp
                               (filter #(-> % (nth 1) number?))
                               (map #(-> % (nth 1))))
                             + 0 children-sizes')]
    (util/set!! this
      :children-sizes children-sizes'
      :width          width'
      :height         height'
      :hug-height     hug-height'
      :total-stretch  total-stretch')))

(util/deftype+ Column [^:mut gap
                       ^:mut children-sizes
                       ^:mut width
                       ^:mut height
                       ^:mut hug-height
                       ^:mut total-stretch]
  :extends AContainerNode
  
  (-measure-impl [this ctx cs]
    (column-children-sizes this ctx cs)
    (let [gap-px (scaled gap)
          gaps   (-> children count dec (max 0))]
      (util/ipoint width (+ height (* gap-px gaps)))))
  
  (-draw-impl [this ctx bounds viewport ^Canvas canvas]
    (column-children-sizes this ctx (util/irect-size bounds))
    (let [gap-px (scaled gap)
          gaps   (-> children count dec (max 0))
          space  (-> (:height bounds)
                   (- hug-height)
                   (- (* gap-px gaps))
                   (max 0))]
      (util/loopr [y (:y bounds)]
        [[child stretch child-size] children-sizes]
        (let [child-height (if (= :hug stretch)
                             (:height child-size)
                             (-> space (/ total-stretch) (* stretch) math/round))
              child-bounds (util/irect-xywh (:x bounds) y (max 0 (:width bounds)) (max 0 child-height))]
          (draw child ctx child-bounds viewport canvas)
          (when (< y (:bottom viewport))
            (recur (long (+ y gap-px child-height))))))))
      
  (-update-element [_this ctx new-element]
    (let [opts (parse-opts new-element)]
      (set! gap (or (util/checked-get-optional opts :gap number?) 0)))))

(defn- column-ctor [& children]
  (map->Column {}))
