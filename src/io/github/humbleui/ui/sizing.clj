(in-ns 'io.github.humbleui.ui)

(core/deftype+ Width []
  :extends AWrapperNode
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[_ opts _] (parse-element element)
          width'     (dimension (:width opts) cs ctx)
          child-size (measure child ctx (assoc cs :width width'))]
      (assoc child-size :width width'))))

(defn width [opts child]
  (map->Width {}))

(core/deftype+ Height []
  :extends AWrapperNode
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[_ opts _] (parse-element element)
          height'    (dimension (:height opts) cs ctx)
          child-size (measure child ctx (assoc cs :height height'))]
      (assoc child-size :height height'))))

(defn height [opts child]
  (map->Height {}))

; (core/deftype+ MaxWidth [probes]
;   :extends core/AWrapper
  
;   protocols/IComponent
;   (-measure [_ ctx cs]
;     (let [width (->> probes
;                   (map #(:width (core/measure % ctx cs)))
;                   (reduce max 0))
;           child-size (core/measure child ctx cs)]
;       (assoc child-size :width width))))

; (defn max-width [probes child]
;   (map->MaxWidth
;     {:probes probes
;      :child  child}))

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
