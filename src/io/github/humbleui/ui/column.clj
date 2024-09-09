(in-ns 'io.github.humbleui.ui)

(defn- column-children-sizes [this ctx cs]
  (let [{:keys [children]} this
        hug-sizes     (mapv
                        (fn [child]
                          (let [stretch (-> child :element meta :stretch)
                                stretch (if (true? stretch) 1 stretch)
                                size    (when (nil? stretch)
                                          (measure child ctx cs))]
                            {:child   child
                             :stretch stretch
                             :size    size}))
                        children)
        hug-height    (transduce (keep #(-> % :size :height)) + 0 hug-sizes)
        total-stretch (transduce (keep :stretch) + 0 hug-sizes)
        space         (-> (:height cs)
                        (- hug-height)
                        (max 0))
        sizes         (mapv
                        (fn [{:keys [child stretch size] :as m}]
                          (if stretch
                            (let [height (-> space (/ total-stretch) (* stretch) math/round)
                                  size   (measure child ctx (assoc cs :height height))]
                              (assoc m :size size))
                            m))
                        hug-sizes)
        height        (transduce (map #(-> % :size :height))  +   0 sizes)
        width         (transduce (map #(-> % :size :width)) max 0 sizes)]
    (util/set!! this
      :this-size      (util/ipoint width height)
      :children-sizes sizes
      :hug-height     hug-height
      :total-stretch  total-stretch)))

(util/deftype+ Column [children-sizes
                       hug-height
                       total-stretch]
  :extends AContainerNode
  
  (-measure-impl [this ctx cs]
    (column-children-sizes this ctx cs)
    this-size)
  
  (-draw-impl [this ctx bounds container-size viewport ^Canvas canvas]
    (when-not this-size
      (column-children-sizes this ctx container-size))
    (let [space  (-> (:height bounds)
                   (- hug-height)
                   (max 0))]
      (util/loopr [y (:y bounds)]
        [{:keys [child stretch size]} children-sizes]
        (let [child-height (if stretch
                             (-> space (/ total-stretch) (* stretch) math/round)
                             (:height size))
              child-bounds (util/irect-xywh (:x bounds) y (:width bounds) child-height)
              child-cs     (cond-> container-size
                             stretch (assoc :height child-height))]
          (draw child ctx child-bounds child-cs viewport canvas)
          (when (< y (:bottom viewport))
            (recur (long (+ y child-height))))))))
  
  (-child-elements [this ctx new-element]
    (let [[_ opts child-els] (parse-element new-element)
          child-els          (util/flatten child-els)
          gap                (:gap opts)
          gap                (if (number? gap)
                               [size {:height gap}]
                               gap)]
      (cond->> child-els
        gap
        (interpose gap)
        
        (:align opts)
        (map #(vector align {:x (:align opts)} %))))))

(defn- column-ctor [& children]
  (map->Column {}))
