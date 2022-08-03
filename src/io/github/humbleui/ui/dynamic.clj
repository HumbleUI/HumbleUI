(ns io.github.humbleui.ui.dynamic
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [java.lang AutoCloseable]))

;; contextual / dynamic

(core/deftype+ Contextual [child-ctor ^:mut child ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (try
      (let [child' (child-ctor ctx)]
        (when-not (identical? child child')
          (core/child-close child)
          (set! child child')))
      (core/measure child ctx cs)
      (catch Throwable e
        (.printStackTrace e)
        cs)))
  
  (-draw [_ ctx rect canvas]
    (try
      (let [child' (child-ctor ctx)]
        (when-not (identical? child child')
          (core/child-close child)
          (set! child child')))
      (set! child-rect rect)
      (core/draw-child child ctx child-rect canvas)
      (catch Throwable e
        (.drawRect canvas (.toRect rect) (paint/fill 0xFFCC3333))
        (.printStackTrace e))))
  
  (-event [_ event]
    (core/event-child child event))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn contextual [child-ctor]
  (->Contextual child-ctor nil nil))

(defn bindings->syms [bindings]
  (->> bindings
    (partition 2)
    (map first)
    (core/collect symbol?)
    (map name)
    (map symbol)
    (into #{})
    (vec)))

(defn dynamic-impl [ctx-sym bindings body]
  (let [syms (bindings->syms bindings)]
    `(let [inputs-fn# (core/memoize-last (fn [~@syms] ~@body))]
       (contextual
         (fn [~ctx-sym]
           (let [~@bindings]
             (inputs-fn# ~@syms)))))))

(defmacro dynamic [ctx-sym bindings & body]
  (dynamic-impl ctx-sym bindings body))