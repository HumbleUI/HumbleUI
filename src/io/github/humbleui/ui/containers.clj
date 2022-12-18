(ns io.github.humbleui.ui.containers
  (:require
    [clojure.math :as math]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]))

(defn flatten-container [input]
  (into []
    (mapcat
      #(cond
         (nil? %)        []
         (vector? %)     [%]
         (sequential? %) (flatten-container %)
         :else           [[:hug nil %]]))
    input))

(defn- normalize-input [input]
  (let [input' (flatten-container input)]
    {:modes    (mapv #(nth % 0 nil) input')
     :factors  (mapv #(nth % 1 nil) input')
     :children (mapv #(nth % 2 nil) input')}))

(core/deftype+ Column [modes factors]
  :extends core/AContainer
    
  protocols/IComponent
  (-measure [_ ctx cs]
    (->> children
      (filter some?)
      (reduce
        (fn [{:keys [width height]} child]
          (let [child-size (core/measure child ctx cs)]
            (core/ipoint (max width (:width child-size)) (+ height (:height child-size)))))
        (core/ipoint 0 0))))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [cs      (core/ipoint (:width rect) (:height rect))
          known   (mapv
                    (fn [mode child]
                      (when (= :hug mode)
                        (core/measure child ctx cs)))
                    modes children)
          space   (- (:height rect) (transduce (keep :height) + 0 known))
          stretch (->> (map #(when (= :stretch %1) %2) modes factors)
                    (filter some?)
                    (reduce + 0))]
      (reduce
        (fn [height [size mode factor child]]
          (let [child-height (long
                               (case mode
                                 :hug     (:height size)
                                 :stretch (-> space (/ stretch) (* factor) (math/round))))
                child-rect (core/irect-xywh (:x rect) (+ (:y rect) height) (max 0 (:width rect)) (max 0 child-height))]
            (core/draw-child child ctx child-rect canvas)
            (+ height child-height)))
        0
        (core/zip known modes factors children)))))

(defn column [& children]
  (map->Column
    (normalize-input children)))

(core/deftype+ Row[modes factors]
  :extends core/AContainer
    
  protocols/IComponent
  (-measure [_ ctx cs]
    (->> children
      (filter some?)
      (reduce
        (fn [{:keys [width height]} child]
          (let [child-size (core/measure child ctx cs)]
            (core/ipoint (+ width (:width child-size)) (max height (:height child-size)))))
        (core/ipoint 0 0))))
  
  (-draw [_ ctx rect canvas]
    (let [cs      (core/ipoint (:width rect) (:height rect))
          known   (mapv
                    (fn [mode child]
                      (when (= :hug mode)
                        (core/measure child ctx cs)))
                    modes children)
          space   (- (:width rect) (transduce (keep :width) + 0 known))
          stretch (->> (map #(when (= :stretch %1) %2) modes factors)
                    (filter some?)
                    (reduce + 0))]
      (reduce
        (fn [width [size mode factor child]]
          (let [child-width (long
                              (case mode
                                :hug     (:width size)
                                :stretch (-> space (/ stretch) (* factor) (math/round))))
                child-rect (core/irect-xywh (+ (:x rect) width) (:y rect) (max 0 child-width) (max 0 (:height rect)))]
            (core/draw-child child ctx child-rect canvas)
            (+ width child-width)))
        0
        (core/zip known modes factors children)))))

(defn row [& children]
  (map->Row
    (normalize-input children)))
