(in-ns 'io.github.humbleui.ui)

(util/deftype+ Contextual [child-ctor ^:mut child ^:mut child-bounds]
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [child' (child-ctor ctx)]
      (when-not (identical? child child')
        (unmount child)
        (set! child child')))
    (if (instance? Throwable child)
      cs
      (measure child ctx cs)))
  
  (-draw [_ ctx bounds viewport canvas]
    (let [child' (child-ctor ctx)]
      (when-not (identical? child child')
        (unmount child)
        (set! child child')))
    (set! child-bounds bounds)
    (if (instance? Throwable child)
      (canvas/draw-rect canvas bounds (paint/fill 0xFFCC3333))
      (draw child ctx child-bounds viewport canvas)))
  
  (-event [_ ctx event]
    (when-not (instance? Throwable child)
      (ui/event child ctx event)))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (when-not (instance? Throwable child)
        (iterate child ctx cb))))
  
  (-unmount [_]
    (unmount child)))

(defn contextual [child-ctor]
  (map->Contextual
    {:child-ctor
     #(try
        (child-ctor %)
        (catch Throwable t
          (util/log-error t)
          t))}))

(defn- dynamic-impl [ctx-sym bindings body]
  (let [syms (util/bindings->syms bindings)]
    `(let [inputs-fn# (util/memoize-last (fn [~@syms] ~@body))]
       (contextual
         (fn [~ctx-sym]
           (let [~@bindings]
             (inputs-fn# ~@syms)))))))

(defmacro dynamic [ctx-sym bindings & body]
  (dynamic-impl ctx-sym bindings body))

(defn with-scale-impl [sym body]
  `(dynamic ctx# [~sym (:scale ctx#)]
     ~@body))
