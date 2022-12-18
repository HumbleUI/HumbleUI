(ns io.github.humbleui.ui.sizing
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]))

(core/deftype+ Width [value]
  :extends core/AWrapper
  
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [width'     (core/dimension value cs ctx)
          child-size (core/measure child ctx (assoc cs :width width'))]
      (assoc child-size :width width'))))

(defn width [value child]
  (map->Width 
    {:value value
     :child child}))

(core/deftype+ Height [value]
  :extends core/AWrapper
  
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [height'    (core/dimension value cs ctx)
          child-size (core/measure child ctx (assoc cs :height height'))]
      (assoc child-size :height height'))))

(defn height [value child]
  (map->Height
    {:value value
     :child child}))

(core/deftype+ MaxWidth [probes]
  :extends core/AWrapper
  
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [width (->> probes
                  (map #(:width (core/measure % ctx cs)))
                  (reduce max 0))
          child-size (core/measure child ctx cs)]
      (assoc child-size :width width))))

(defn max-width [probes child]
  (map->MaxWidth
    {:probes probes
     :child  child}))

