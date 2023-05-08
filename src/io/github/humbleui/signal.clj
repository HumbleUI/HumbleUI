(ns io.github.humbleui.signal
  (:refer-clojure :exclude [mapv reset! swap!])
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
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

(defn disj-output [signal output]
  (let [outputs  (:outputs signal)
        outputs' (core/without #(identical? (read-ref %) output) outputs)]
    (protocols/-set! signal :outputs outputs')))
      
(defn- set-state! [signal state]
  ; (core/log "set-state!" (:value signal) state)
  (when (and
          (= :eager (:type signal))
          *effects*
          (not= :clean state))
    (vswap! *effects* conj signal))
  (when (not= state (:state signal))
    (protocols/-set! signal :state state)
    (when-not (= :clean state)
      (doouts [out (:outputs signal)]
        (set-state! out :check)))))

(defn- reset-impl! [signal value' cache']
  (protocols/-set! signal :state :clean)
  (when (not= (:value signal) value')
    (protocols/-set! signal :value value')
    (protocols/-set! signal :cache cache')
    (doouts [out (:outputs signal)]
      (set-state! out :dirty)))
  value')

(defn- read-dirty [signal]
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
            (protocols/-set! input :outputs outputs')))
        (protocols/-set! signal :inputs inputs')
        (reset-impl! signal value' cache')))))

(defn- read-check [signal]
  (loop [inputs (:inputs signal)]
    (if (empty? inputs)
      (do
        (protocols/-set! signal :state :clean)
        (:value signal))
      (do
        (binding [*context* nil]
          @(first inputs))
        (if (= :dirty (:state signal))
          (read-dirty signal)
          (recur (next inputs)))))))

;; User APIs

;; TODO synchronize
(core/deftype+ Signal [name value-fn ^:mut value ^:mut cache ^:mut inputs ^:mut outputs ^:mut state type]
  Object
  (toString [_]
    (str "#Signal{name=" name ", state=" state ", value=" value "}"))
  IDeref
  (deref [this]
    (when *context*
      (vswap! *context* conj! this))
    (case state
      :clean    value
      :dirty    (read-dirty this)
      :check    (read-check this)
      :disposed (throw (ex-info "Can't read disposed signal" {})))))

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
  `(signal* "anon-signal" (fn [~'_ ~'_] {:value (do ~@body)})))

(defmacro defsignal [name & body]
  `(def ~name
     (signal* (quote ~name) (fn [~'_ ~'_] {:value (do ~@body)}))))

(defn maybe-read [signal-or-value]
  (if (instance? Signal signal-or-value)
    @signal-or-value
    signal-or-value))

(defn reset! [signal value']
  (let [*effects (volatile! #{})]
    ;; clear out all dependencies
    (doseq [input (:inputs signal)]
      (disj-output input signal))
    (protocols/-set! signal :inputs #{})
    ;; change value and collect all triggered effects
    (binding [*effects* *effects]
      (reset-impl! signal value' nil))
    ;; execute effects
    (doseq [effect @*effects]
      @effect)))

(defn swap! [signal f & args]
  (reset! signal (apply f @signal args)))

(defn dispose! [& signals]
  (doseq [signal signals]
    (doseq [input (:inputs signal)]
      (disj-output input signal))
    (doto signal
      (protocols/-set! :state :disposed)
      (protocols/-set! :value nil)
      (protocols/-set! :cache nil))))

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
       (protocols/-set! input# :outputs (conj outputs# (make-ref signal#))))
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
