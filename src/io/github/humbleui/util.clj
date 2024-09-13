(ns io.github.humbleui.util
  (:refer-clojure :exclude [find flatten])
  (:require
    [clojure.java.io :as io]
    [clojure.math :as math]
    [clojure.set :as set]
    [clojure.walk :as walk]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.error :as error]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [io.github.humbleui.skija Canvas]
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

(def ^Timer timer
  (Timer. true))

(def ^{:arglists '([^Throwable t])} log-error
  error/log-error)

(defmacro catch-and-log [& body]
  `(try
     ~@body
     (catch Throwable t#
       (log-error t#))))

(def ^:dynamic *text-input-ctx*)

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

(defmacro if-some+ [bindings then else]
  `(let ~bindings
     (if (every? some? ~(bindings->syms bindings))
       ~then
       ~else)))

(defmacro when-some+ [bindings & body]
  `(let ~bindings
     (when (every? some? ~(bindings->syms bindings))
       ~@body)))

(defn close [o]
  (when (instance? AutoCloseable o)
    (.close ^AutoCloseable o)))

(defmacro memo-fn [bindings & body]
  (let [syms (as-> bindings %
               (collect symbol? %)
               (map without-ns %)
               (into #{} %)
               (disj % '& '_)
               (vec %))
        gens (mapv (fn [_] (gensym)) (range (count bindings)))]
    `(let [*state# (atom nil)]
       (fn ~gens
         (let ~(vec (mapcat vector bindings gens))
           (let [state#         @*state#
                 [args# value#] state#]
             (if (= args# ~syms)
               value#
               (let [_#     (close value#)
                     value# (do ~@body)]
                 (reset! *state# [~syms value#])
                 value#))))))))

(def ^:private ^:dynamic *if+-syms)
  
(defn- if+-rewrite-cond-impl [cond]
  (clojure.core/cond
    (empty? cond)
    true
    
    (and
      (= :let (first cond))
      (empty? (second cond)))
    (if+-rewrite-cond-impl (nnext cond))
    
    (= :let (first cond))
    (let [[var val & rest] (second cond)
          sym                (gensym)]
      (vswap! *if+-syms conj [var sym])
      (list 'let [var (list 'clojure.core/vreset! sym val)]
        (if+-rewrite-cond-impl
          (cons 
            :let
            (cons rest
              (nnext cond))))))
    
    :else
    (list 'and
      (first cond)
      (if+-rewrite-cond-impl (next cond)))))

(defn- if+-rewrite-cond [cond]
  (binding [*if+-syms (volatile! [])]
    [(if+-rewrite-cond-impl cond) @*if+-syms]))

(defn- flatten-1 [xs]
  (vec
    (mapcat identity xs)))

(defmacro if+
  "Allows sharing local variables between condition and then clause.
      
      Use `:let [...]` form (not nested!) inside `and` condition and its bindings
      will be visible in later `and` clauses and inside `then` branch:
      
        (if+ (and
               (= 1 2)
               ;; same :let syntax as in doseq/for
               :let [x 3
                     y (+ x 4)]
               ;; x and y visible downstream
               (> y x))
          
          ;; then: x and y visible here!
          (+ x y 5)
          
          ;; else: no x or y
          6)"
  [cond then else]
  (if (and
        (seq? cond)
        (or
          (= 'and (first cond))
          (= 'clojure.core/and (first cond))))
    (let [[cond' syms] (if+-rewrite-cond (next cond))]
      `(let ~(flatten-1
               (for [[_ sym] syms]
                 [sym '(volatile! nil)]))
         (if ~cond'
           (let ~(flatten-1
                   (for [[binding sym] syms]
                     [binding (list 'deref sym)]))
             ~then)
           ~else)))
    (list 'if cond then else)))

(defmacro cond+ [& clauses]
  (when-some [[test expr & rest] clauses]
    (condp = test
      :do   `(do  ~expr (cond+ ~@rest))
      :let  `(let ~expr (cond+ ~@rest))
      :some `(or  ~expr (cond+ ~@rest))
      `(if+ ~test ~expr (cond+ ~@rest)))))

(defmacro loop+ [bindings & body]
  (let [bindings (partition 2 bindings)
        syms     (map first bindings)
        vals     (map second bindings)
        gensyms  (map #(gensym (name %)) syms)]
    `(loop ~(vec (interleave gensyms vals))
       (let ~(vec (interleave syms gensyms))
         ~@(clojure.walk/postwalk
             (fn [form]
               (if (and (list? form) (= 'recur (first form)))
                 (let [bindings' (or (second form) [])
                       syms'     (into #{} (map first (partition 2 bindings')))]
                   `(let ~bindings'
                      (recur ~@(map (fn [sym gensym]
                                      (or (syms' sym) gensym))
                                 syms gensyms))))
                 form))
             body)))))

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

(defn invoke [f & args]
  (when f
    (apply f args)))

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

(defn some-set [& args]
  (into #{} (filter some?) args))

(defn update-some [m k f & args]
  (if-some [v (get m k)]
    (assoc m k (apply f v args))
    m))

(defn consv [x xs]
  (vec (cons x xs)))

(defn vector* [& args]
  (vec
    (concat (butlast args) (last args))))

(defn update-last [xs f & args]
  (apply update xs (dec (count xs)) f args))

(defn lastv [xs]
  (when (> (count xs) 1)
    (nth xs (dec (count xs)))))

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

(defmacro checked-get [m k pred]
  `(let [v# (get ~m ~k)]
     (if (~pred v#)
       v#
       (throw (ex-info (str ~(str "Getting (" k " " m "), expected: " pred ", got: ") (pr-str v#)) {})))))

(defmacro checked-get-optional [m k pred]
  `(when-some [v# (get ~m ~k)]
     (if (~pred v#)
       v#
       (throw (ex-info (str ~(str "Getting (" k " " m "), expected: " pred ", got: ") (pr-str v#)) {})))))

(defn map-by [f xs]
  (reduce
    (fn [m x]
      (assoc m (f x) x))
    {} xs))

(defn zip [& xs]
  (apply map vector xs))

(defn flatten [xs]
  (mapcat
    #(cond
       (nil? %)        []
       (vector? %)     [%]
       (sequential? %) (flatten %)
       :else           [%])
    xs))

(defn conjv-limited [xs x limit]
  (if (>= (count xs) limit)
    (conj (vec (take (dec limit) xs)) x)
    (conj (or xs []) x)))

(defn merge-some [& maps]
  (apply merge-with #(or %2 %1) maps))

(defn find [pred xs]
  (reduce (fn [_ x] (when (pred x) (reduced x))) nil xs))

(defn find-by [key-fn key xs]
  (reduce (fn [_ x] (when (= key (key-fn x)) (reduced x))) nil xs))

(defn repeatedlyv [n f]
  (into [] (repeatedly n f)))

(defn slurpable? [src]
  (or
    (string? src)
    (bytes? src)
    (instance? java.io.InputStream src)
    (instance? java.net.URL src)
    (instance? java.net.URI src)))

(defn slurp-bytes ^bytes [src]
  (cond
    (bytes? src)
    src
    
    (instance? java.io.InputStream src)
    (.readAllBytes ^java.io.InputStream src)
    
    :else
    (with-open [is (io/input-stream src)]
      (.readAllBytes is))))

(defn lazy-resource [path]
  (delay
    (slurp-bytes
      (io/resource (str "io/github/humbleui/" path)))))

(defn maybe-deref [x]
  (if (instance? clojure.lang.IDeref x)
    (deref x)
    x))

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

(defn irect-size [^IRect r]
  (ipoint (.getWidth r) (.getHeight r)))

(defn irect-position [^IRect r]
  (ipoint (.getLeft r) (.getTop r)))

(defn rect-xywh ^Rect [x y w h]
  (Rect/makeXYWH x y w h))

(defn rect-ltrb ^Rect [l t r b]
  (Rect/makeLTRB l t r b))

(defn rect-size [^Rect r]
  (point (.getWidth r) (.getHeight r)))

(defn rect-position [^Rect r]
  (point (.getLeft r) (.getTop r)))

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
  {:pre [(some? point)]}
  (when rect
    (and
      (<= (:x rect) (:x point))
      (< (:x point) (:right rect))
      (<= (:y rect) (:y point))
      (< (:y point) (:bottom rect)))))

(defn irect-intersect [^IRect a ^IRect b]
  (when (and a b)
    (.intersect a b)))

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
                   (.invoke ^java.lang.reflect.Method vararg f (make-array Object 0)))]
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
  (now))

(defn log [& args]
  (locking lock
    (let [dt    (- (System/currentTimeMillis) t0)
          mins  (quot dt 60000)
          secs  (mod (quot dt 1000) 60)
          msecs (mod dt 1000)]
      (apply println (format "%02d:%02d.%03d" mins secs msecs) args))
    (flush)))

(defn log-debug [ctx & args]
  (when (:debug? ctx)
    (apply log args)
    (last args)))

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

(defn- untag-symbol [sym]
  (vary-meta sym dissoc :tag))

(defn- untag-method
  "Converts typed argument lists to untyped ones + top-level let with tags"
  [method]
  (let [[name args & body] method]
    (if-some [typed-args (filter #(:tag (meta %)) args)]
      (let [untyped-args (mapv untag-symbol args)
            bindings     (vec (mapcat #(list % (untag-symbol %)) typed-args))]
        (list name untyped-args
          (list* 'clojure.core/let bindings
            body)))
      method)))

(defmacro deftype+
  "Same as deftype, but:

   1. Can “inherit” default protocols/method impls from parent (:extends)
   2. All fields ^:unsynchronized-mutable by default
   3. Allows using type annotations in protocol arglist
   4. Read mutable fields through ILookup: (:field instance)
   5. Write fields from outside through ISettable: (-set! instance key value)
   6. Allow with-meta"
  [name fields & body]
  (let [[parent body] (if (= :extends (first body))
                        (let [[_ sym & body'] body]
                          [(or (some-> sym resolve deref)
                             (throw (ex-info (str "Can't resolve parent symbol: " sym) {:symbol sym})))
                           body'])
                        [nil body])
        fields        (->> (concat fields (:fields parent))
                        (map #(vary-meta % assoc :unsynchronized-mutable true))
                        vec)
        protocols     (->> body
                        (filter symbol?)
                        (map qualify-symbol)
                        (set)
                        (set/union (:protocols parent)))
        methods       (->> body
                        (remove symbol?)
                        (map untag-method))
        signatures    (set (map signature methods))
        value-sym     (gensym 'value)]
    `(do
       (deftype ~name ~(conj fields '__m)
         ~@protocols

         ;; all parent methods
         ~@(for [[sig body] (:methods parent)
                 :when (not (signatures sig))
                 :let [[name cnt] sig
                       args (vec (repeatedly cnt gensym))]]
             `(~name ~args
                (~body ~@args)))

         ;; own methods
         ~@methods
       
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
             ~@(mapcat #(vector (keyword %) (list 'set! % value-sym)) fields))))
              
       (defn ~(symbol (str 'map-> name)) [m#]
         (let [{:keys ~fields} m#]
           (new ~name ~@fields nil))))))

;; prototypes

(defn set!!
  ([obj k v]
   (protocols/-set! obj k v)
   obj)
  ([obj k1 v1 k2 v2]
   (protocols/-set! obj k1 v1)
   (protocols/-set! obj k2 v2)
   obj)
  ([obj k1 v1 k2 v2 k3 v3]
   (protocols/-set! obj k1 v1)
   (protocols/-set! obj k2 v2)
   (protocols/-set! obj k3 v3)
   obj)
  ([obj k1 v1 k2 v2 k3 v3 & kvs]
   (protocols/-set! obj k1 v1)
   (protocols/-set! obj k2 v2)
   (protocols/-set! obj k3 v3)
   (doseq [[k v] (partition 2 kvs)]
     (protocols/-set! obj k v))
   obj))

(defn update!! [this key f & args]
  (protocols/-set! this key (apply f (get this key) args)))

(defn- merge-parents [parent child]
  {:fields    (vec (concat (:fields child) (:fields parent)))
   :protocols (set/union (:protocols parent) (:protocols child))
   :methods   (merge (:methods parent) (:methods child))})

(defmacro defparent
  "Defines base “class” that deftype+ can extend from.
   Supports extra field and protocols which deftype+ can partially override.
   If calling external functions, use fully-qualified or namespaced symbol"
  [sym & body]
  (let [[doc body]    (if (string? (first body)) [(first body) (next body)] ["" body])
        [fields body] [(first body) (next body)]
        [parent body] (if (= :extends (first body)) [(second body) (nnext body)] [nil body])
        protocols     (into (:protocols parent #{})
                        (->> body (filter symbol?) (map qualify-symbol)))
        methods       (->> body
                        (remove symbol?)
                        (map #(vector (list 'quote (signature %)) (cons 'fn %)))
                        (into {}))
        definition    {:fields    (list 'quote fields)
                       :protocols (list 'quote protocols)
                       :methods   methods}]
    (if parent
      `(def ~sym ~doc
         (#'merge-parents ~parent ~definition))
      `(def ~sym ~doc
         ~definition))))

(defn before-ns-unload []
  (.cancel timer))
