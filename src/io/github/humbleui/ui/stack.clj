(in-ns 'io.github.humbleui.ui)

(util/deftype+ Stack []
  :extends AContainerNode
  
  (-measure-impl [_ ctx cs]
    (reduce
      (fn [size child]
        (let [{:keys [width height]} (measure child ctx cs)]
          (util/ipoint
            (max (:width size) width)
            (max (:height size) height))))
      (util/ipoint 0 0)
      children))
  
  (-draw-impl [_ ctx bounds container-size viewport canvas]
    (doseq [child children]
      (draw child ctx bounds container-size viewport canvas)))
  
  (-event [this ctx event]
    (let [ctx (protocols/-context this ctx)]
      (binding [*node* this
                *ctx*  ctx]
        (reduce 
          (fn [_ child]
            (when-let [res (ui/event child ctx event)]
              (reduced res)))
          nil
          (reverse children))))))

(defn- stack-ctor [& children]
  (map->Stack {}))
