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
    (let [child' (child-ctor ctx)]
      (when-not (identical? child child')
        (core/child-close child)
        (set! child child')))
    (if (instance? Throwable child)
      cs
      (core/measure child ctx cs)))
  
  (-draw [_ ctx rect canvas]
    (let [child' (child-ctor ctx)]
      (when-not (identical? child child')
        (core/child-close child)
        (set! child child')))
    (set! child-rect rect)
    (if (instance? Throwable child)
      (.drawRect canvas (.toRect rect) (paint/fill 0xFFCC3333))
      (core/draw-child child ctx child-rect canvas)))
  
  (-event [_ ctx event]
    (when-not (instance? Throwable child)
      (core/event-child child ctx event)))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (when-not (instance? Throwable child)
        (protocols/-iterate child ctx cb))))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn contextual [child-ctor]
  (->Contextual
    #(try
       (child-ctor %)
       (catch Throwable t
         (core/log-error t)
         t))
    nil nil))

(defn dynamic-impl [ctx-sym bindings body]
  (let [syms (core/bindings->syms bindings)]
    `(let [inputs-fn# (core/memoize-last (fn [~@syms] ~@body))]
       (contextual
         (fn [~ctx-sym]
           (let [~@bindings]
             (inputs-fn# ~@syms)))))))

(defmacro dynamic [ctx-sym bindings & body]
  (dynamic-impl ctx-sym bindings body))