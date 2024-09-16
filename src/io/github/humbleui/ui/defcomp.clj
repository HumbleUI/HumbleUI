(in-ns 'io.github.humbleui.ui)

(def ^:private *comp-key
  (volatile! 0))

(defn- auto-keys [form]
  (walk/postwalk
    (fn [form]
      (if (and (vector? form) (instance? clojure.lang.IObj form))
        (let [m (meta form)]
          (if (contains? m :key)
            form
            (vary-meta form assoc :key (str "io.github.humbleui.ui/" (vswap! *comp-key inc)))))
        form))
    form))

(defmacro defcomp
  "Define component. Similar to defn. Can return:
   
   - Markup
   - Anonymous render function that returns markup. Arglists must match
   - A map with following keys:
   
     :should-setup?   :: (fn [<arglist>])
     :should-render?  :: (fn [<arglist>])
     :after-mount     :: (fn [])
     :before-render   :: (fn [])
     :user-measure    :: (fn [comp constraints])
     :user-draw       :: (fn [child bounds container-size viewport canvas])
     :render          :: (fn [<arglist>])
     :after-render    :: (fn [])
     :before-draw     :: (fn [])
     :after-draw      :: (fn [])
     :after-unmount   :: (fn [])" 
  [name & fdecl]
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

(alter-meta! #'defcomp
  assoc :arglists '([name doc-string? attr-map? [params*] prepost-map? body]))
