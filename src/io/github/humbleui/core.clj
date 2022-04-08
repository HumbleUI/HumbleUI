(ns io.github.humbleui.core
  (:require
    [clojure.set :as set])
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

(defmacro cond+ [& clauses]
  (when-some [[test expr & rest] clauses]
    (condp = test
      :do   `(do ~expr (cond+ ~@rest))
      :let  `(let ~expr (cond+ ~@rest))
      :some `(or ~expr (cond+ ~@rest))
      `(if ~test ~expr (cond+ ~@rest)))))

(defmacro case-instance [e & clauses]
  `(condp instance? ~e
     ~@(mapcat
         (fn [expr]
           (case (count expr)
             1 expr
             2 (let [[type clause] expr]
                 [type `(let [~e ~(vary-meta e assoc :tag type)]
                          ~clause)])))
         (partition-all 2 clauses))))

(defmacro spy [msg & body]
  `(let [ret# (do ~@body)]
     (println (str ~msg ":") ret#)
     ret#))

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

(defmacro for-vec [& body]
  `(vec (for ~@body)))

(defmacro for-map [& body]
  `(into {} (for ~@body)))

(defn zip [& xs]
  (apply map vector xs))

(defn start [^Runnable cb]
  (App/start cb))

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

(defprotocol ISettable
  (-set! [_ key value]))

(defmacro deftype+
  "Same as deftype, but:

   1. Uses ^:mut instead of ^:unsynchronized-mutable
   2. Allows using type annotations in protocol arglist
   3. Read mutable fields through ILookup: (:field instance)
   4. Write to mutable fields from outside through ISettable: (-set! instance key value)
   5. Allow with-meta"
  [name fields & body]
  (let [update-field  #(vary-meta % set/rename-keys {:mut :unsynchronized-mutable})
        remove-tag    #(vary-meta % dissoc :tag)
        update-method (fn [[name args & body]]
                        (list name
                          (mapv remove-tag args)
                          (list* 'clojure.core/let
                            (vec (mapcat #(vector % (remove-tag %)) (filter #(:tag (meta %)) args)))
                            body)))
        value-sym     (gensym 'value)]
    `(do
       (deftype ~name
         ~(mapv update-field (conj fields '__m))
       
         ~@(map #(if (list? %) (update-method %) %) body)
       
         clojure.lang.IMeta
         (meta [_] ~'__m)
       
         clojure.lang.IObj
         (withMeta [_ meta#]
           (new ~name ~@fields meta#))
       
         clojure.lang.ILookup
         (valAt [_# key# notFound#]
           (case key#
             ~@(mapcat #(vector (keyword %) %) fields)
             notFound#))
         (valAt [this# key#]
           (.valAt this# key# nil))
       
         ISettable
         (-set! [_# key# ~value-sym]
           (case key#
             ~@(mapcat #(vector (keyword %) (list 'set! % value-sym)) (filter #(:mut (meta %)) fields)))))
       (defn ~(symbol (str '-> name)) ~fields
         (new ~name ~@fields nil)))))