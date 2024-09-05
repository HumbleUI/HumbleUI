(in-ns 'io.github.humbleui.ui)

(defn- row-children-sizes [this ctx cs]
  (let [{:keys [gap children]} this
        gap-px         (scaled gap ctx)
        gaps           (-> children count dec (max 0))
        hug-sizes      (mapv
                         (fn [child]
                           (let [stretch (-> child :element meta :stretch)
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
                         (- (* gap-px gaps))
                         (max 0))
        sizes          (mapv
                         (fn [{:keys [child stretch size] :as m}]
                           (if stretch
                             (let [width (-> space (/ total-stretch) (* stretch) math/round)
                                   size  (measure child ctx (assoc cs :width width))]
                               (assoc m :size size))
                             m))
                         hug-sizes)
        width          (+ (transduce (map #(-> % :size :width))  +   0 sizes) (* gap-px gaps))
        height         (transduce (map #(-> % :size :height)) max 0 sizes)]
    (util/set!! this
      :this-size      (util/ipoint width height)
      :children-sizes sizes
      :hug-width      hug-width
      :total-stretch  total-stretch)))

(util/deftype+ Row [gap
                    children-sizes
                    hug-width
                    total-stretch]
  :extends AContainerNode
    
  (-measure-impl [this ctx cs]
    (row-children-sizes this ctx cs)
    this-size)
  
  (-draw-impl [this ctx bounds container-size viewport ^Canvas canvas]
    (when-not this-size
      (row-children-sizes this ctx container-size))
    (let [gap-px (scaled gap ctx)
          gaps   (-> children count dec (max 0))
          space  (-> (:width bounds)
                   (- hug-width)
                   (- (* gap-px gaps))
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
            (recur (long (+ x gap-px child-width))))))))
  
  (-reconcile-opts [this ctx new-element]
    (let [opts (parse-opts new-element)
          gap' (or (util/checked-get-optional opts :gap number?) 0)]
      (when (not= gap gap')
        (set! gap gap')
        (invalidate-size this)))))

(defn- row-ctor [& children]
  (map->Row {}))
