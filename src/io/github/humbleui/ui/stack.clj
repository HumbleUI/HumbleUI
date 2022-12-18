(ns io.github.humbleui.ui.stack
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]))

(core/deftype+ Stack []
  :extends core/AContainer
  
  protocols/IComponent
  (-measure [_ ctx cs]
    (reduce
      (fn [size child]
        (let [{:keys [width height]} (core/measure child ctx cs)]
          (core/ipoint (max (:width size) width) (max (:height size) height))))
      (core/ipoint 0 0) children))
  
  (-draw [_ ctx rect canvas]
    (doseq [child children]
      (core/draw-child child ctx rect canvas)))
  
  (-event [_ ctx event]
    (reduce 
      (fn [_ child]
        (when-let [res (core/event-child child ctx event)]
          (reduced res)))
      nil
      (reverse children))))

(defn stack [& children]
  (map->Stack
    {:children (->> children flatten (remove nil?) vec)}))
