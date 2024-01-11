(in-ns 'io.github.humbleui.ui)

(core/deftype+ WithContext [data child ^:mut child-rect]
  protocols/IComponent
  (-context [_ ctx]
    (merge ctx data))
  
  (-measure [this ctx cs]
    (measure child (protocols/-context this ctx) cs))
  
  (-draw [this ctx rect canvas]
    (set! child-rect rect)
    (draw-child child (protocols/-context this ctx) child-rect canvas))
  
  (-event [this ctx event]
    (event-child child (protocols/-context this ctx) event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child (protocols/-context this ctx) cb)))
  
  (-unmount [_]
    (unmount-child child)))

(defn with-context [data child]
  (->WithContext data child nil))
