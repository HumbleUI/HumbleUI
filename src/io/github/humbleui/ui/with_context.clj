(in-ns 'io.github.humbleui.ui)

(util/deftype+ WithContextClassic [data child ^:mut bounds]
  protocols/IComponent
  (-context [_ ctx]
    (merge ctx data))
  
  (-measure [this ctx cs]
    (measure child (protocols/-context this ctx) cs))
  
  (-draw [this ctx bounds' canvas]
    (set! bounds bounds')
    (draw child (protocols/-context this ctx) bounds canvas))
  
  (-event [this ctx event]
    (ui/event child (protocols/-context this ctx) event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (iterate child (protocols/-context this ctx) cb)))
  
  (-unmount [_]
    (unmount child)))

(defn with-context-classic [data child]
  (->WithContextClassic data child nil))

(util/deftype+ WithContext []
  :extends AWrapperNode
  protocols/IComponent
  (-context [_ ctx]
    (let [[_ overrides _ ] (parse-element element)]
      (merge ctx overrides))))

(defn with-context [overrides child]
  (map->WithContext {}))
