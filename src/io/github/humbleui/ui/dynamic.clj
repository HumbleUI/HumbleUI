(ns io.github.humbleui.ui.dynamic
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [java.lang AutoCloseable]))

;; contextual / dynamic

(core/deftype+ Contextual [child-ctor ^:mut child ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [child' (child-ctor ctx)]
      (when-not (identical? child child')
        (core/child-close child)
        (set! child child')))
    (core/measure child ctx cs))
  
  (-draw [_ ctx rect canvas]
    (let [child' (child-ctor ctx)]
      (when-not (identical? child child')
        (core/child-close child)
        (set! child child')))
    (set! child-rect rect)
    (core/draw-child child ctx child-rect canvas))
  
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