(in-ns 'io.github.humbleui.ui)

(defn- row-children-sizes [this ctx cs]
  (let [{:keys [children]} this
        hug-sizes      (mapv
                         (fn [child]
                           (let [stretch (-> child :element meta :stretch)
                                 stretch (if (true? stretch) 1 stretch)
                                 size    (when (nil? stretch)
                                           (measure child ctx cs))]
                             {:child   child
                              :stretch stretch
                              :size    size}))
                         children)
        hug-width      (transduce (keep #(-> % :size :width)) + 0 hug-sizes)
        total-stretch  (transduce (keep :stretch) + 0 hug-sizes)
        space          (-> (:width cs)
                         (- hug-width)
                         (max 0))
        sizes          (mapv
                         (fn [{:keys [child stretch size] :as m}]
                           (if stretch
                             (let [width (-> space (/ total-stretch) (* stretch) math/round)
                                   size  (measure child ctx (assoc cs :width width))]
                               (assoc m :size size))
                             m))
                         hug-sizes)
        width          (transduce (map #(-> % :size :width))  +   0 sizes)
        height         (transduce (map #(-> % :size :height)) max 0 sizes)]
    (util/set!! this
      :this-size      (util/ipoint width height)
      :children-sizes sizes
      :hug-width      hug-width
      :total-stretch  total-stretch)))

(util/deftype+ Row [children-sizes
                    hug-width
                    total-stretch]
  :extends AContainerNode
    
  (-measure-impl [this ctx cs]
    (row-children-sizes this ctx cs)
    this-size)
  
  (-draw-impl [this ctx bounds container-size viewport ^Canvas canvas]
    (when-not this-size
      (row-children-sizes this ctx container-size))
    (let [space  (-> (:width bounds)
                   (- hug-width)
                   (max 0))]
      (util/loopr [x (:x bounds)]
        [{:keys [child stretch size]} children-sizes]
        (let [child-width  (if stretch
                             (-> space (/ total-stretch) (* stretch) math/round)
                             (:width size))
              child-bounds (util/irect-xywh x (:y bounds) child-width (:height bounds))
              child-cs     (cond-> container-size
                             stretch (assoc :width child-width))]
          (draw child ctx child-bounds child-cs viewport canvas)
          (when (< x (:right viewport))
            (recur (long (+ x child-width))))))))
    
  (-child-elements [this ctx new-element]
    (let [[_ opts child-els] (parse-element new-element)
          child-els          (util/flatten child-els)
          gap                (:gap opts)
          gap                (if (number? gap)
                               [size {:width gap}]
                               gap)]
      (cond->> child-els
        gap
        (interpose gap)
        
        (:align opts)
        (map #(vector align {:y (:align opts)} %))))))

(defn- row-ctor
    "Container component. Children take minimal width by default (hug):
   
     [ui/row
       [ui/gap {:width 10}]
       [ui/gap {:width 20}]]
   
   Add ^:stretch metadata to make children occupy all leftover space after hugs:
   
     [ui/row
       ^:stretch [ui/gap]
       [ui/gap]]
   
   Stretch can be a number, in that case space is divided proportionally:
   
     [ui/row
       ^{:stretch 1} [ui/gap]
       ^{:stretch 2} [ui/gap]]
   
   Row’s height and all of its childrens’ heights are set to the tallest child.
   
   Options are:
   
     :gap   :: <number> | <markup> - what to put between children
     :align :: :top | :center | :bottom | <number> - vertical align to apply to all children"
  [& children]
  (map->Row {}))
