(ns io.github.humbleui.profile)

(def *stats (atom {}))

(defmacro measure [key & body]
  `(let [t#   (System/nanoTime)
         ret# (do ~@body)]
     (swap! *stats #(-> %
                      (update ~key update :time (fnil + 0) (- (System/nanoTime) t#))
                      (update ~key update :count (fnil inc 0))))
     ret#))

(defn reset
  ([]
   (reset! *stats {}))
  ([key]
   (swap! *stats dissoc key)))

(defn log
  ([]
   (doseq [[k _] @*stats]
     (log k)))
  ([key]
   (let [stats (get @*stats key)
         time  (-> (:time stats) (/ 1000000) (double))]
     (print (str key ":") (format "%.3f" time) "ms")
     (when (> (:count stats) 1)
       (print "," (:count stats) "iters," (format "%.3f" (/ time (:count stats))) "ms/iter"))
     (println))))