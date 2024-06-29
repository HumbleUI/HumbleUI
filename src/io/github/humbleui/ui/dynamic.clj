(in-ns 'io.github.humbleui.ui)

(core/deftype+ Contextual [child-ctor ^:mut child ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [child' (child-ctor ctx)]
      (when-not (identical? child child')
        (unmount-child child)
        (set! child child')))
    (if (instance? Throwable child)
      cs
      (measure child ctx cs)))
  
  (-draw [_ ctx rect canvas]
    (let [child' (child-ctor ctx)]
      (when-not (identical? child child')
        (unmount-child child)
        (set! child child')))
    (set! child-rect rect)
    (if (instance? Throwable child)
      (canvas/draw-rect canvas rect (paint/fill 0xFFCC3333))
      (draw-child child ctx child-rect canvas)))
  
  (-event [_ ctx event]
    (when-not (instance? Throwable child)
      (event-child child ctx event)))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (when-not (instance? Throwable child)
        (protocols/-iterate child ctx cb))))
  
  (-unmount [_]
    (unmount-child child)))

(defn contextual [child-ctor]
  (->Contextual
    #(try
       (child-ctor %)
       (catch Throwable t
         (core/log-error t)
         t))
    nil nil))

(defn- dynamic-impl [ctx-sym bindings body]
  (let [syms (core/bindings->syms bindings)]
    `(let [inputs-fn# (core/memoize-last (fn [~@syms] ~@body))]
       (contextual
         (fn [~ctx-sym]
           (let [~@bindings]
             (inputs-fn# ~@syms)))))))

(defmacro dynamic [ctx-sym bindings & body]
  (dynamic-impl ctx-sym bindings body))

(defn with-scale-impl [sym body]
  `(dynamic ctx# [~sym (:scale ctx#)]
     ~@body))
