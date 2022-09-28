(ns io.github.humbleui.core
  (:require
    [clojure.java.io :as io]
    [clojure.math :as math]
    [clojure.set :as set]
    [clojure.walk :as walk]
    [io.github.humbleui.error :as error]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [io.github.humbleui.jwm App Screen]
    [io.github.humbleui.skija Canvas]
    [io.github.humbleui.skija.shaper Shaper]
    [io.github.humbleui.types IPoint IRect Point Rect RRect]
    [java.lang AutoCloseable]
    [java.util Timer TimerTask]))

(set! *warn-on-reflection* true)

;; constants

(def double-click-threshold-ms
  500)

;; state

(def ^Shaper shaper
  (Shaper/makeShapeDontWrapOrReorder))

(defonce ^Timer timer
  (Timer. true))

(def ^{:arglists '([^Throwable t])} log-error
  error/log-error)

(defmacro catch-and-log [& body]
  `(try
     ~@body
     (catch Throwable t#
       (log-error t#))))

;; macros

(defn collect
  "Traverse `form` recursively, returnning a vector of elements that satisfy `pred`"
  ([pred form] (collect [] pred form))
  ([acc pred form]
   (cond
     (pred form)        (conj acc form)
     (sequential? form) (reduce (fn [acc el] (collect acc pred el)) acc form)
     (map? form)        (reduce-kv (fn [acc k v] (-> acc (collect pred k) (collect pred v))) acc form)
     :else              acc)))

(defn bindings->syms
  "Takes let-like bindings and returns all symbols on left sides, including
   inside destructured forms"
  [bindings]
  (as-> bindings %
    (partition 2 %)
    (map first %)
    (collect symbol? %)
    (map name %)
    (map symbol %)
    (into #{} %)
    (disj % '& '_)
    (vec %)))

(defmacro when-every [bindings & body]
  `(let ~bindings
     (when (every? some? ~(bindings->syms bindings))
       ~@body)))

(defn memoize-last [ctor]
  (let [*state (volatile! nil)]
    (fn [& args']
      (or
        (when-some [[args value] @*state]
          (if (= args args')
            value
            (when (instance? AutoCloseable value)
              (.close ^AutoCloseable value))))
        (let [value' (try
                       (apply ctor args')
                       (catch Throwable t
                         (log-error t)
                         t))]
          (vreset! *state [args' value'])
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

(defmacro match
  "Similar to case/condp =, but supports wildcards

   (match [a b (+ a b)]
     [1 2 3] (println a)
     [1 _ 3] (println b)
     :else   (println (+ a b))))"
  [e & clauses]
  (let [es       (mapv #(gensym (if (symbol? %) (name %) "G__")) e)
        gen-cond (fn [pattern]
                   (if (vector? pattern)
                     (list* `and
                       (map (fn [sym val]
                               (if (= val '_)
                                 true
                                 `(= ~sym ~val))) es pattern))
                     pattern))]
    `(let ~(vec (mapcat vector es e))
       (cond
         ~@(mapcat identity
             (for [[pattern body] (partition 2 clauses)]
               [(gen-cond pattern) body]))))))

(defmacro spy [msg & body]
  `(let [ret# (do ~@body)]
     (println (str ~msg ":") ret#)
     ret#))

(defmacro doto-some [x & forms]
  `(let [x# ~x]
     (when (some? x#)
       (doto x# ~@forms))))

(defn- loopr-rewrite-recurs [accs body]
  (walk/prewalk
    (fn [form]
      (if (and (seq? form) (= 'recur (first form)))
        `(do
           ~@(map (fn [[vs s _] v] `(vreset! ~vs ~v)) accs (next form)))
        form))
    body))

(defn- loopr-reduce [accs iters body]
  (let [[sym val & rest] iters]
    `(reduce
       (fn [_# ~sym]
         ~(if (seq rest)
            (loopr-reduce accs rest body)
            `(let ~(->> accs
                     (mapcat (fn [[vs s _]] [s `(deref ~vs)]))
                     vec)
               ~(loopr-rewrite-recurs accs body))))
       nil
       ~val)))

; Inspired by https://aphyr.com/posts/360-loopr-a-loop-reduction-macro-for-clojure
(defmacro loopr
  "loop + reduce + for-like nested iteration in one.
   
   Takes an initial binding vector for accumulator variables, (like `loop`);
   then a binding vector of loop variables to collections (like `for`);
   then a body form, then a final form.

   Iterates over each element of the collections, like `for` would, and
   evaluates body with that combination of elements bound.

   Like `loop`, the body should generally contain one or more (recur ...) forms
   with new values for each accumulator.

   Example:

   (loopr [count 0
           sum   0]
          [row [[1 2 3] [4 5 6] [7 8 9]]
           x   row]
     (recur (inc count) (+ sum x))
     (/ sum count)) ; => 45/9 = 5"
  [accs iters body & [final]]
  (assert (even? (count accs)) (str "Exptected even number of accs, got " (count accs)))
  (assert (even? (count iters)) (str "Exptected even number of iters, got " (count iters)))
  (assert (<= 2 (count iters)) (str "Exptected at least one iterator"))
  (let [accs'  (mapv (fn [[s v]] [(gensym (str s)) s v]) (partition 2 accs))
        final' (cond
                 (some? final)       final
                 (= 1 (count accs')) (second (first accs'))
                 :else               (mapv second accs'))]
    `(let ~(->> accs'
             (mapcat (fn [[vs s v]] [vs `(volatile! ~v) s `(deref ~vs)]))
             vec)
       ~(loopr-reduce accs' iters body)
       (let ~(->> accs'
               (mapcat (fn [[vs s v]] [s `(deref ~vs)]))
               vec)
         ~final'))))
    
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

(defn ^bytes slurp-bytes [src]
  (if (bytes? src)
    src
    (with-open [is (io/input-stream src)]
      (.readAllBytes is))))

(defn lazy-resource [path]
  (delay
    (slurp-bytes
      (io/resource (str "io/github/humbleui/" path)))))

(defn ^IPoint ipoint [^long x ^long y]
  (IPoint. x y))

(defn ^IPoint size [^long x ^long y]
  (IPoint. x y))

(defn ^Point point [x y]
  (Point. x y))

(defn ^IRect irect-xywh [^long x ^long y ^long w ^long h]
  (IRect/makeXYWH x y w h))

(defn ^IRect irect-ltrb [^long l ^long t ^long r ^long b]
  (IRect/makeLTRB l t r b))

(defn ^Rect rect-xywh [x y w h]
  (Rect/makeXYWH x y w h))

(defn ^Rect rect-ltrb [l t r b]
  (Rect/makeLTRB l t r b))

(defn ^RRect rrect-xywh
  ([x y w h r]
   (RRect/makeXYWH x y w h r))
  ([x y w h xr yr]
   (RRect/makeXYWH x y w h xr yr))
  ([x y w h tr tl br bl]
   (RRect/makeXYWH x y w h tr tl br bl)))

(defn ^RRect rrect-ltrb
  ([l t r b r]
   (RRect/makeLTRB l t r b r))
  ([l t r b xr yr]
   (RRect/makeLTRB l t r b xr yr))
  ([l t r b tr tl br bl]
   (RRect/makeLTRB l t r b tr tl br bl)))

(defn rect-contains? [rect point]
  {:pre [(some? rect)
         (some? point)]}
  (and
    (<= (:x rect) (:x point))
    (< (:x point) (:right rect))
    (<= (:y rect) (:y point))
    (< (:y point) (:bottom rect))))

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
          (log-error t))))))

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

(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread ex]
      (log-error ^Throwable ex))))

(defmacro thread [& body]
  `(future
     (try
       ~@body
       (catch Throwable t#
         (log-error t#)))))