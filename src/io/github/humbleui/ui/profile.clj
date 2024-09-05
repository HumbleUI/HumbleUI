(in-ns 'io.github.humbleui.ui)

(require '[clj-async-profiler.core :as profiler])

(util/deftype+ Profile [value]
  :extends AWrapperNode  
  
  (-draw-impl [_ ctx bounds container-size viewport ^Canvas canvas]
    (when @value
      (println "Profiling...")
      (let [t0       (System/nanoTime)
            duration 10000
            _        (profiler/start)
            ops      (loop [ops 0]
                       (if (< (- (System/nanoTime) t0) (* duration 1000000))
                         (do
                           (draw child ctx bounds container-size viewport canvas)
                           (recur (inc ops)))
                         ops))
            file     (profiler/stop)]
        (println "Finished profiling, " (-> (System/nanoTime) (- t0) (/ 1000000.0) (/ ops) (->> (format "%.2f"))) " ms/op, " (.getPath ^File file))
        (reset! value false)))
    (draw child ctx bounds container-size viewport canvas))
  
  (-reconcile-opts [_this ctx new-element]
    (let [opts (parse-opts new-element)]
      (set! value (:value opts)))))

(defn- profile-ctor [opts child]
  (map->Profile {}))

(comment
  (profiler/clear-results)
  (profiler/profile-for 10))
