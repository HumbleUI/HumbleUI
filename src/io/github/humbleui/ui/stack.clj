(ns io.github.humbleui.ui.stack
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [io.github.humbleui.types IPoint RRect]
    [io.github.humbleui.skija Color Font]))

(core/deftype+ Stack [children ^:mut my-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (reduce
      (fn [size child]
        (let [{:keys [width height]} (core/measure child ctx cs)]
          (IPoint. (max (:width size) width) (max (:height size) height))))
      (IPoint. 0 0) children))
  
  (-draw [_ ctx ^IRect rect ^Canvas canvas]
    (set! my-rect rect)
    (doseq [child children]
      (core/draw-child child ctx rect canvas)))
  
  (-event [_ ctx event]
    (reduce 
      (fn [_ child]
        (when-let [res (core/event-child child ctx event)]
          (reduced res)))
      nil (reverse children)))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (some #(protocols/-iterate % ctx cb) children))))

(defn stack [& children]
  (->Stack children nil))
