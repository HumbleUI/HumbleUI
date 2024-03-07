(ns io.github.humbleui.signal
  (:refer-clojure :exclude [mapv reset! swap!])
  (:require
    [io.github.humbleui.protocols :as protocols]
    [extend-clj.core :as extend-clj])
  (:import
    [clojure.lang IDeref]
    [java.lang.ref Reference WeakReference]))

(def ^:private ^:dynamic *context*
  nil)

(def ^:private ^:dynamic *effects*
  nil)

(defn make-ref [val]
  (WeakReference. val))

(defn read-ref [^WeakReference ref]
  (.get ref))

(defmacro doouts [[sym outputs] & body]
  `(doseq [ref# ~outputs
           :let [~sym (read-ref ref#)]
           :when (some? ~sym)]
     ~@body))

(defn without [pred coll]
  (persistent!
    (reduce
      (fn [coll el]
        (if (pred el)
          coll
          (conj! coll el)))
      (transient (empty coll))
      coll)))

(defn set!! [obj & kvs]
  (doseq [[k v] (partition 2 kvs)]
    (protocols/-set! obj k v))
  obj)

(defn disj-output [signal output]
  (let [outputs  (:outputs signal)
        outputs' (without #(identical? (read-ref %) output) outputs)]
    (set!! signal :outputs outputs')))
      
(defn- set-state! [signal state]
  ; (core/log "set-state!" (:name signal) state)
  (when (and
          (= :eager (:type signal))
          *effects*
          (not= :clean state))
    (vswap! *effects* conj signal))
  (when (not= state (:state signal))
    (set!! signal :state state)
    (when-not (= :clean state)
      (doouts [out (:outputs signal)]
        (set-state! out :check)))))

(defn- reset-impl! [signal value' cache']
  ; (core/log "reset-impl!" (:name signal) value' cache')
  (set!! signal :state :clean)
  (when (not= (:value signal) value')
    (set!! signal
      :value value'
      :cache cache')
    (doouts [out (:outputs signal)]
      (set-state! out :dirty)))
  value')

(defn- read-dirty [signal]
  ; (core/log "read-dirty" (:name signal))
  (let [*context (volatile! (transient #{}))
        {value' :value
         cache' :cache} (binding [*context* *context]
                          ((:value-fn signal) (:value signal) (:cache signal)))]
    (when-not (= :eager (:type signal))
      (let [inputs  (:inputs signal)
            inputs' (persistent! @*context)
            _       (assert (every? #(= :lazy (:type %)) inputs'))
            ref     (make-ref signal)]
        ;; remove from inputs we don’t reference
        (doseq [input inputs
                :when (not (inputs' input))]
          (disj-output input signal))
        ;; add to newly acquired inputs
        (doseq [input inputs'
                :when (not (inputs input))]
          (let [outputs  (:outputs input)
                outputs' (conj outputs ref)]
            (set!! input :outputs outputs')))
        (set!! signal :inputs inputs')
        (reset-impl! signal value' cache')))))

(defn- read-check [signal]
  ; (core/log "read-check" (:name signal))
  (loop [inputs (:inputs signal)]
    (if (empty? inputs)
      (do
        (set!! signal :state :clean)
        (:value signal))
      (do
        (binding [*context* nil]
          @(first inputs))
        (if (= :dirty (:state signal))
          (read-dirty signal)
          (recur (next inputs)))))))

;; User APIs
(declare reset!)
;; TODO synchronize
(extend-clj/deftype-atom Signal
  [name
   value-fn
   ^:volatile-mutable value
   ^:volatile-mutable cache
   ^:volatile-mutable inputs
   ^:volatile-mutable outputs
   ^:volatile-mutable state
   type]

  (deref-impl [this]
    (when *context*
      (vswap! *context* conj! this))
    (case state
      :clean    value
      :dirty    (read-dirty this)
      :check    (read-check this)
      :disposed (throw (ex-info (str "Can't read disposed signal '" name "'") {}))))

  ;; TODO check oldv
  (compare-and-set-impl [this oldv newv]
    (let [*effects (volatile! #{})]
      ;; clear out all dependencies
      (doseq [input inputs]
        (disj-output input this))
      (set! inputs #{})
      ;; change value and collect all triggered effects
      (binding [*effects* *effects]
        (reset-impl! this newv nil))
      ;; execute effects
      (doseq [effect @*effects]
        @effect))
    true)
  
  protocols/ISettable
  (-set! [_ k v]
    (case k
      :value   (set! value v)
      :cache   (set! cache v)
      :inputs  (set! inputs v)
      :outputs (set! outputs v)
      :state   (set! state v)))
  
  (toString [this]
    (str "#signal[name=" (or name (Integer/toHexString (System/identityHashCode this))) " value=" value "]")))

(defn map->Signal [m]
  (->Signal
    (:name m)
    (:value-fn m)
    (:value m)
    (:cache m)
    (:inputs m)
    (:outputs m)
    (:state m)
    (:type m)))

(defmethod print-method Signal [o ^java.io.Writer w]
  (.write w (str o)))

(defn signal* [name value-fn]
  (let [signal (map->Signal
                 {:name     name
                  :value-fn value-fn
                  :inputs   #{}
                  :outputs  #{}
                  :state    :dirty
                  :type     :lazy})]
    (read-dirty signal) ;; force deps
    signal))

(defmacro signal-named
  "Observable derived computation"
  [name & body]
  `(signal* ~name (fn [~'_ ~'_] {:value (do ~@body)})))

(defmacro signal
  "Observable derived computation"
  [& body]
  `(signal* nil (fn [~'_ ~'_] {:value (do ~@body)})))

(defmacro defsignal [name & body]
  `(def ~name
     (signal* (quote ~name) (fn [~'_ ~'_] {:value (do ~@body)}))))

(defn signal? [x]
  (instance? Signal x))

(defn maybe-read [signal-or-value]
  (if (instance? Signal signal-or-value)
    @signal-or-value
    signal-or-value))

(defn reset! [signal value']
  (clojure.core/reset! signal value'))

(defn reset-changed! [signal value']
  (when (not= value' @signal)
    (clojure.core/reset! signal value')))

(defn swap! [signal f & args]
  (apply clojure.core/swap! signal f args))

(defn dispose! [& signals]
  (doseq [signal signals]
    (doseq [input (:inputs signal)]
      (disj-output input signal))
    (set!! signal
      :state :disposed
      :value nil
      :cache nil))
  nil)

(defmacro effect-named [name inputs & body]
  `(let [inputs# (->> ~inputs
                   (filter #(instance? Signal %)))
         signal# (map->Signal
                   {:name     ~name
                    :value-fn (fn [_# _#]
                                ~@body)
                    :inputs   (set inputs#)
                    :outputs  #{}
                    :state    :clean
                    :type     :eager})]
     (doseq [input# inputs#
             :let [outputs# (:outputs input#)]]
       (set!! input# :outputs (conj outputs# (make-ref signal#))))
     signal#))

(defmacro effect [inputs & body]
  `(effect-named "anon-effect" ~inputs ~@body))

(defn mapv [f *xs]
  (signal*
    :mapv
    (fn [old-val cache]
      (let [xs      @*xs
            mapping (into {} (map vector cache old-val))
            xs'     (clojure.core/mapv #(or (mapping %) (binding [*context* nil] (f %))) xs)]
        {:cache xs
         :value xs'}))))
