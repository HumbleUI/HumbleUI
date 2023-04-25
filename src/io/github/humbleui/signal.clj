(ns io.github.humbleui.signal
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [clojure.lang IDeref]
    [java.lang.ref Reference WeakReference]))

(def ^:private ^:dynamic *context*
  nil)

(defmacro doouts [[sym outputs] & body]
  `(doseq [^Reference ref# ~outputs
           :let [~sym (.get ref#)]
           :when (some? ~sym)]
     ~@body))
      
(defn- set-state! [signal state]
  (when (not= state (:state signal))
    (protocols/-set! signal :state state)
    (when-not (= :clean state)
      (doouts [out (:outputs signal)]
        (set-state! out :check)))))

(defn- mutate-impl! [signal value']
  (protocols/-set! signal :state :clean)
  (when (not= (:value signal) value')
    (protocols/-set! signal :value value')
    (doouts [out (:outputs signal)]
      (set-state! out :dirty)))
  value')

(defn- read-dirty [signal]
  (let [*context (volatile! (transient #{}))
        value'   (binding [*context* *context]
                   ((:value-fn signal)))
        inputs   (:inputs signal)
        inputs'  (persistent! @*context)
        ref      (WeakReference. signal)]
    ;; remove from inputs we don’t reference
    (doseq [input inputs
            :when (not (inputs' input))]
      (let [outputs  (:outputs input)
            outputs' (core/without #(identical? (.get ^Reference %) signal) outputs)]
        (protocols/-set! input :outputs outputs')))
    ;; add to newly acquired inputs
    (doseq [input inputs'
            :when (not (inputs input))]
      (let [outputs  (:outputs input)
            outputs' (conj outputs ref)]
        (protocols/-set! input :outputs outputs')))
    (protocols/-set! signal :inputs inputs')
    (mutate-impl! signal value')))

(defn- read-check [signal]
  (loop [inputs (:inputs signal)]
    (if (empty? inputs)
      (do
        (protocols/-set! signal :state :clean)
        (:value signal))
      (do
        @(first inputs)
        (if (= :dirty (:state signal))
          (read-dirty signal)
          (recur (next inputs)))))))

;; User APIs

;; TODO synchronize
(core/deftype+ Signal [value-fn ^:mut value ^:mut inputs ^:mut outputs ^:mut state]
  Object
  (toString [_]
    (str "#Signal{value=" value ", state=" state "}"))
  IDeref
  (deref [this]
    (when *context*
      (vswap! *context* conj! this))
    (case state
      :clean value
      :dirty (read-dirty this)
      :check (read-check this))))
                  
(defn signal 
  "Observable mutable state"
  [initial]
  (map->Signal
    {:value   initial
     :outputs #{}
     :state   :clean}))

(defmacro computed
  "Observable derived computation"
  [& body]
  `(map->Signal
     {:value-fn (fn []
                  ~@body)
      :inputs   #{}
      :outputs  #{}
      :state    :dirty}))

(defn mutate! [signal value']
  (assert (nil? (:inputs signal)) "Only source signals can be directly mutated")
  (mutate-impl! signal value'))

;; TODO effect
