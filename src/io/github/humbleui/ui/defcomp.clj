(in-ns 'io.github.humbleui.ui)

(def ^:private *key
  (volatile! 0))

(defn- auto-keys [form]
  (walk/postwalk
    (fn [form]
      (if (and (vector? form) (instance? clojure.lang.IObj form))
        (let [m (meta form)]
          (if (contains? m :key)
            form
            (vary-meta form assoc :key (str "io.github.humbleui.ui/" (vswap! *key inc)))))
        form))
    form))

(defmacro defcomp [name & fdecl]
  (let [[m fdecl] (if (string? (first fdecl))
                    [{:doc (first fdecl)} (next fdecl)]
                    [{} fdecl])
        [m fdecl] (if (map? (first fdecl))
                    [(merge m (first fdecl)) (next fdecl)]
                    [m fdecl])
        fdecl     (if (vector? (first fdecl))
                    (list fdecl)
                    fdecl)
        [m fdecl] (if (map? (last fdecl))
                    [(merge m (last fdecl)) (butlast fdecl)]
                    [m fdecl])]
    `(defn ~(vary-meta name merge m)
       ~@(for [[params & body] fdecl]
           (list params
             `(let [~'&node *node*]
                ~@(auto-keys body)))))))
