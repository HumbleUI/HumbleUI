(in-ns 'io.github.humbleui.ui)

(defn assert-arities [f g]
  (assert (= (util/arities f) (util/arities g)) (str "Arities of node fn and render fn should match, node: " (util/arities f) ", render: " (util/arities g))))

(defn merge-mixins [a b]
  (reduce-kv
    (fn [m k v]
      (let [mv (m k)]
        (cond
          (nil? mv)
          (assoc m k v)
          
          (#{:measure :draw :render :value} k)
          (throw (ex-info (str "Duplicate key in mixin maps: " k) {:left a, :right b}))
          
          (#{:should-setup? :should-render?} k)
          (assoc m k (fn [& args]
                       (or
                         (apply mv args)
                         (apply v args))))
          
          (#{:before-draw :after-draw :before-render :after-render :after-mount :after-unmount} k)
          (assoc m k (fn [& args]
                       (apply mv args)
                       (apply v args)))
          
          :else
          (throw (ex-info (str "Unexpected key in mixin: " k) {:left a, :right b})))))
    a b))

(defn make-record [^Class class]
  (let [ns   (str/replace (.getPackageName class) "_" "-")
        name (str "map->" (.getSimpleName class))
        ctor (ns-resolve (symbol ns) (symbol name))]
    (ctor {})))

(declare map->FnNode)

(defn make-impl
  ([el]
   (make-impl (map->FnNode {}) el))
  ([start-node el]
   (util/cond+
     (satisfies? protocols/IComponent el)
     el

     :let [[f & args] el
           f (maybe-deref f)]
        
     :else
     (binding [*node* start-node]
       (let [res  (cond
                    (fn? f)    (apply f args)
                    (map? f)   f
                    (class? f) (make-record f))
             node (loop [res res]
                    (util/cond+
                      (fn? res)
                      (do
                        (assert-arities f res)
                        (util/set!! *node* :render res)
                        *node*)
               
                      (satisfies? protocols/IComponent res)
                      res
                   
                      (map? res)
                      (let [render (:render res)]
                        (when (and (fn? f) render)
                          (assert-arities f render))
                        (util/set!! *node*
                          :user-measure   (:measure res)
                          :user-draw      (:draw res)
                          :render         render
                          :should-setup?  (:should-setup? res)
                          :should-render? (:should-render? res)
                          :before-draw    (:before-draw res)
                          :after-draw     (:after-draw res)
                          :before-render  (:before-render res)
                          :after-render   (:after-render res)
                          :after-mount    (:after-mount res)
                          :after-unmount  (:after-unmount res))
                        *node*)
               
                      (and (sequential? res) (map? (first res)))
                      (recur (reduce merge-mixins res))
                   
                      (sequential? res)
                      (do
                        (util/set!! *node* :render f)
                        *node*)
                     
                      :else
                      (throw (ex-info (str "Unexpected return type: " res) {:f f :args args :res res}))))]
         (util/set!! node
           :element el
           :dirty? true)
         (when-some [key (:key (meta el))]
           (util/set!! node :key key))
         (when-some [ref (:ref (meta el))]
           (reset! ref node))
         node)))))

(defn make
  ([el]
   (try
     (make-impl el)
     (catch Exception e
       (util/log-error e)
       (make-impl [@(resolve 'io.github.humbleui.ui/error) e]))))
  ([el ctx]
   (binding [*ctx* ctx]
     (make el))))

(defn should-reconcile? [ctx old-node new-el]
  (and 
    old-node
    (let [left  (maybe-deref (nth (:element old-node) 0))
          right (maybe-deref (nth new-el 0))]
      (or
        (identical? left right)
        ;; same lambdas with different captured vars still should reconcile
        (and
          (and (fn? left) (fn? right))
          (identical? (class left) (class right)))))
    (protocols/-should-reconcile? old-node ctx new-el)))


(defn autoconvert [el]
  (cond
    (string? el)
    [@(resolve 'io.github.humbleui.ui/label) el]
    
    (map? el)
    [el]
    
    (fn? el)
    [el]

    :else
    el))

(defn reconcile-many [ctx old-nodes new-els]
  (util/loop+ [old-nodes-keyed (reduce
                                 (fn [m n]
                                   (if-some [key (:key n)]
                                     (assoc! m key n)
                                     m))
                                 (transient {})
                                 old-nodes)
               old-nodes       (filter #(and % (nil? (:key %))) old-nodes)
               new-els         new-els
               res             (transient [])
               keys-idxs       (transient {})]
    (util/cond+
      (empty? new-els)
      (do
        (doseq [node (concat
                       old-nodes
                       (vals (persistent! old-nodes-keyed)))]
          (unmount node))
        (persistent! res))
      
      :let [[old-node & old-nodes'] old-nodes
            [new-el & new-els']     new-els
            new-el                  (autoconvert new-el)
            key                     (:key (meta new-el))]
      
      key
      (let [key-idx    (inc (keys-idxs key -1))
            keys-idxs' (assoc! keys-idxs key key-idx)
            key'       [key key-idx]
            new-el'    (vary-meta new-el assoc :key key')
            old-node   (old-nodes-keyed key')]
        (cond
          ;; new key
          (nil? old-node)
          (let [new-node (make new-el' ctx)]
            (recur [new-els   new-els'
                    res       (conj! res new-node)
                    keys-idxs keys-idxs']))
          
          ;; compatible key
          (should-reconcile? ctx old-node new-el')
          (do
            (protocols/-reconcile old-node ctx new-el')
            (recur [old-nodes-keyed (dissoc! old-nodes-keyed key')
                    new-els         new-els'
                    res             (conj! res old-node)
                    keys-idxs       keys-idxs']))
          
          ;; non-compatible key
          :else
          (let [new-node (make new-el' ctx)]
            (unmount old-node)
            (recur [old-nodes-keyed (dissoc! old-nodes-keyed key')
                    new-els         new-els'
                    res             (conj! res new-node)
                    keys-idxs       keys-idxs']))))

      (should-reconcile? ctx old-node new-el)
      (do
        ; (println "compatible" old-node new-el)
        (protocols/-reconcile old-node ctx new-el)
        (recur [old-nodes old-nodes'
                new-els   new-els'
                res       (conj! res old-node)]))
      
      ;; old-node was dropped
      (should-reconcile? ctx (first old-nodes') new-el)
      (let [; _ (println "old-node dropped" old-node new-el)
            _ (unmount old-node)
            [old-node & old-nodes'] old-nodes']
        (protocols/-reconcile old-node ctx new-el)
        (recur [old-nodes (next old-nodes')
                new-els   new-els'
                res       (conj! res old-node)]))
      
      ;; new-el was inserted
      (should-reconcile? ctx old-node (first new-els'))
      (let [; _ (println "new-el inserted" old-node new-el)
            new-node (make new-el ctx)]
        (recur [new-els new-els'
                res     (conj! res new-node)]))
      
      ;; just incompatible
      :else
      (let [; _ (println "incompatible" old-node new-el)
            new-node (make new-el ctx)]
        (unmount old-node)
        (recur [old-nodes old-nodes'
                new-els   new-els'
                res       (conj! res new-node)])))))
