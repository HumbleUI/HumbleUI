(ns io.github.humbleui.core
  (:import
   [io.github.humbleui.jwm App]
   [java.lang AutoCloseable]))

(defrecord Point [x y])

(defn point-offset [a b]
  (Point. (+ (:x a) (:x b)) (+ (:y a) (:y b))))

(defrecord Size [width height])

(defrecord Rect [x y width height])

(defn rect-contains? [{:keys [x y width height]} point]
  (and
    (<= x (:x point) (+ x width))
    (<= y (:y point) (+ y height))))

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