(in-ns 'io.github.humbleui.ui)

(alias 'ui 'io.github.humbleui.ui)

(require
  '[io.github.humbleui.debug :as debug])

;; utils

(def ^Shaper shaper
  (Shaper/makeShapeDontWrapOrReorder))

(defn dimension ^long [size cs ctx]
  (let [scale (:scale ctx)]
    (->
      (if (fn? size)
        (* scale
          (size {:width  (/ (:width cs) scale)
                 :height (/ (:height cs) scale)
                 :scale  scale}))
        (* scale size))
      (math/round)
      (long))))

;; protocols

(defn measure [comp ctx ^IPoint cs]
  (assert (instance? IPoint cs) (str "Expected IPoint as cs, got: " cs))
  (when comp
    (let [res (protocols/-measure comp ctx cs)]
      (assert (instance? IPoint res) (str "Expected IPoint as result, got: " res))
      res)))

(defn draw [comp ctx ^IRect rect ^Canvas canvas]
  {:pre [(instance? IRect rect)]}
  (protocols/-draw comp ctx rect canvas))

(defn draw-child [comp ctx ^IRect rect ^Canvas canvas]
  (when comp
    (let [count (.getSaveCount canvas)]
      (try
        (draw comp ctx rect canvas)
        (finally
          (.restoreToCount canvas count))))))

(defn event [comp ctx event]
  (protocols/-event comp ctx event))

(defn event-child [comp ctx event]
  (when comp
    (protocols/-event comp ctx event)))

(defn iterate-child [comp ctx cb]
  (when comp
    (protocols/-iterate comp ctx cb)))

(defn unmount [comp]
  (when comp
    (protocols/-unmount comp)))

(defn unmount-child [comp]
  (when comp
    (protocols/-unmount comp)))

(defn parse-element [vals]
  (if (map? (nth vals 1))
    [(nth vals 0) (nth vals 1) (subvec vals 2)]
    [(nth vals 0) {} (subvec vals 1)]))

(defn invoke-callback [comp key & args]
  (let [[_ opts _] (parse-element (:element comp))]
    (apply core/invoke (key opts) args)))

;; vdom

(def ^:dynamic *ctx*)

(def ^:dynamic *node*)

(defn scale
  ([]
   (:scale *ctx*))
  ([ctx]
   (:scale ctx)))

(defn scaled [x]
  (when x
    (* x (:scale *ctx*))))

(defn descaled [x]
  (when x
    (/ x (:scale *ctx*))))

(declare get-font)

(defn cap-height []
  (-> (get-font) font/metrics :cap-height (/ (scale))))
  
(declare map->FnNode)

(defn parse-opts [element]
  (let [[_ opts & _] (parse-element element)]
    opts))

(defn maybe-render [node ctx]
  (when (or
          (:dirty? node)
          (when-some [should-render? (:should-render? node)]
            (apply should-render? (next (:element node)))))
    (protocols/-reconcile-impl node ctx (:element node))
    (core/set!! node :dirty? false)))

(defn make-record [^Class class]
  (let [ns   (str/replace (.getPackageName class) "_" "-")
        name (str "map->" (.getSimpleName class))
        ctor (ns-resolve (symbol ns) (symbol name))]
    (ctor {})))

(defn collect [key xs]
  (core/when-some+ [cbs (not-empty (vec (keep key xs)))]
    (fn [& args]
      (doseq [cb cbs]
        (apply cb args)))))

(defn collect-bool [key xs]
  (core/when-some+ [cbs (not-empty (vec (keep key xs)))]
    (fn [& args]
      (reduce #(or %1 (apply %2 args)) false cbs))))

(defn assert-arities [f g]
  (assert (= (core/arities f) (core/arities g)) (str "Arities of node fn and render fn should match, node: " (core/arities f) ", render: " (core/arities g))))

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

(defn make-impl
  ([el]
   (make-impl (map->FnNode {}) el))
  ([start-node el]
   (core/cond+
     (satisfies? protocols/IComponent el)
     el

     :let [[f & args] el
           f (core/maybe-deref f)]
        
     :else
     (binding [*node* start-node]
       (let [res  (cond
                    (fn? f)    (apply f args)
                    (class? f) (make-record f))
             node (loop [res res]
                    (core/cond+
                      (fn? res)
                      (do
                        (assert-arities f res)
                        (core/set!! *node* :render res)
                        *node*)
               
                      (satisfies? protocols/IComponent res)
                      res
                   
                      (map? res)
                      (let [render (:render res)]
                        (when render
                          (assert-arities f render))
                        (core/set!! *node*
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
                        (core/set!! *node* :render f)
                        *node*)
                     
                      :else
                      (throw (ex-info (str "Unexpected return type: " res) {:f f :args args :res res}))))]
         (core/set!! node
           :element el
           :dirty? true)
         (when-some [key (:key (meta el))]
           (core/set!! node :key key))
         (when-some [ref (:ref (meta el))]
           (reset! ref node))
         node)))))

(defn make
  ([el]
   (try
     (make-impl el)
     (catch Exception e
       (core/log-error e)
       (make-impl [@(resolve 'io.github.humbleui.ui/error) e]))))
  ([el ctx]
   (binding [*ctx* ctx]
     (make el))))

(defn should-reconcile? [ctx old-node new-el]
  (and 
    old-node
    (let [left  (core/maybe-deref (nth (:element old-node) 0))
          right (core/maybe-deref (nth new-el 0))]
      (or
        (identical? left right)
        ;; same lambdas with different captured vars still should reconcile
        (and
          (and (fn? left) (fn? right))
          (identical? (class left) (class right)))))
    (protocols/-should-reconcile? old-node ctx new-el)))

(defn keys-match? [keys m1 m2]
  (=
    (select-keys m1 keys)
    (select-keys m2 keys)))

(defn opts-match? [keys element new-element]
  (let [[_ opts _] (parse-element element)
        [_ new-opts _] (parse-element new-element)]
    (keys-match? keys opts new-opts)))

(defn autoconvert [el]
  (cond
    (string? el)
    [@(resolve 'io.github.humbleui.ui/label) el]
    
    :else
    el))

(defn reconcile-many [ctx old-nodes new-els]
  (core/loop+ [old-nodes-keyed (reduce
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
    (core/cond+
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

(defn force-render [node window]
  (core/set!! node :dirty? true)
  (.requestFrame ^Window window))

;; Nodes

(def ctor-border
  (paint/stroke 0x80FF00FF 4))

(core/defparent ANode
  [^:mut element
   ^:mut mounted?
   ^:mut rect
   ^:mut key
   ^:mut dirty?]
  
  protocols/IComponent
  (-context [_ ctx]
    ctx)

  (-measure [this ctx cs]
    (ui/maybe-render this ctx)
    (protocols/-measure-impl this ctx cs))
    
  (-draw [this ctx rect' canvas]
    (protocols/-set! this :rect rect')
    (ui/maybe-render this ctx)
    (protocols/-draw-impl this ctx rect' canvas)
    (when (and @debug/*outlines? (not (:mounted? this)))
      (canvas/draw-rect canvas (-> ^io.github.humbleui.types.IRect rect' .toRect (.inflate 4)) ui/ctor-border)
      (protocols/-set! this :mounted? true)))
  
  (-event [this ctx event]
    (when-some [ctx' (protocols/-context this ctx)]
      (protocols/-event-impl this ctx' event)))
  
  (-event-impl [this ctx event]
    nil)

  (-child-elements [this ctx new-element]
    (let [[_ _ child-els] (parse-element new-element)]
      child-els))
  
  (-reconcile [this ctx new-element]
    (when (not (identical? (:element this) new-element))
      (protocols/-reconcile-impl this ctx new-element)
      (protocols/-set! this :element new-element))
    this)
  
  (-reconcile-impl [this _ctx element]
    (throw (ex-info "Not implemented" {:element (:element this)})))
  
  (-should-reconcile? [_this _ctx _element]
    true)
  
  (-unmount [this]
    (protocols/-unmount-impl this))
  
  (-unmount-impl [_this]))

(core/defparent ATerminalNode
  "Simple component that has no children"
  []
  :extends ANode
  protocols/IComponent
  (-iterate [this _ctx cb]
    (cb this))
  
  (-reconcile-impl [this ctx _new-element]
    this))

(core/defparent AWrapperNode
  "A component that has exactly one child"
  [^:mut child]
  :extends ANode
  protocols/IComponent
  (-measure-impl [this ctx cs]
    (when-some [ctx' (protocols/-context this ctx)]
      (measure (:child this) ctx' cs)))

  (-draw-impl [this ctx rect canvas]
    (when-some [ctx' (protocols/-context this ctx)]
      (draw-child (:child this) ctx' rect canvas)))
  
  (-event-impl [this ctx event]
    (event-child (:child this) ctx event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (when-some [ctx' (protocols/-context this ctx)]
        (iterate-child (:child this) ctx' cb))))
  
  (-reconcile-impl [this ctx el']
    (let [child-els (protocols/-child-elements this ctx el')
          ctx'      (protocols/-context this ctx)
          [child']  (reconcile-many ctx' [(:child this)] child-els)]
      (protocols/-set! this :child child')))
  
  (-unmount [this]
    (unmount-child (:child this))
    (protocols/-unmount-impl this)))

(core/defparent AContainerNode
  "A component that has multiple children"
  [^:mut children]
  :extends ANode
  protocols/IComponent  
  (-event [this ctx event]
    (when-some [ctx' (protocols/-context this ctx)]
      (binding [*node* this
                *ctx*  ctx']
        (core/eager-or
          (reduce #(core/eager-or %1 (protocols/-event %2 ctx event)) nil (:children this))
          (protocols/-event-impl this ctx' event)))))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (some #(iterate-child % ctx cb) (:children this))))
  
  (-reconcile-impl [this ctx el']
    (let [child-els (protocols/-child-elements this ctx el')
          child-els (core/flatten child-els)
          children' (reconcile-many ctx (:children this) child-els)]
      (protocols/-set! this :children children')))
  
  (-unmount [this]
    (doseq [child (:children this)]
      (unmount-child child))
    (protocols/-unmount-impl this)))

;; FnNode

(core/deftype+ FnNode [^:mut child
                       ^:mut effect
                       ^:mut should-setup?
                       ^:mut should-render?
                       ^:mut after-mount
                       ^:mut before-render
                       ^:mut user-measure
                       ^:mut user-draw
                       ^:mut render
                       ^:mut after-render
                       ^:mut before-draw
                       ^:mut after-draw
                       ^:mut after-unmount]
  :extends ANode
  protocols/IComponent
  (-measure-impl [this ctx cs]
    (binding [*node* this
              *ctx*  ctx]
      (when render
        (set! rect (core/irect-xywh 0 0 (:width cs) (:height cs)))
        (maybe-render this ctx))
      (if user-measure
        (user-measure child cs)
        (measure child ctx cs))))
  
  (-draw [this ctx rect' canvas]
    (set! rect rect')
    (binding [*node* this
              *ctx*  ctx]
      (when render
        (maybe-render this ctx))
      (core/invoke before-draw)
      (if user-draw
        (user-draw child rect canvas)
        (protocols/-draw child ctx rect canvas))
      (core/invoke after-draw)
      (when-not mounted?
        (core/invoke after-mount))
      (when (and @debug/*outlines? (not mounted?))
        (canvas/draw-rect canvas (-> ^IRect rect .toRect (.inflate 4)) ctor-border)
        (set! mounted? true))))
    
  (-event-impl [this ctx event]
    (binding [*node* this
              *ctx*  ctx]
      (event-child child ctx event)))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (when render
        (iterate-child child ctx cb))))
  
  (-reconcile [this ctx new-element]
    (when (if should-render?
            (apply should-render? (next new-element))
            (not (identical? element new-element)))
      (protocols/-reconcile-impl this ctx new-element))
    (set! element new-element)
    this)
  
  (-reconcile-impl [this ctx new-element]
    (when (or
            (not (identical? (first element) (first new-element)))
            (apply core/invoke should-setup? (next new-element)))
      (make-impl this new-element))
    (when render
      (core/invoke before-render)
      (try
        (binding [signal/*context* (volatile! (transient #{}))
                  *ctx*            ctx]
          (let [child-el (apply render (next new-element))
                [child'] (reconcile-many ctx [child] [child-el])
                _        (set! child child')
                _        (set! element new-element)
                signals  (persistent! @@#'signal/*context*)
                window   (:window ctx)]
            (some-> effect signal/dispose!)
            (set! effect
              (when-not (empty? signals)
                (signal/effect signals
                  (force-render this window))))))
        (finally
          (core/invoke after-render)))))
  
  (-unmount [this]
    (unmount-child child)
    (some-> effect signal/dispose!)
    (when after-unmount
      (after-unmount)))
  
  java.lang.Object
  (toString [_]
    (pr-str element)))

(defmethod print-method FnNode [o ^java.io.Writer w]
  (.write w (str o)))

;; defcomp

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
