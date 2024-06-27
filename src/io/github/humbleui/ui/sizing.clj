(in-ns 'io.github.humbleui.ui)

(core/deftype+ Width []
  :extends AWrapperNode
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[_ opts _] (parse-element element)
          width'     (dimension (core/checked-get opts :width number?) cs ctx)
          child-size (measure child ctx (assoc cs :width width'))]
      (assoc child-size :width width'))))

(defn width [opts child]
  (map->Width {}))

(core/deftype+ Height []
  :extends AWrapperNode
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[_ opts _] (parse-element element)
          height'    (dimension (core/checked-get opts :height number?) cs ctx)
          child-size (measure child ctx (assoc cs :height height'))]
      (assoc child-size :height height'))))

(defn height [opts child]
  (map->Height {}))

(defn size [opts child]
  [ui/width {:width (:width opts)}
   [ui/height {:height (:height opts)}
    child]])

(core/deftype+ MaxWidth []
  :extends AWrapperNode
  
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[_ opts _] (parse-element element)
          probes     (core/checked-get opts :probes sequential?)
          width      (->> probes
                       (map #(-> % make (measure ctx cs) :width))
                       (reduce max 0))
          child-size (measure child ctx cs)]
      (assoc child-size :width width))))

(defn max-width [opts child]
  (map->MaxWidth {}))

(defn node-size []
  (let [scale (or (:scale *ctx*) 1)
        w     (or (:width (:rect *node*)) 0)
        h     (or (:height (:rect *node*)) 0)]
    (core/point (/ w scale) (/ h scale))))

(defn use-size []
  (let [*size (signal/signal (core/point 0 0))]
    {:before-draw
     (fn []
       (signal/reset! *size (node-size)))
     :value *size}))
