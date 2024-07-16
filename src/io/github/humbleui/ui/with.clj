(in-ns 'io.github.humbleui.ui)

(defn normalize-mixin [x]
  (cond
    (fn? x)
    [{:render x}]
    
    (map? x)
    [x]
    
    (and (sequential? x) (map? (first x)))
    x
    
    :else
    (throw (ex-info (str "Malformed mixin: " (pr-str x)) {:value x}))))

(defn with-impl [sym form body]
  `(let [form#  ~form
         ~sym   (:value form#)
         mixin# (dissoc form# :value)
         res#   ~body]
     (reduce merge-mixins
       (concat
         (normalize-mixin mixin#)
         (normalize-mixin res#)))))

(defmacro with [bindings & body]
  (if (= 0 (count bindings))
    `(do ~@body)
    (with-impl (first bindings) (second bindings)
      `(with ~(nnext bindings)
         ~@body))))
