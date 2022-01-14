(ns io.github.humbleui.core
  (:import
   [io.github.humbleui.jwm App Screen]
   [java.lang AutoCloseable]))

(defn memoize-last [ctor]
  (let [*atom (volatile! nil)]
    (fn [& args']
      (or
        (when-some [[args value] @*atom]
          (if (= args args')
            value
            (when (instance? AutoCloseable value)
              (.close ^AutoCloseable value))))
        (let [value' (apply ctor args')]
          (vreset! *atom [args' value'])
          value')))))

(defmacro defn-memoize-last [name & body]
  `(def ~name (memoize-last (fn ~@body))))

(defn eager-or
  ([] nil)
  ([x] x)
  ([x y] (or x y))
  ([x y z] (or x y z))
  ([x y z & rest]
   (reduce #(or %1 %2) (or x y z) rest)))

(defn clamp [x from to]
  (min (max x from) to))

(defn init []
  (App/init))

(defn start []
  (App/start))

(defn terminate []
  (App/terminate))

(defmacro doui-async [& forms]
  `(let [p# (promise)]
     (App/runOnUIThread #(deliver p# (try ~@forms (catch Throwable t# t#))))
     p#))

(defmacro doui [& forms]
  `(let [res# (deref (doui-async ~@forms))]
     (if (instance? Throwable res#)
       (throw res#)
       res#)))

(defmacro doto-some [x & forms]
  `(let [x# ~x]
     (when (some? x#)
       (doto x# ~@forms))))

(defn screen->clj [^Screen screen]
  {:id        (.getId screen)
   :primary?  (.isPrimary screen)
   :bounds    (.getBounds screen)
   :work-area (.getWorkArea screen)
   :scale     (.getScale screen)})

(defn primary-screen []
  (screen->clj (App/getPrimaryScreen)))

(defn screens []
  (mapv screen->clj (App/getScreens)))
