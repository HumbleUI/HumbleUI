(in-ns 'io.github.humbleui.ui)

(core/deftype+ Stack []
  :extends AContainerNode
  
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (reduce
      (fn [size child]
        (let [{:keys [width height]} (measure child ctx cs)]
          (core/ipoint
            (max (:width size) width)
            (max (:height size) height))))
      (core/ipoint 0 0)
      children))
  
  (-draw-impl [_ ctx bounds canvas]
    (doseq [child children]
      (draw-child child ctx bounds canvas)))
  
  (-event [this ctx event]
    (let [ctx (protocols/-context this ctx)]
      (binding [*node* this
                *ctx*  ctx]
        (reduce 
          (fn [_ child]
            (when-let [res (event-child child ctx event)]
              (reduced res)))
          nil
          (reverse children))))))

(defn- stack-ctor [& children]
  (map->Stack {}))
