(ns io.github.humbleui.core
  (:require
    [clojure.java.io :as io]
    [clojure.math :as math]
    [clojure.set :as set]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [io.github.humbleui.jwm App Screen]
    [io.github.humbleui.skija Canvas]
    [io.github.humbleui.skija.shaper Shaper]
    [io.github.humbleui.types IPoint IRect]
    [java.lang AutoCloseable]
    [java.util Timer TimerTask]))

(set! *warn-on-reflection* true)

;; constants

(def double-click-threshold-ms 500)

;; state

(def ^Shaper shaper (Shaper/makeShapeDontWrapOrReorder))

(defonce ^Timer timer (Timer. true))

;; macros

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

(defmacro cond+ [& clauses]
  (when-some [[test expr & rest] clauses]
    (condp = test
      :do   `(do ~expr (cond+ ~@rest))
      :let  `(let ~expr (cond+ ~@rest))
      :some `(or ~expr (cond+ ~@rest))
      `(if ~test ~expr (cond+ ~@rest)))))

(defmacro case-instance
  "Dispatch on `instance?` and tag symbol inside matched branch"
  [e & clauses]
  `(condp instance? ~e
     ~@(mapcat
         (fn [expr]
           (case (count expr)
             1 expr
             2 (let [[type clause] expr]
                 [type `(let [~e ~(vary-meta e assoc :tag type)]
                          ~clause)])))
         (partition-all 2 clauses))))

(defmacro when-case
  "Same as
   
   (when <test>
     (case <e>
       <clauses>
       nil))"
  [test e & clauses]
  `(when ~test
     (case ~e
       ~@clauses
       nil)))

(defmacro spy [msg & body]
  `(let [ret# (do ~@body)]
     (println (str ~msg ":") ret#)
     ret#))

(defmacro doto-some [x & forms]
  `(let [x# ~x]
     (when (some? x#)
       (doto x# ~@forms))))


;; utilities

(defn eager-or
  "A version of `or` that always evaluates all its arguments first"
  ([] nil)
  ([x] x)
  ([x y] (or x y))
  ([x y z] (or x y z))
  ([x y z & rest]
   (reduce #(or %1 %2) (or x y z) rest)))

(defn clamp [x from to]
  (min (max x from) to))

(defn between? [x from to]
  (and
    (<= from x)
    (< x to)))

(defmacro for-vec [& body]
  `(vec (for ~@body)))

(defmacro for-map [& body]
  `(into {} (for ~@body)))

(defn zip [& xs]
  (apply map vector xs))

(defn conjv-limited [xs x limit]
  (if (>= (count xs) limit)
    (conj (vec (take (dec limit) xs)) x)
    (conj (or xs []) x)))

(defn merge-some [a b]
  (merge-with #(or %2 %1) a b))

(defn collect
  "Traverse `form` recursively, returnning a vector of elements that satisfy `pred`"
  ([pred form] (collect [] pred form))
  ([acc pred form]
   (cond
     (pred form)        (conj acc form)
     (sequential? form) (reduce (fn [acc el] (collect acc pred el)) acc form)
     (map? form)        (reduce-kv (fn [acc k v] (-> acc (collect pred k) (collect pred v))) acc form)
     :else              acc)))

(defn ^bytes slurp-bytes [src]
  (if (bytes? src)
    src
    (with-open [is (io/input-stream src)]
      (.readAllBytes is))))

(defn lazy-resource [path]
  (delay
    (slurp-bytes
      (io/resource (str "io/github/humbleui/" path)))))

(defn measure [comp ctx ^IPoint cs]
  {:pre  [(instance? IPoint cs)]
   :post [(instance? IPoint %)]}
  (protocols/-measure comp ctx cs))

(defn draw [comp ctx ^IRect rect ^Canvas canvas]
  {:pre [(instance? IRect rect)]}
  (protocols/-draw comp ctx rect canvas))

(defn draw-child [comp ctx ^IRect rect ^Canvas canvas]
  (when comp
    (let [count (.getSaveCount canvas)]
      (try
        (draw comp ctx rect canvas)
        (finally
          (.restoreToCount canvas count))))))

(defn event [comp ctx event]
  (protocols/-event comp ctx event))

(defn event-child [comp ctx event]
  (when comp
    (protocols/-event comp ctx event)))

(defn child-close [child]
  (when (instance? AutoCloseable child)
    (.close ^AutoCloseable child)))

(defn dimension ^long [size cs ctx]
  (let [scale (:scale ctx)]
    (->
      (if (fn? size)
        (* scale
          (size {:width  (/ (:width cs) scale)
                 :height (/ (:height cs) scale)
                 :scale  scale}))
        (* scale size))
      (math/round)
      (long))))

;; deftype+

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
       
         protocols/ISettable
         (-set! [_# key# ~value-sym]
           (case key#
             ~@(mapcat #(vector (keyword %) (list 'set! % value-sym)) (filter #(:mut (meta %)) fields)))))
       
       (defn ~(symbol (str '-> name)) ~fields
         (new ~name ~@fields nil)))))

(defn- ^TimerTask timer-task [f]
  (proxy [TimerTask] []
    (run []
      (try
        (f)
        (catch Throwable t
          (.printStackTrace t))))))

(defn schedule
  ([f ^long delay]
   (let [t (timer-task f)]
     (.schedule timer t delay)
     #(.cancel t)))
  ([f ^long delay ^long period]
   (let [t (timer-task f)]
     (.scheduleAtFixedRate timer t delay period)
     #(.cancel t))))

(defn now []
  (System/currentTimeMillis))