(ns io.github.humbleui.core
  (:refer-clojure :exclude [find])
  (:require
    [clojure.java.io :as io]
    [clojure.math :as math]
    [clojure.set :as set]
    [clojure.walk :as walk]
    [io.github.humbleui.error :as error]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [io.github.humbleui.skija Canvas]
    [io.github.humbleui.skija.shaper Shaper]
    [io.github.humbleui.types IPoint IRange IRect Point Rect RRect]
    [java.lang AutoCloseable]
    [java.util Timer TimerTask]))

;; constants

(def double-click-threshold-ms
  500)

(defn radius->sigma [r]
  (+ 0.5 (* r 0.57735)))

(defn sigma->radius [s]
  (-> s
    (- 0.5)
    (/ 0.57735)))

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

(def ^:dynamic *ctx*)

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

(defn without-ns [sym]
  (with-meta
    (symbol (name sym))
    (meta sym)))

(defn bindings->syms
  "Takes let-like bindings and returns all symbols on left sides, including
   inside destructured forms"
  [bindings]
  (as-> bindings %
    (partition 2 %)
    (map first %)
    (collect symbol? %)
    (map without-ns %)
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
           ~@(map (fn [[vs _s _] v] `(vreset! ~vs ~v)) accs (next form)))
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
               (mapcat (fn [[vs s _v]] [s `(deref ~vs)]))
               vec)
         ~final'))))
    
;; utilities

(defn iround ^long [x]
  (long (math/round x)))

(defn iceil ^long [x]
  (long (math/ceil x)))

(defn ifloor ^long [x]
  (long (math/floor x)))

