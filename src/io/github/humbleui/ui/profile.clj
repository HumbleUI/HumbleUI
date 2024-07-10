(in-ns 'io.github.humbleui.ui)

(require '[clj-async-profiler.core :as profiler])

(core/deftype+ Profile []
  :extends AWrapperNode  
  protocols/IComponent
  (-draw-impl [_ ctx rect ^Canvas canvas]
    (let [[_ opts _] (parse-element element)
          {:keys [value]} opts]
      (when @value
        (println "Profiling...")
        (profiler/profile
          (let [t0  (System/nanoTime)
                ops (loop [ops 0]
                      (if (< (- (System/nanoTime) t0) (* 10000 1000000))
                        (do
                          (draw-child child ctx rect canvas)
                          (recur (inc ops)))
                        ops))]
            (println "Finished profiling, " (-> (System/nanoTime) (- t0) (/ 1000000.0) (/ ops) (->> (format "%.2f"))) " ms/op")))
        (reset! value false))
      (draw-child child ctx rect canvas))))

(defn- profile-ctor [opts child]
  (map->Profile {}))
