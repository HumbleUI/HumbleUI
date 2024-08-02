(in-ns 'io.github.humbleui.ui)

(defn- row-children-sizes [this ctx cs]
  (let [{:keys [children]} this
        children-sizes'    (mapv
                             (fn [child]
                               [child (-> child :element meta :stretch (or :hug)) (measure child ctx cs)])
                             children)
        width'              (transduce (map #(-> % (nth 2) :width))  +   0 children-sizes')
        height'             (transduce (map #(-> % (nth 2) :height)) max 0 children-sizes')
        hug-width'          (transduce
                              (comp
                                (filter #(-> % (nth 1) (= :hug)))
                                (map #(-> % (nth 2) :width)))
                              + 0 children-sizes')
        total-stretch'      (transduce
                              (comp
                                (filter #(-> % (nth 1) number?))
                                (map #(-> % (nth 1))))
                              + 0 children-sizes')]
    (util/set!! this
      :children-sizes children-sizes'
      :width          width'
      :height         height'
      :hug-width      hug-width'
      :total-stretch  total-stretch')))

(util/deftype+ Row [^:mut gap
                    ^:mut children-sizes
                    ^:mut width
                    ^:mut height
                    ^:mut hug-width
                    ^:mut total-stretch]
  :extends AContainerNode
    
  (-measure-impl [this ctx cs]
    (row-children-sizes this ctx cs)
    (let [gap-px (scaled gap)
          gaps   (-> children count dec (max 0))]
      (util/ipoint (+ width (* gap-px gaps)) height)))
  
  (-draw-impl [this ctx bounds viewport ^Canvas canvas]
    (row-children-sizes this ctx (util/irect-size bounds))
    (let [gap-px (scaled gap)
          gaps   (-> children count dec (max 0))
          space  (-> (:width bounds)
                   (- hug-width)
                   (- (* gap-px gaps))
                   (max 0))]
      (util/loopr [x (:x bounds)]
        [[child stretch child-size] children-sizes]
        (let [child-width (if (= :hug stretch)
                            (:width child-size)
                            (-> space (/ total-stretch) (* stretch) math/round))
              child-bounds (util/irect-xywh x (:y bounds) (max 0 child-width) (max 0 (:height bounds)))]
          (draw child ctx child-bounds viewport canvas)
          (when (< x (:right viewport))
            (recur (long (+ x gap-px child-width))))))))
  
  (-reconcile-opts [_this ctx new-element]
    (let [opts (parse-opts new-element)]
      (set! gap (or (util/checked-get-optional opts :gap number?) 0)))))

(defn- row-ctor [& children]
  (map->Row {}))