(defn eager-or
  "A version of `or` that always evaluates all its arguments first"
  ([] nil)
  ([x] x)
  ([x y] (or x y))
  ([x y z] (or x y z))
  ([x y z & rest]
   (reduce #(or %1 %2) (or x y z) rest)))

(defn invoke [f]
  (when f
    (f)))

(defn clamp [x from to]
  (min (max x from) to))

(defn single [xs]
  (assert (= 1 (count xs)) ("Expected 1 element, got " (count xs)))
  (first xs))

(defn some-map [& args]
  (persistent!
    (reduce
      (fn [m [k v]]
        (if (some? v)
          (assoc! m k v)
          m))
      (transient {}) (partition 2 args))))

(defn consv [x xs]
  (vec (cons x xs)))

(defn between? [x from to]
  (and
    (<= from x)
    (< x to)))

(defmacro for-vec [& body]
  `(vec (for ~@body)))

(defmacro for-map [& body]
  `(into {} (for ~@body)))

(defn map-vals [f m]
  (reduce-kv
    (fn [m k v]
      (assoc m k (f v)))
    {} m))

(defn map-by [f xs]
  (reduce
    (fn [m x]
      (assoc m (f x) x))
    {} xs))

(defn zip [& xs]
  (apply map vector xs))

(defn conjv-limited [xs x limit]
  (if (>= (count xs) limit)
    (conj (vec (take (dec limit) xs)) x)
    (conj (or xs []) x)))

(defn merge-some [a b]
  (merge-with #(or %2 %1) a b))

(defn find [pred xs]
  (reduce (fn [_ x] (when (pred x) (reduced x))) nil xs))

(defn find-by [key-fn key xs]
  (reduce (fn [_ x] (when (= key (key-fn x)) (reduced x))) nil xs))

(defn repeatedlyv [n f]
  (into [] (repeatedly n f)))

(defn without [pred coll]
  (persistent!
    (reduce
      (fn [coll el]
        (if (pred el)
          coll
          (conj! coll el)))
      (transient (empty coll))
      coll)))

(defn slurp-bytes ^bytes [src]
  (if (bytes? src)
    src
    (with-open [is (io/input-stream src)]
      (.readAllBytes is))))

(defn lazy-resource [path]
  (delay
    (slurp-bytes
      (io/resource (str "io/github/humbleui/" path)))))

(defn ipoint ^IPoint [^long x ^long y]
  (IPoint. x y))

(defn isize ^IPoint [^long x ^long y]
  (IPoint. x y))

(defn point ^Point [x y]
  (Point. x y))

(defn irange ^IRange [^long start ^long end]
  (IRange. start end))

(defn irect-xywh ^IRect [^long x ^long y ^long w ^long h]
  (IRect/makeXYWH x y w h))

(defn irect-ltrb ^IRect [^long l ^long t ^long r ^long b]
  (IRect/makeLTRB l t r b))

(defn rect-xywh ^Rect [x y w h]
  (Rect/makeXYWH x y w h))

(defn rect-ltrb ^Rect [l t r b]
  (Rect/makeLTRB l t r b))

(defn rrect-xywh
  (^RRect [x y w h r]
    (RRect/makeXYWH x y w h r))
  (^RRect [x y w h xr yr]
    (RRect/makeXYWH x y w h xr yr))
  (^RRect [x y w h tr tl br bl]
    (RRect/makeXYWH x y w h tr tl br bl)))

(defn rrect-complex-xywh ^RRect [x y w h radii]
  (RRect/makeComplexXYWH x y w h (into-array Float/TYPE radii)))

(defn rrect-ltrb
  (^RRect [l t r b radius]
    (RRect/makeLTRB l t r b radius))
  (^RRect [l t r b xr yr]
    (RRect/makeLTRB l t r b xr yr))
  (^RRect [l t r b tr tl br bl]
    (RRect/makeLTRB l t r b tr tl br bl)))

(defn irect ^IRect [^Rect rect]
  (.toIRect rect))

(defn rect ^Rect [^IRect irect]
  (.toRect irect))

(defn rect-contains? [rect point]
  {:pre [(some? rect)
         (some? point)]}
  (and
    (<= (:x rect) (:x point))
    (< (:x point) (:right rect))
    (<= (:y rect) (:y point))
    (< (:y point) (:bottom rect))))

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

(defn arities [f]
    (let [methods  (.getDeclaredMethods (class f))
          fixed    (->> methods
                     (filter #(= "invoke" (.getName ^java.lang.reflect.Method %)))
                     (map #(.getParameterCount ^java.lang.reflect.Method %))
                     (sort)
                     (vec))
          vararg   (->> methods
                     (find #(= "getRequiredArity" (.getName ^java.lang.reflect.Method %))))
          required (when vararg
                     (.invoke ^java.lang.reflect.Method vararg f (make-array 0)))]
      (some-map
        :fixed fixed
        :vararg required)))

(defn- timer-task ^TimerTask [f]
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
    (uncaughtException [_ _ ex]
      (log-error ^Throwable ex))))

(defmacro thread [& body]
  `(future
     (try
       ~@body
       (catch Throwable t#
         (log-error t#)))))

(defmacro import-vars
  "Makes an “alias” in current namespace for var from external namespace.
   Keeps :arglists meta"
  [& aliases]
  (list*
    'do
    (for [alias aliases
          :let [var  (resolve alias)
                meta {:arglists
                      (list 'quote (:arglists (meta var)))}
                sym' (with-meta (symbol (name alias)) meta)]]
      (list 'def sym' alias))))

(def ^:private lock
  (Object.))

(def t0
  (System/currentTimeMillis))

(defn log [& args]
  (locking lock
    (let [dt    (- (System/currentTimeMillis) t0)
          mins  (quot dt 60000)
          secs  (mod (quot dt 1000) 60)
          msecs (mod dt 1000)]
      (apply println (format "%02d:%02d.%03d" mins secs msecs) args))
    (flush)))

;; deftype+

(defn- signature [method]
  (let [[sym args & _] method]
    [sym (count args)]))

(defn- qualify-symbol
  "Converts symbol to fully-qualified form in current namespace via resolve"
  [s]
  (let [o (resolve s)]
    (cond
      (nil? o)   s
      (class? o) (symbol (.getName ^Class o))
      (var? o)   (symbol o)
      :else      s)))

(defn- update-method
  "Fully qualifies all namespaced symbols in method bodies.
   Converts typed argument lists to untyped ones + top-level let with tags"
  [[name args & body]]
  (let [remove-tag      #(vary-meta % dissoc :tag)
        untyped-args    (mapv remove-tag args)
        typed-args      (filter #(:tag (meta %)) args)
        bindings        (vec (mapcat #(list % (remove-tag %)) typed-args))
        qualified-body  (clojure.walk/postwalk
                          (fn [x]
                            (if (and (symbol? x) (namespace x))
                              (qualify-symbol x)
                              x))
                          body)]
    (if (empty? bindings)
      (list* name untyped-args qualified-body)
      (list
        name untyped-args
        (list* 'clojure.core/let bindings
          qualified-body)))))

(defn- group-protos
  "Converts flat list of protocols and methods into a map
   {protocol -> {signature -> body}}"
  [body]
  (loop [body  body
         res   {}
         proto nil]
    (if (empty? body)
      res
      (let [[head & tail] body]
        (if (symbol? head)
          (let [proto (qualify-symbol head)]
            (recur tail res proto))
          (let [method (update-method head)
                sig    (signature method)]
            (recur tail (update res proto assoc sig method) proto)))))))

(defmacro deftype+
  "Same as deftype, but:

   1. Can “inherit” default protocols/method impls from parent (:extends)
   2. Uses ^:mut instead of ^:unsynchronized-mutable
   3. Allows using type annotations in protocol arglist
   4. Read mutable fields through ILookup: (:field instance)
   5. Write to mutable fields from outside through ISettable: (-set! instance key value)
   6. Allow with-meta"
  [name fields & body]
  (let [[parent body] (if (= :extends (first body))
                        (let [[_ sym & body'] body]
                          [(or (some-> sym resolve deref)
                             (throw (ex-info (str "Can't resolve symbol: " sym) {:symbol sym})))
                           body'])
                        [nil body])
        update-field  #(vary-meta % set/rename-keys {:mut :unsynchronized-mutable})
        fields        (->> 
                        (concat fields (:fields parent))
                        (mapv update-field))
        mut-fields    (filter #(:unsynchronized-mutable (meta %)) fields)
        protos        (->>
                        (merge-with merge
                          (:protocols parent)
                          (group-protos body))
                        (map-vals vals))
        value-sym     (gensym 'value)]
    `(do
       (deftype ~name
         ~(conj fields '__m)
         
         ~@(for [[proto methods] protos
                 form (cons proto methods)]
             form)
       
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
             ~@(mapcat #(vector (keyword %) (list 'set! % value-sym)) mut-fields))))
       
       (defn ~(symbol (str '-> name)) ~fields
         (new ~name ~@fields nil)) ;; __m
       
       (defn ~(symbol (str 'map-> name)) [m#]
         (let [{:keys ~fields} m#]
           (new ~name ~@fields nil))))))

;; prototypes

(defn set!! [obj & kvs]
  (doseq [[k v] (partition 2 kvs)]
    (protocols/-set! obj k v))
  obj)

(defn measure [comp ctx ^IPoint cs]
  {:pre  [(instance? IPoint cs)]
   :post [(instance? IPoint %)]}
  (when comp
    (protocols/-measure comp ctx cs)))

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

(defn iterate-child [comp ctx cb]
  (when comp
    (protocols/-iterate comp ctx cb)))

(defn child-close [child]
  (when (instance? AutoCloseable child)
    (.close ^AutoCloseable child)))

(defmacro defparent
  "Defines base “class” that deftype+ can extend from.
   Supports extra field and protocols which deftype+ can partially override.
   If calling external functions, use fully-qualified or namespaced symbol"
  [sym & body]
  (let [[doc body]    (if (string? (first body)) [(first body) (next body)] ["" body])
        [fields body] [(first body) (next body)]
        [parent body] (if (= :extends (first body)) [(second body) (nnext body)] [nil body])
        parent        (when parent
                        (or (some-> parent resolve deref)
                          (throw (ex-info (str "Can't resolve symbol: " parent) {:symbol symbol}))))
        fields    (vec (concat (:fields parent) fields))
        protocols (merge-with merge (:protocols parent) (group-protos body))]
    `(def ~sym ~doc
       {:fields    (quote ~fields)
        :protocols (quote ~protocols)})))

(alias 'core 'io.github.humbleui.core)

(defparent ATerminal
  "Simple component that has no children"
  []
  protocols/IComponent
  (-measure [_ _ cs]
    (core/ipoint 0 0))
  (-draw [_ _ _ _])
  (-event [_ _ _])
  (-iterate [this _ cb]
    (cb this)))

(defparent AWrapper
  "A component that has exactly one child"
  [child ^:mut child-rect]
  protocols/IContext
  (-context [_ ctx]
    ctx)

  protocols/IComponent
  (-measure [this ctx cs]
    (when-some [ctx' (protocols/-context this ctx)]
      (core/measure child ctx' cs)))
  
  (-draw [this ctx rect canvas]
    (when-some [ctx' (protocols/-context this ctx)]
      (set! child-rect rect)
      (core/draw-child child ctx' rect canvas)))

  (-event [this ctx event]
    (when-some [ctx' (protocols/-context this ctx)]
      (core/event-child child ctx' event)))

  (-iterate [this ctx cb]
    (or
      (cb this)
      (when-some [ctx' (protocols/-context this ctx)]
        (core/iterate-child child ctx' cb))))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defparent AContainer
  "A component that has multiple children"
  [children]
  protocols/IComponent
  (-event [this ctx event]
    (reduce
      (fn [acc child]
        (core/eager-or acc
          (core/event-child child ctx event)))
      false
      children))

  (-iterate [this ctx cb]
    (or
      (cb this)
      (some #(core/iterate-child % ctx cb) children)))
  
  AutoCloseable
  (close [_]
    (doseq [child children]
      (core/child-close child))))
