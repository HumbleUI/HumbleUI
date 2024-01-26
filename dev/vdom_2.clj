(ns vdom-2
  (:refer-clojure :exclude [flatten])
  (:require
    [clojure.core.server :as server]
    [clojure.math :as math]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [clj-async-profiler.core :as profiler]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.signal :as s]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.typeface :as typeface]
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.window :as window]
    [state :as state])
  (:import
    [io.github.humbleui.types IRect]
    [io.github.humbleui.skija Canvas TextLine]
    [io.github.humbleui.skija.shaper ShapingOptions]))

(when-not (.equalsIgnoreCase "false" (System/getProperty "io.github.humbleui.pprint-fn"))
  (defmethod print-method clojure.lang.AFunction [o ^java.io.Writer w]
    (.write w (clojure.lang.Compiler/demunge (.getName (class o))))))

(declare make map->FnNode)

(def ^:dynamic *ctx*)

(def ^:dynamic *node*)

(defn force-update [node]
  (core/set!! node :dirty? true)
  (state/request-frame))

(def ctor-border
  (paint/stroke 0x40FF00FF 2))

(defn flatten [xs]
  (mapcat #(if (and (not (vector? %)) (sequential? %)) (flatten %) [%]) xs))

(defn maybe-opts [vals]
  (if (map? (first vals))
    [(first vals) (next vals)]
    [{} vals]))

(defn invoke [f]
  (when f
    (f)))

(defn maybe-render [node ctx]
  (binding [*node* node
            *ctx*  ctx]
    (when (or
            (:dirty? node)
            (when-some [should-render? (:should-render? node)]
              (apply should-render? (next (:el node)))))
      (protocols/-reconcile-impl node (:el node))
      (core/set!! node :dirty? false))))

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

(defn make [el]
  (if (satisfies? protocols/IComponent el)
    el
    (binding [*node* (map->FnNode {})]
      (let [[f & args] el
            res  (cond
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
                         :measure        (:measure res)
                         :draw           (:draw res)
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
          :el el
          :dirty? true)
        (when-some [key (:key (meta el))]
          (core/set!! node :key key))
        (when-some [ref (:ref (meta el))]
          (reset! ref node))
        node))))

(defn should-reconcile? [old-node new-el]
  (and 
    old-node
    (identical? (first (:el old-node)) (first new-el))
    (protocols/-should-reconcile? old-node new-el)))

(defn unmount [node]
  (core/iterate-child node nil
    #(invoke (:after-unmount %))))

(defn reconcile-many [old-nodes new-els]
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
          (let [new-node (make new-el')]
            (recur [new-els   new-els'
                    res       (conj! res new-node)
                    keys-idxs keys-idxs']))
          
          ;; compatible key
          (should-reconcile? old-node new-el')
          (do
            (protocols/-reconcile old-node new-el')
            (recur [old-nodes-keyed (dissoc! old-nodes-keyed key')
                    new-els         new-els'
                    res             (conj! res old-node)
                    keys-idxs       keys-idxs']))
          
          ;; non-compatible key
          :else
          (let [new-node (make new-el')]
            (unmount old-node)
            (recur [old-nodes-keyed (dissoc! old-nodes-keyed key')
                    new-els         new-els'
                    res             (conj! res new-node)
                    keys-idxs       keys-idxs']))))

      (should-reconcile? old-node new-el)
      (do
        ; (println "compatible" old-node new-el)
        (protocols/-reconcile old-node new-el)
        (recur [old-nodes old-nodes'
                new-els   new-els'
                res       (conj! res old-node)]))
      
      ;; old-node was dropped
      (should-reconcile? (first old-nodes') new-el)
      (let [; _ (println "old-node dropped" old-node new-el)
            _ (unmount old-node)
            [old-node & old-nodes'] old-nodes']
        (protocols/-reconcile old-node new-el)
        (recur [old-nodes (next old-nodes')
                new-els   new-els'
                res       (conj! res old-node)]))
      
      ;; new-el was inserted
      (should-reconcile? old-node (first new-els'))
      (let [; _ (println "new-el inserted" old-node new-el)
            new-node (make new-el)]
        (recur [new-els new-els'
                res     (conj! res new-node)]))
      
      ;; just incompatible
      :else
      (let [; _ (println "incompatible" old-node new-el)
            new-node (make new-el)]
        (unmount old-node)
        (recur [old-nodes old-nodes'
                new-els   new-els'
                res       (conj! res new-node)])))))

(core/defparent ANode4
  [^:mut el
   ^:mut mounted?
   ^:mut self-rect
   ^:mut key
   ^:mut dirty?]
  
  protocols/IComponent
  (-measure [this ctx cs]
    (maybe-render this ctx)
    (binding [*node* this]
      (protocols/-measure-impl this ctx cs)))
    
  (-draw [this ctx rect canvas]
    (set! self-rect rect)
    (maybe-render this ctx)
    (binding [*node* this]
      (protocols/-draw-impl this ctx rect canvas))
    (when-not mounted?
      (canvas/draw-rect canvas (-> ^IRect rect .toRect (.inflate 4)) ctor-border)
      (set! mounted? true)))
  
  (-event [this ctx event]
    (maybe-render this ctx)
    (binding [*node* this]
      (protocols/-event-impl this ctx event)))
    
  protocols/IVDom
  (protocols/-reconcile [this new-el]
    (when (not (identical? (:el this) new-el))
      (protocols/-reconcile-impl this new-el)
      (set! el new-el))
    this)
  
  (protocols/-reconcile-impl [this el]
    (throw (ex-info "Not implemented" {:el el})))
  
  (-should-reconcile? [this el]
    true))

(core/defparent ATerminal4 []
  :extends ANode4
  protocols/IComponent
  (-event-impl [this ctx event])
  
  (-iterate [this ctx cb]
    (cb this))
  
  protocols/IVDom
  (-should-reconcile? [this new-el]
    (= el new-el))
  
  (protocols/-reconcile-impl [this el']
    this))
  
(core/defparent AWrapper4 [^:mut child]
  :extends ANode4
  protocols/IContext
  (-context [_ ctx]
    ctx)

  protocols/IComponent
  (-measure-impl [this ctx cs]
    (when-some [ctx' (protocols/-context this ctx)]
      (core/measure child ctx' cs)))

  (-draw-impl [this ctx rect canvas]
    (when-some [ctx' (protocols/-context this ctx)]
      (core/draw child ctx' rect canvas)))
  
  (-event-impl [this ctx event]
    (when-some [ctx' (protocols/-context this ctx)]
      (core/event child ctx' event)))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (when-some [ctx' (protocols/-context this ctx)]
        (core/iterate-child child ctx' cb))))
  
  protocols/IVDom
  (protocols/-reconcile-impl [_ el']
    (let [[_ [child-el]] (maybe-opts (next el'))
          [child']       (reconcile-many [child] [child-el])]
      (set! child child'))))

(core/defparent AContainer4 [^:mut children]
  :extends ANode4
  protocols/IComponent  
  (-event-impl [this ctx event]
    (reduce #(core/eager-or %1 (protocols/-event %2 ctx event)) nil children))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (some #(core/iterate-child % ctx cb) children)))
  
  protocols/IVDom
  (protocols/-reconcile-impl [_ el']
    (let [[_ child-els] (maybe-opts (next el'))
          child-els     (flatten child-els)
          children'     (reconcile-many children child-els)]
      (set! children children'))))

(core/deftype+ FnNode [^:mut child
                       ^:mut should-setup?
                       ^:mut should-render?
                       ^:mut after-mount
                       ^:mut before-render
                       ^:mut measure
                       ^:mut draw
                       ^:mut render
                       ^:mut after-render
                       ^:mut before-draw
                       ^:mut after-draw
                       ^:mut after-unmount]
  :extends ANode4
  protocols/IComponent
  (-measure-impl [this ctx cs]
    (when render
      (set! self-rect (core/irect-xywh 0 0 (:width cs) (:height cs)))
      (maybe-render this ctx))
    (if measure
      (binding [*ctx* ctx]
        (measure child cs))
      (core/measure child ctx cs)))
  
  (-draw [this ctx rect canvas]
    (set! self-rect rect)
    (when render
      (maybe-render this ctx))
    (invoke before-draw)
    (binding [*ctx* ctx]
      (if draw
        (draw child rect canvas)
        (core/draw child ctx rect canvas)))
    (invoke after-draw)
    (when-not mounted?
      (invoke after-mount)
      (canvas/draw-rect canvas (-> ^IRect rect .toRect (.inflate 4)) ctor-border)
      (set! mounted? true)))
    
  (-event-impl [this ctx event]
    (when render
      (maybe-render this ctx)
      (core/event child ctx event)))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (when render
        (core/iterate-child child ctx cb))))
  
  protocols/IVDom
  (protocols/-reconcile [this new-el]
    (when (if should-render?
            (apply should-render? (next new-el))
            (not (identical? (:el this) new-el)))
      (protocols/-reconcile-impl this new-el))
    (set! el new-el)
    this)
  
  (protocols/-reconcile-impl [this new-el]
    (when render
      (invoke before-render)
      (try
        (let [child-el (apply render (next new-el))
              [child'] (reconcile-many [child] [child-el])]
          (set! child child')
          (set! el new-el))
        (finally
          (invoke after-render)))))
  
  (-should-reconcile? [_ new-el]
    (if should-setup?
      (not (apply should-setup? (next new-el)))
      true))
  
  java.lang.Object
  (toString [_]
    (pr-str el)))

(defmethod print-method FnNode [o ^java.io.Writer w]
  (.write w (str o)))

(core/deftype+ Gap []
  :extends ATerminal4
  protocols/IComponent
  (-measure-impl [this ctx cs]
    (let [[opts] (maybe-opts (next el))
          {:keys [width height]} opts]
      (core/ipoint
        (* (:scale ctx) (or width 0))
        (* (:scale ctx) (or height 0)))))
  
  (-draw-impl [this ctx rect canvas]))

(defn gap [opts]
  (map->Gap {}))

(core/deftype+ Label [^:mut ^TextLine line]
  :extends ATerminal4
  protocols/IComponent
  (-measure-impl [this ctx cs]
    (core/ipoint
      (math/ceil (.getWidth line))
      (* (:scale ctx) (:leading ctx))))
  
  (-draw-impl [this ctx rect ^Canvas canvas]
    (.drawTextLine canvas
      line
      (:x rect)
      (+ (:y rect) (* (:scale ctx) (:leading ctx)))
      (:fill-text ctx))))

(defn a-label [& args]
  (let [[opts texts] (maybe-opts args)
        font         (or (:font opts) (:font-ui *ctx*))
        features     (or (:features opts) (:features *ctx*) ShapingOptions/DEFAULT)
        line         (.shapeLine core/shaper (str/join texts) font features)
        size         (core/ipoint
                       (math/ceil (.getWidth line))
                       (* (:scale *ctx*) (:leading *ctx*)))]
    (map->Label {:line line})))

(declare row with-context)

(defn label [& children]
  (if (= 1 (count children))
    [a-label (first children)]
    [row {:gap 0}
     (map #(vector a-label %) children)]))

(core/deftype+ WithContext []
  :extends AWrapper4
  protocols/IContext
  (-context [_ ctx]
    (let [[opts _] (maybe-opts (next el))]
      (merge ctx opts))))

(defn with-context [ctx child]
  (assert (map? ctx))
  (map->WithContext {}))

(core/deftype+ Center []
  :extends AWrapper4
  protocols/IComponent
  (-draw-impl [this ctx rect canvas]
    (let [w          (:width rect)
          h          (:height rect)
          child-size (protocols/-measure child ctx (core/isize w h))
          cw         (:width child-size)
          ch         (:height child-size)
          rect'      (core/irect-xywh
                       (-> (:x rect) (+ (/ w 2)) (- (/ cw 2)))
                       (-> (:y rect) (+ (/ h 2)) (- (/ ch 2)))
                       cw ch)]
      (protocols/-draw child ctx rect' canvas))))

(defn center [child]
  (map->Center {}))

(core/deftype+ Clickable []
  :extends AWrapper4
  protocols/IComponent  
  (-event-impl [this ctx event]
    (when (and
            (= :mouse-button (:event event))
            (:pressed? event)
            (core/rect-contains? self-rect (core/ipoint (:x event) (:y event))))
      (core/when-some+ [[opts _] (maybe-opts (next el))
                        {:keys [on-click]} opts]
        (on-click event)))
    (core/event-child child ctx event)))

(defn clickable [opts child]
  (map->Clickable {}))

(core/deftype+ Padding []
  :extends AWrapper4
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[opts _] (maybe-opts (next el))
          scale    (:scale ctx)
          left     (* scale (or (:left opts)   (:horizontal opts) (:padding opts) (:padding ctx) 0))
          right    (* scale (or (:right opts)  (:horizontal opts) (:padding opts) (:padding ctx) 0))
          top      (* scale (or (:top opts)    (:vertical opts)   (:padding opts) (:padding ctx) 0))
          bottom   (* scale (or (:bottom opts) (:vertical opts)   (:padding opts) (:padding ctx) 0))
          cs'      (core/ipoint
                     (- (:width cs) left right)
                     (- (:height cs) top bottom))
          size'    (core/measure child ctx cs')]
      (core/ipoint
        (+ (:width size') left right)
        (+ (:height size') top bottom))))

  (-draw-impl [this ctx rect canvas]
    (let [[opts _] (maybe-opts (next el))
          scale    (:scale ctx)
          left     (* scale (or (:left opts)   (:horizontal opts) (:padding opts) (:padding ctx) 0))
          right    (* scale (or (:right opts)  (:horizontal opts) (:padding opts) (:padding ctx) 0))
          top      (* scale (or (:top opts)    (:vertical opts)   (:padding opts) (:padding ctx) 0))
          bottom   (* scale (or (:bottom opts) (:vertical opts)   (:padding opts) (:padding ctx) 0))
          rect'    (core/irect-ltrb
                     (+ (:x rect) left)
                     (+ (:y rect) top)
                     (- (:right rect) right)
                     (- (:bottom rect) bottom))]
      (protocols/-draw child ctx rect' canvas))))

(defn padding
  ([child]
   (map->Padding {}))
  ([opts child]
   (map->Padding {})))

(core/deftype+ Rect []
  :extends AWrapper4
  protocols/IComponent  
  (-draw-impl [this ctx rect canvas]
    (let [[opts _] (maybe-opts (next el))]
      (canvas/draw-rect canvas rect (:fill opts))
      (core/draw-child child ctx rect canvas))))

(defn rect [opts child]
  (map->Rect {}))

(core/deftype+ Width []
  :extends AWrapper4
  protocols/IComponent  
  (-measure-impl [_ ctx cs]
    (let [scale    (:scale ctx)
          [opts _] (maybe-opts (next el))
          size     (core/measure child ctx 
                     (core/ipoint (* scale (:width opts)) (:height cs)))]
      (core/ipoint (* scale (:width opts)) (:height size)))))

(defn width [opts child]
  (map->Width {}))

(core/deftype+ Column []
  :extends AContainer4
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[opts _] (maybe-opts (next el))
          gap      (* (:scale ctx) (or (:gap opts) (:padding ctx)))]
      (loop [children children
             w        0
             h        0]
        (if-some [child (first children)]
          (let [size (protocols/-measure child ctx cs)]
            (recur
              (next children)
              (long (max w (:width size)))
              (long (+ h (:height size) gap))))
          (core/isize w (if (> h gap) (- h gap) h))))))
  
  (-draw-impl [this ctx rect canvas]
    (let [[opts _] (maybe-opts (next el))
          gap      (* (:scale ctx) (or (:gap opts) (:padding ctx)))
          width    (:width rect)]
      (loop [children children
             top      (:y rect)]
        (when-some [child (first children)]
          (let [size (protocols/-measure child ctx (core/isize (:width rect) (:height rect)))
                x    (+ (:x rect) (/ (- width (:width size)) 2))]
            (protocols/-draw child ctx (core/irect-xywh x top (:width size) (:height size)) canvas)
            (recur (next children) (+ top (:height size) gap))))))))

(defn column [& children]
  (map->Column {}))

(core/deftype+ Row []
  :extends AContainer4
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[opts _] (maybe-opts (next el))
          gap      (* (:scale ctx) (or (:gap opts) (:padding ctx)))]
      (loop [children children
             w        0
             h        0]
        (if-some [child (first children)]
          (let [size (protocols/-measure child ctx cs)]
            (recur
              (next children)
              (long (+ w (:width size) gap))
              (long (max h (:height size)))))
          (core/isize (if (> w gap) (- w gap) w) h)))))
  
  (-draw-impl [this ctx rect canvas]
    (let [[opts _] (maybe-opts (next el))
          gap      (* (:scale ctx) (or (:gap opts) (:padding ctx)))
          height   (:height rect)]
      (loop [children children
             left     (:x rect)]
        (when-some [child (first children)]
          (let [size (protocols/-measure child ctx (core/isize (:width rect) (:height rect)))
                y    (+ (:y rect) (/ (- height (:height size)) 2))]
            (protocols/-draw child ctx (core/irect-xywh left y (:width size) (:height size)) canvas)
            (recur (next children) (+ left (:width size) gap))))))))

(defn row [& children]
  (map->Row {}))

(core/deftype+ Split []
  :extends AContainer4
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    cs)
  
  (-draw-impl [this ctx rect canvas]
    (let [[left right] children
          left-size  (core/measure left ctx (core/isize (:width rect) (:height rect)))
          left-rect  (core/irect-xywh
                       (:x rect)
                       (:y rect)
                       (:width left-size)
                       (:height rect))
          right-rect (core/irect-xywh
                       (+ (:x rect) (:width left-size))
                       (:y rect)
                       (- (:width rect) (:width left-size))
                       (:height rect))]
      (core/draw left ctx left-rect canvas)
      (core/draw right ctx right-rect canvas))))

(defn split
  ([left right]
   (split {} left right))
  ([opts left right]
   (map->Split {})))

(defn ratom [init]
  (let [node *node*
        res  (atom init)]
    (add-watch res ::force-update
      (fn [_ _ old new]
        (when (not= old new)
          (force-update node))))
    res))

(defn button [opts text]
  [clickable (select-keys opts [:on-click])
   [rect {:fill (:hui.button/bg *ctx*)}
    [padding {}
     [center
      [label text]]]]])

(defn use-signals []
  (let [id      (rand-int 10000)
        *effect (volatile! nil)
        node    *node*]
    {:before-render
     (fn []
       (push-thread-bindings {#'s/*context* (volatile! (transient #{}))}))
     :after-render
     (fn []
       (let [signals (persistent! @@#'s/*context*)]
         (pop-thread-bindings)
         (some-> @*effect s/dispose!)
         (vreset! *effect
           (when-not (empty? signals)
             (s/effect signals
               (force-update node))))))
     :after-unmount
     (fn []
       (some-> @*effect s/dispose!))}))

(def *key
  (volatile! 0))

(defn auto-keys [form]
  (walk/postwalk
    (fn [form]
      (if (and (vector? form) (instance? clojure.lang.IObj form))
        (let [m (meta form)]
          (if (contains? m :key)
            form
            (vary-meta form assoc :key (vswap! *key inc))))
        form))
    form))

(defmacro defcomp [name args & body]
  `(defn ~name ~args
     (let [~'&node *node*]
       ~@(auto-keys body))))

;; examples

(defn example-static []
  [label "Hello, bumbles!"])

(defn comp-no-args []
  [label "No args"])

(defn comp-one-arg [msg]
  [label msg])

(defn comp-two-args [a b]
  [label a b])

(defn comp-varargs [& msgs]
  [row
   (map #(vector label %) msgs)])

(defn example-arguments []
  [column
   [comp-no-args]
   [comp-one-arg "One arg"]
   [comp-two-args "Two" " args"]
   [comp-varargs "Multiple variable args"]
   [comp-varargs "Multiple" " variable" " args"]])

(defn example-return-fn []
  (let [*state (ratom 0)]
    (fn []
      [row
       [label "Clicked: " @*state]
       [button {:on-click (fn [_] (swap! *state inc))} "INC"]])))

(defn example-return-map []
  (let [*state (ratom 0)]
    {:render
     (fn []
       [row
        [label "Clicked: " @*state]
        [button {:on-click (fn [_] (swap! *state inc))} "INC"]])}))

(defn example-return-maps []
  (let [*state (ratom 0)]
    [{:after-unmount (fn [] (println "unmount 1"))}
     {:after-unmount (fn [] (println "unmount 2"))}
     {:after-unmount (fn [] (println "unmount 3"))
      :render
      (fn []
        [row
         [label "Clicked: " @*state]
         [button {:on-click (fn [_] (swap! *state inc))} "INC"]])}]))

(defn example-anon-fn []
  [column
   [(fn []
      [label "just fn"])]
   [(fn [x y z]
      [label "fn + args: " x y z])
    1 2 3]
   [(fn []
      (fn []
        [label "setup"]))]
   [(fn []
      {:render
       (fn []
         [label "map"])})]])
         

(defn timer []
  (let [*state (ratom 0)
        _      (println "starting timer")
        cancel (core/schedule
                 #(swap! *state inc) 0 1000)]
    {:after-unmount
     (fn []
       (println "cancelling timer")
       (cancel))
     :render
     (fn []
       [label "Time: " @*state " sec"])}))

(defn example-lifecycle []
  (let [*visible? (ratom true)]
    (fn []
      (if @*visible?
        [column
         [timer]
         [button {:on-click (fn [_] (reset! *visible? false))} "Hide"]]
        [column
         [button {:on-click (fn [_] (reset! *visible? true))} "Show"]]))))

(defn use-state [val]
  (let [*state (ratom val)]
    {:value *state}))

(defn use-timer []
  (with [*state (use-state 0)]
    (println "starting timer")
    (let [cancel (core/schedule
                   #(swap! *state inc) 0 1000)]
      {:value *state
       :after-unmount
       (fn []
         (println "cancelling timer")
         (cancel))})))

(defn timer2 []
  (with [*timer (use-timer)]
    (fn []
      [label "Time: " @*timer " sec"])))

(defn example-mixins []
  (let [*visible? (ratom true)]
    (fn []
      (if @*visible?
        [column
         [timer2]
         [button {:on-click (fn [_] (reset! *visible? false))} "Hide"]]
        [column
         [button {:on-click (fn [_] (reset! *visible? true))} "Show"]]))))

(defcomp example-diff-incompat []
  (let [*state (ratom 0)]
    (fn []
      [column
       (for [i (range 6)
             :let [lbl [padding {:padding (:padding *ctx*)}
                        [label "Item " i]]]]
         (if (= i @*state)
           [rect {:fill (:hui.button/bg *ctx*)} lbl]
           [clickable {:on-click (fn [_] (reset! *state i))} lbl]))])))

(defcomp example-diff-compat []
  (let [*state      (ratom 0)
        transparent (paint/fill 0x00000000)]
    (fn []
      [column
       (for [i (range 6)]
         [clickable {:on-click (fn [_] (reset! *state i))}
          [rect {:fill (if (= i @*state)
                         (:hui.button/bg *ctx*)
                         transparent)}
           [padding {:padding (:padding *ctx*)}
            [label "Item " i]]]])])))

(defcomp example-diff-keys []
  (let [*state (ratom 0)]
    (fn []
      [column
       (for [i (range 6)]
         (if (= i @*state)
           ^{:key i} [rect {:fill (:hui.button/bg *ctx*)}
                      [padding {:padding (:padding *ctx*)}
                       [label "Item " i]]]
           ^{:key i} [clickable {:on-click (fn [_] (reset! *state i))}
                      [padding {:padding (:padding *ctx*)}
                       [label "Item " i]]]))])))

(defn auto-key-label [text]
  (let [*state (atom 0)
        fill   (paint/fill 0xFFDAE8C8)]
    {:render
     (fn [text]
       [rect {:fill fill}
        [padding {:padding (:padding *ctx*)}
         [label text " (render #" (swap! *state inc) ")"]]])
     :after-unmount
     (fn []
       (println "close" text @*state)
       (.close fill))
     }))

(defcomp example-auto-keys []
  [row
   [column
    (if (> (rand) 0.5)
      [auto-key-label "Auto key left"]
      [auto-key-label "Auto key right"])
    (if (> (rand) 0.5)
      ^{:key :second} [auto-key-label "Manual key left"]
      ^{:key :second} [auto-key-label "Manual key right"])
    (for [i (range 0 (+ 1 (rand-int 5)))]
      [label "Item " i])
    [auto-key-label "Auto key bottom"]
    ^{:key :bottom} [auto-key-label "Manual key bottom"]]
   [column
    [button {:on-click (fn [_] (force-update &node))} "Randomize"]]])

(defcomp example-force-update-fn []
  (let [*state (atom 0)]
    (fn []
      [row
       [label "Clicked: " @*state]
       [button
        {:on-click
         (fn [_] 
           (swap! *state inc)
           (force-update &node))}
        "INC"]])))

(defcomp example-force-update-inline []
  [row
   [label "Random: " (rand)]
   [button {:on-click (fn [_] (force-update &node))} "INC"]])

(defn should-setup [arg]
  {:should-setup?
   (fn [arg']
     (> arg' arg))
   :render
   (fn [arg']
     [label "setup: " arg ", render: " arg'])})

(defn example-should-setup []
  (let [*state (ratom 0)]
    (fn []
      [column
       [should-setup @*state]
       [row
        [button {:on-click (fn [_] (swap! *state dec))} "DEC"]
        [button {:on-click (fn [_] (swap! *state inc))} "INC"]]])))

(defn should-render [arg]
  (let [*last-arg (atom arg)]
    {:should-render?
     (fn [arg']
       (let [[last-arg _] (reset-vals! *last-arg arg')]
         (> arg' last-arg)))
     :render
     (fn [arg']
       (reset! *last-arg arg')
       [label "setup: " arg ", render: " arg'])}))

(defn example-should-render []
  (let [*state (ratom 0)]
    (fn []
      [column
       [should-render @*state]
       [row
        [button {:on-click (fn [_] (swap! *state dec))} "DEC"]
        [button {:on-click (fn [_] (swap! *state inc))} "INC"]]])))

(defn skip-identical [arg]
  (let [*render (atom 0)]
    (fn [arg]
      (swap! *render inc)
      (println "Render" arg)
      [label arg " " @*render])))

(defn example-skip-identical []
  (let [node   *node*
        cached [skip-identical "Cached"]]
    (fn []
      [column
       [skip-identical "Non-cached"]
       cached
       [button {:on-click (fn [_] (force-update node))} "Render"]])))

(defn example-signals-label [_ _]
  (with [_ (use-signals)]
    (let [*render (volatile! 0)]
      (fn [text *signal]
        [label text @*signal ", render: " (vswap! *render inc)]))))

(defn example-signals []
  (let [*signal (s/signal "s" 0)
        *double (s/signal "(* s 2)" (* 2 @*signal))
        *quot   (s/signal "(quot s 3)" (quot @*signal 3))]
    (fn []
      [column
       [example-signals-label "Signal: " *signal]
       [example-signals-label "Signal: " *signal]
       [example-signals-label "Double: " *double]
       [example-signals-label "Quot 3: " *quot]
       [row
        [button {:on-click (fn [_] (s/swap! *signal dec))} "DEC"]
        [button {:on-click (fn [_] (s/swap! *signal inc))} "INC"]]])))

(defcomp example-refs []
  (let [*ref   (atom nil)
        *size  (ratom nil)
        *state (ratom "A")]
    {:render
     (fn []
       (core/log "render" &node)
       [column
        ^{:ref *ref} [label @*state]
        [label "Size: " @*size]
        [button {:on-click
                 (fn [_]
                   (reset! *state
                     (str/join
                       (repeatedly (+ 1 (rand-int 10))
                         #(rand-nth "abcdefghijklmnopqrstuvwxyz")))))}
         "Randomize"]])
     :after-draw
     (fn []
       (let [rect (:self-rect @*ref)
             size [(:width rect) (:height rect)]]
         (reset! *size size)))}))

(defn example-materialize []
  (let [labels ["Ok" "Save" "Save & Quit"]
        nodes  (mapv #(make [button {} %]) labels)
        cs     (core/ipoint Integer/MAX_VALUE Integer/MAX_VALUE)
        widths (mapv #(/ (:width (core/measure % *ctx* cs)) (:scale *ctx*)) nodes)
        max-w  (reduce max 0 widths)]
    [row
     (for [node nodes]
       [width {:width max-w} node])]))

(defn measure-draw-terminal []
  (let [paint (paint/fill 0x2033CC33)]
    {:measure
     (fn [_ cs]
       (core/ipoint 200 100))
     :draw
     (fn [_ rect canvas]
       (canvas/draw-rect canvas rect paint))}))

(defn draw-wrapper [_]
  (let [paint (paint/fill 0x20CC3333)]
    {:draw
     (fn [child rect canvas]
       (canvas/draw-rect canvas rect paint)
       (core/draw child *ctx* rect canvas))
     :render
     (fn [child-el]
       child-el)}))

(defn measure-draw-wrapper [_]
  (let [red   (paint/fill 0x20CC3333)
        blue  (paint/fill 0x203333CC)
        gap   (* (:scale *ctx*) 10)]
    {:measure
     (fn [child cs]
       (let [size (core/measure child *ctx* cs)]
         (core/ipoint 
           (+ (:width size) (* 2 gap))
           (+ (:height size) (* 2 gap)))))

     :draw
     (fn [child rect canvas]
       (canvas/draw-rect canvas rect red)
       (canvas/with-canvas canvas
         (let [rect' (core/irect-xywh
                       (+ (:x rect) gap)
                       (+ (:y rect) gap)
                       (- (:width rect) (* 2 gap))
                       (- (:height rect) (* 2 gap)))]
           (core/draw child *ctx* rect' canvas)
           (canvas/draw-rect canvas rect' blue))))
     
     :render
     (fn [child-el]
       child-el)}))

(defn example-measure-draw []
  [column
   [measure-draw-terminal]
   [draw-wrapper
    [label "Without measure"]]
   [measure-draw-wrapper
    [label "With measure"]]])

(defn node-size []
  (let [scale (or (:scale *ctx*) 1)
        w     (or (:width (:self-rect *node*)) 0)
        h     (or (:height (:self-rect *node*)) 0)]
    (core/point (/ w scale) (/ h scale))))

(defn use-size []
  (with [_ (use-signals)]
    (let [*size (s/signal (core/point 0 0))]
      {:before-draw
       (fn [& args]
         (s/reset! *size (node-size)))
       :value *size})))

(defcomp size-user [x y z]
  (with [*size (use-size)]
    (let [padding 10
          size    60
          *cnt    (s/signal 
                    (quot
                      (+ (long (:width @*size)) padding)
                      (+ size padding)))]
      (fn [x y z]
        (let [cnt @*cnt]
          (core/log "render" cnt "cols")
          [row
           (for [[_ text] (map vector (range 0 cnt) (cycle [x y z]))]
             [rect {:fill (:hui.button/bg *ctx*)}
              [width {:width size}
               [label text]]])])))))

(defn example-use-size []
  [size-user 1 2 3])

(defn item [*state id]
  (println "mount" id)
  {:after-unmount (fn [] (println "unmount" id))
   :render
   (fn [*state id]
     [row
      [label "Id: " id ", clicks: " (@*state id)]
      [button {:on-click (fn [_] (swap! *state update id inc))} "INC"]
      [button {:on-click (fn [_] (swap! *state dissoc id))} "DEL"]])})

(defn example-rows []
  (let [*state (ratom (into (sorted-map) (map #(vector % 0) (range 3))))]
    (fn []
      [column
       (for [k (keys @*state)]
         ^{:key k} [item *state k])
       [button {:on-click (fn [_] (swap! *state assoc (inc (reduce max 0 (keys @*state))) 0))} "ADD"]])))

;; shell

(def examples
  ["static"
   "arguments"
   "return-fn"
   "return-map"
   "return-maps"
   "anon-fn"
   "lifecycle"
   "mixins"
   "diff-incompat"
   "diff-compat"
   "diff-keys"
   "auto-keys"
   "force-update-fn"
   "force-update-inline"
   "should-setup"
   "should-render"
   "skip-identical"
   "signals"
   "refs"
   "materialize"
   "measure-draw"
   "use-size"
   "rows"])

(defn app-impl []
  (let [*selected (ratom "use-size" #_(first examples))]
    (fn []
      [split
       [padding {:padding 10}
        [column {:gap 0}
         (for [name examples
               :let [lbl [padding {:padding 10}
                          [label name]]]]
           (if (= @*selected name)
             ^{:key name} [rect {:fill (:hui.button/bg *ctx*)} lbl]
             ^{:key name} [clickable {:on-click (fn [_] (reset! *selected name))} lbl]))]]
       [center
        [@(ns-resolve 'vdom-2 (symbol (str "example-" @*selected)))]]])))

(def app
  (ui/default-theme
    {:cap-height 10}
    (ui/with-context
      {:features (.withFeatures ShapingOptions/DEFAULT "tnum")
       :padding  10}
      (make [app-impl]))))
