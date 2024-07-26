(in-ns 'io.github.humbleui.ui)

(util/deftype+ ReserveWidth [^:mut probes]
  :extends AWrapperNode
  
  (-measure-impl [_ ctx cs]
    (let [width      (->> probes
                       (map #(-> % (measure ctx cs) :width))
                       (reduce max 0))
          child-size (measure child ctx cs)]
      (assoc child-size :width width)))
  
  (-reconcile-impl [this ctx el']
    (let [[_ opts [child-el]] (parse-element el')
          probes'             (reconcile-many ctx probes (util/checked-get opts :probes sequential?))
          [child']            (reconcile-many ctx [child] [child-el])]
      (set! probes probes')
      (set! child child'))))

(defn reserve-width-ctor [opts child]
  (map->ReserveWidth {}))
