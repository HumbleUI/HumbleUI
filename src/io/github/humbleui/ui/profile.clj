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
        (let [t0       (System/nanoTime)
              duration 10000
              _        (profiler/start)
              ops      (loop [ops 0]
                         (if (< (- (System/nanoTime) t0) (* duration 1000000))
                           (do
                             (draw-child child ctx rect canvas)
                             (recur (inc ops)))
                           ops))
              file     (profiler/stop)]
          (println "Finished profiling, " (-> (System/nanoTime) (- t0) (/ 1000000.0) (/ ops) (->> (format "%.2f"))) " ms/op, " (.getPath ^File file))
          (reset! value false)))
      (draw-child child ctx rect canvas))))

(defn- profile-ctor [opts child]
  (map->Profile {}))

(comment
  (profiler/clear-results)
  (profiler/profile-for 10))