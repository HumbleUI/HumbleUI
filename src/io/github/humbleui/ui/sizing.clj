(in-ns 'io.github.humbleui.ui)

(core/deftype+ Width []
  :extends AWrapperNode
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[_ opts _] (parse-element element)
          width'     (dimension (core/checked-get opts :width (some-fn number? fn?)) cs ctx)
          child-size (measure child ctx (assoc cs :width width'))]
      (assoc child-size :width width'))))

(defn width [opts child]
  (map->Width {}))

(core/deftype+ Height []
  :extends AWrapperNode
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[_ opts _] (parse-element element)
          height'    (dimension (core/checked-get opts :height (some-fn number? fn?)) cs ctx)
          child-size (measure child ctx (assoc cs :height height'))]
      (assoc child-size :height height'))))

(defn height [opts child]
  (map->Height {}))

(core/deftype+ Size []
  :extends AWrapperNode
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[_ opts _] (parse-element element)
          width      (some-> opts :width (dimension cs ctx))
          height     (some-> opts :height (dimension cs ctx))]
      (cond
        (and width height)
        (core/ipoint width height)
        
        (and width child)
        (assoc (measure child ctx (assoc cs :width width)) :width width)
        
        (and height child)
        (assoc (measure child ctx (assoc cs :height height)) :height height)
        
        width
        (core/ipoint width 0)
        
        height
        (core/ipoint 0 height)
        
        :else
        (core/ipoint 0 0)))))

(defn size
  ([opts]
   (size opts nil))
  ([opts child]
   (map->Size {})))

(core/deftype+ ReserveWidth [^:mut probes]
  :extends AWrapperNode
  
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [width      (->> probes
                       (map #(-> % (measure ctx cs) :width))
                       (reduce max 0))
          child-size (measure child ctx cs)]
      (assoc child-size :width width)))
  
  (-reconcile-impl [this ctx el']
    (let [[_ opts [child-el]] (parse-element el')
          probes'             (reconcile-many ctx probes (core/checked-get opts :probes sequential?))
          [child']            (reconcile-many ctx [child] [child-el])]
      (set! probes probes')
      (set! child child'))))

(defn reserve-width [opts child]
  (map->ReserveWidth {}))

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
