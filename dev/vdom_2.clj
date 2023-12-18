(ns vdom-2
  (:refer-clojure :exclude [flatten])
  (:require
    [clojure.core.server :as server]
    [clojure.math :as math]
    [clojure.string :as str]
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

(declare make map->FnComponent)

(def ^:dynamic *ctx*)

(def ^:dynamic *comp*)

(defn invalidate [comp]
  (core/set!! comp :dirty? true)
  (state/request-frame))

(def ctor-border
  (paint/stroke 0x80FF00FF 2))

(defn flatten [xs]
  (mapcat #(if (and (not (vector? %)) (sequential? %)) (flatten %) [%]) xs))

(defn maybe-opts [vals]
  (if (map? (first vals))
    [(first vals) (next vals)]
    [{} vals]))

(defn invoke [f]
  (when f
    (f)))

(defn maybe-render [comp ctx]
  (binding [*comp* comp
            *ctx*  ctx]
    (when (:dirty? comp)
      (invoke (:before-render comp))
      (try
        (protocols/-reconcile-impl comp (:el comp))
        (core/set!! comp :dirty? false)
        (finally
          (invoke (:after-render comp)))))))

(defn make-record [^Class class]
  (let [ns   (str/replace (.getPackageName class) "_" "-")
        name (str "map->" (.getSimpleName class))
        ctor (ns-resolve (symbol ns) (symbol name))]
    (ctor {})))

(defn collect [key xs]
  (let [cbs (keep key xs)]
    (fn [& args]
      (doseq [cb cbs]
        (apply cb args)))))

(defn make [el]
  (if (satisfies? protocols/IComponent el)
    el
    (binding [*comp* (map->FnComponent {})]
      (let [[f & args] el
            res  (cond
                   (ifn? f)   (apply f args)
                   (class? f) (make-record f))
            comp (core/cond+
                   (map? res)
                   (do
                     (core/set!! *comp*
                       :render        (:render res)
                       :before-render (:before-render res)
                       :after-render  (:after-render res)
                       :after-unmount (:after-unmount res)
                       :dirty?        true)
                     *comp*)
               
                   (and (vector? res) (map? (first res)))
                   (do
                     (core/set!! *comp*
                       :render        (some :render res)
                       :before-render (collect :before-render res)
                       :after-render  (collect :after-render res)
                       :after-unmount (collect :after-unmount res)
                       :dirty?        true)
                     *comp*)
                   
                   (vector? res)
                   (do
                     (core/set!! *comp*
                       :render f
                       :dirty? true)
                     *comp*)
                   
                   (ifn? res)
                   (do
                     (core/set!! *comp*
                       :render res
                       :dirty? true)
                     *comp*)
               
                   (satisfies? protocols/IComponent res)
                   (do
                     (core/set!! res :dirty? true)
                     res)
               
                   :else
                   (throw (ex-info (str "Unexpected return type: " res) {:f f :args args :res res})))]
        (core/set!! comp :el el)
        (when-some [key (:key (meta el))]
          (core/set!! comp :key key))
        comp))))

(defn compatible? [old-comp new-el]
  (and 
    old-comp
    (identical? (first (:el old-comp)) (first new-el))
    (protocols/-compatible-impl old-comp new-el)))

(defn unmount [comp]
  (core/iterate-child comp nil
    #(invoke (:after-unmount %))))

(defn reconcile [old-comps new-els]
  (loop [old-comps-keyed (->> old-comps
                           (filter :key)
                           (map #(vector (:key %) %))
                           (into {}))
         old-comps       (filter #(and % (nil? (:key %))) old-comps)
         new-els   new-els
         res       []]
    (core/cond+
      (empty? new-els)
      (do
        (doseq [comp (concat old-comps (vals old-comps-keyed))]
          (unmount comp))
        res)
      
      :let [[old-comp & old-comps'] old-comps
            [new-el & new-els'] new-els
            key (:key (meta new-el))]
      
      key
      (let [old-comp (old-comps-keyed key)]
        (cond
          ;; new key
          (nil? old-comp)
          (let [new-comp (make new-el)]
            (recur old-comps-keyed old-comps new-els' (conj res new-comp)))
          
          ;; compatible key
          (compatible? old-comp new-el)
          (do
            (protocols/-reconcile-impl old-comp new-el)
            (recur (dissoc old-comps-keyed key) old-comps new-els' (conj res old-comp)))
          
          ;; non-compatible key
          :else
          (let [new-comp (make new-el)]
            (unmount old-comp)
            (recur (dissoc old-comps-keyed key) old-comps new-els' (conj res new-comp)))))

      (compatible? old-comp new-el)
      (do
        ; (println "compatible" old-comp new-el)
        (protocols/-reconcile-impl old-comp new-el)
        (recur old-comps-keyed old-comps' new-els' (conj res old-comp)))
      
      ;; old-comp was dropped
      (compatible? (first old-comps') new-el)
      (let [; _ (println "old-comp dropped" old-comp new-el)
            _ (unmount old-comp)
            [old-comp & old-comps'] old-comps']
        (protocols/-reconcile-impl old-comp new-el)
        (recur old-comps-keyed (next old-comps') new-els' (conj res old-comp)))
      
      ;; new-el was inserted
      (compatible? old-comp (first new-els'))
      (let [; _ (println "new-el inserted" old-comp new-el)
            new-comp (make new-el)]
        (recur old-comps-keyed old-comps new-els' (conj res new-comp)))
      
      ;; just incompatible
      :else
      (let [; _ (println "incompatible" old-comp new-el)
            new-comp (make new-el)]
        (unmount old-comp)
        (recur old-comps-keyed old-comps' new-els' (conj res new-comp))))))

(core/defparent AComponent4
  [^:mut el
   ^:mut mounted?
   ^:mut self-rect
   ^:mut key
   ^:mut dirty?]
  
  protocols/IComponent
  (-measure [this ctx cs]
    (maybe-render this ctx)
    (protocols/-measure-impl this ctx cs))
    
  (-draw [this ctx rect canvas]
    (set! self-rect rect)
    (maybe-render this ctx)
    (protocols/-draw-impl this ctx rect canvas)
    (when-not mounted?
      (canvas/draw-rect canvas (-> ^IRect rect .toRect (.inflate 4)) ctor-border)
      (set! mounted? true)))
  
  (-event [this ctx event]
    (maybe-render this ctx)
    (protocols/-event-impl this ctx event))
    
  protocols/IVDom
  (-reconcile-impl [this el]
    (throw (ex-info "Not implemented" {:el el})))
  
  (-compatible-impl [this el]
    true))

(core/defparent ATerminal4 []
  :extends AComponent4
  protocols/IComponent
  (-event-impl [this ctx event])
  
  (-iterate [this ctx cb]
    (cb this))
  
  protocols/IVDom
  (-compatible-impl [this new-el]
    (= el new-el))
  
  (-reconcile-impl [this el']
    this))
  
(core/defparent AWrapper4 [^:mut child]
  :extends AComponent4
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
  (-reconcile-impl [_ el']
    (let [[_ [child-el]] (maybe-opts (next el'))
          [child'] (reconcile [child] [child-el])]
      (set! child child')
      (set! el el'))))

(core/defparent AContainer4 [^:mut children]
  :extends AComponent4
  protocols/IComponent  
  (-event-impl [this ctx event]
    (reduce #(core/eager-or %1 (protocols/-event %2 ctx event)) nil children))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (some #(core/iterate-child % ctx cb) children)))
  
  protocols/IVDom
  (-reconcile-impl [_ el']
    (let [[_ child-els] (maybe-opts (next el'))
          child-els (flatten child-els)
          children' (reconcile children child-els)]
      (set! children children')
      (set! el el'))))

(core/deftype+ FnComponent [^:mut render
                            ^:mut before-render
                            ^:mut after-render
                            ^:mut after-unmount
                            ^:mut child]
  :extends AComponent4
  protocols/IComponent
  (-measure-impl [this ctx cs]
    (maybe-render this ctx)
    (core/measure child ctx cs))

  (-draw-impl [this ctx rect canvas]
    ; (core/log "Render")
    (maybe-render this ctx)
    (core/draw child ctx rect canvas))
  
  (-event-impl [this ctx event]
    (maybe-render this ctx)
    (core/event child ctx event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (core/iterate-child child ctx cb)))
  
  protocols/IVDom
  (-reconcile-impl [_ el']
    (let [child-el (apply render (next el'))
          [child'] (reconcile [child] [child-el])]
      (set! child child')
      (set! el el'))))

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
  (-measure-impl [_ ctx cs]
    cs)

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
      (core/when-every [[opts _] (maybe-opts (next el))
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
          left     (* scale (or (:left opts)   (:horizontal opts) (:padding opts) 0))
          right    (* scale (or (:right opts)  (:horizontal opts) (:padding opts) 0))
          top      (* scale (or (:top opts)    (:vertical opts)   (:padding opts) 0))
          bottom   (* scale (or (:bottom opts) (:vertical opts)   (:padding opts) 0))
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
          left     (* scale (or (:left opts)   (:horizontal opts) (:padding opts) 0))
          right    (* scale (or (:right opts)  (:horizontal opts) (:padding opts) 0))
          top      (* scale (or (:top opts)    (:vertical opts)   (:padding opts) 0))
          bottom   (* scale (or (:bottom opts) (:vertical opts)   (:padding opts) 0))
          rect'    (core/irect-ltrb
                     (+ (:x rect) left)
                     (+ (:y rect) top)
                     (- (:right rect) right)
                     (- (:bottom rect) bottom))]
      (protocols/-draw child ctx rect' canvas))))

(defn padding [opts child]
  (map->Padding {}))

(core/deftype+ Rect []
  :extends AWrapper4
  protocols/IComponent  
  (-draw-impl [this ctx rect canvas]
    (let [[opts _] (maybe-opts (next el))]
      (canvas/draw-rect canvas rect (:fill opts))
      (core/draw-child child ctx rect canvas))))

(defn rect [opts child]
  (map->Rect {}))

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
          (core/isize w h)))))
  
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
          (core/isize w h)))))
  
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
  (let [comp *comp*
        res  (atom init)]
    (add-watch res ::invalidate
      (fn [_ _ old new]
        (when (not= old new)
          (invalidate comp))))
    res))

(defn button [opts child]
  [clickable (select-keys opts [:on-click])
   [rect {:fill (:hui.button/bg *ctx*)}
    [padding {:horizontal (:padding *ctx*)
              :vertical   (quot (:padding *ctx*) 2)}
     child]]])

(defn use-signals []
  (let [*effect (volatile! nil)
        comp    *comp*]
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
               (invalidate comp))))))
     :after-unmount
     (fn []
       (some-> @*effect s/dispose!))}))

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
       [button {:on-click (fn [_] (swap! *state inc))}
        [label "INC"]]])))

(defn example-return-map []
  (let [*state (ratom 0)]
    {:render
     (fn []
       [row
        [label "Clicked: " @*state]
        [button {:on-click (fn [_] (swap! *state inc))}
         [label "INC"]]])}))

(defn example-return-maps []
  (let [*state (ratom 0)]
    [{:after-unmount (fn [] (println "unmount 1"))}
     {:after-unmount (fn [] (println "unmount 2"))}
     {:after-unmount (fn [] (println "unmount 3"))
      :render
      (fn []
        [row
         [label "Clicked: " @*state]
         [button {:on-click (fn [_] (swap! *state inc))}
          [label "INC"]]])}]))

(defn example-lifecycle []
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
       [label "Timer: " @*state " sec"])}))

(defn example-diff-incompat []
  (let [*state (ratom 0)]
    (fn []
      [column
       (for [i (range 6)
             :let [lbl [padding {:padding (:padding *ctx*)}
                        [label "Item " i]]]]
         (if (= i @*state)
           [rect {:fill (:hui.button/bg *ctx*)} lbl]
           [clickable {:on-click (fn [_] (reset! *state i))} lbl]))])))

(defn example-diff-compat []
  (let [*state (ratom 0)
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

(defn example-diff-keys []
  (let [*state (ratom 0)]
    (fn []
      [column
       (for [i (range 6)]
         (if (= i @*state)
           ^{:key i} [rect {:fill (:hui.button/bg *ctx*)}
                      [padding {:padding (:padding *ctx*)}
                       [label "Item" i]]]
           ^{:key i} [clickable {:on-click (fn [_] (reset! *state i))}
                      [padding {:padding (:padding *ctx*)}
                       [label "Item" i]]]))])))

(defn example-invalidate []
  (let [*state (atom 0)
        comp   *comp*]
    (fn []
      [row
       [label "Clicked: " @*state]
       [button {:on-click (fn [_] 
                            (swap! *state inc)
                            (invalidate comp))}
        [label "INC"]]])))

(defn example-signals-label [_ _]
  (let [*render (volatile! 0)]
    [(use-signals)
     {:render
      (fn [text *signal]
        [label text @*signal ", render: " (vswap! *render inc)])}]))

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
        [button {:on-click (fn [_] (s/swap! *signal dec))}
         [label "DEC"]]
        [button {:on-click (fn [_] (s/swap! *signal inc))}
         [label "INC"]]]])))

(defn item [*state id]
  (println "mount" id)
  {:after-unmount (fn [] (println "unmount" id))
   :render
   (fn [*state id]
     [row
      [label "Id: " id ", clicks: " (@*state id)]
      [button {:on-click (fn [_] (swap! *state update id inc))}
       [label "INC"]]
      [button {:on-click (fn [_] (swap! *state dissoc id))}
       [label "DEL"]]])})

(defn example-rows []
  (let [*state (ratom (into (sorted-map) (map #(vector % 0) (range 3))))]
    (fn []
      [column
       (for [k (keys @*state)]
         ^{:key k} [item *state k])
       [button {:on-click (fn [_] (swap! *state assoc (inc (reduce max 0 (keys @*state))) 0))}
        [label "ADD"]]])))

;; shell

(def examples
  ["static"
   "arguments"
   "return-fn"
   "return-map"
   "return-maps"
   "lifecycle"
   "diff-incompat"
   "diff-compat"
   "diff-keys"
   "invalidate"
   "signals"
   "rows"])

(defn app-impl []
  (let [*selected (ratom (first examples))]
    (fn []
      [split
       [column
        (for [name examples
              :let [lbl [padding {:padding (:padding *ctx*)}
                         [label name]]]]
          (if (= @*selected name)
            ^{:key name} [rect {:fill (:hui.button/bg *ctx*)} lbl]
            ^{:key name} [clickable {:on-click (fn [_] (reset! *selected name))} lbl]))]
       [center
        [@(ns-resolve 'vdom-2 (symbol (str "example-" @*selected)))]]])))

(def app
  (ui/default-theme
    {:cap-height 10}
    (ui/with-context
      {:features (.withFeatures ShapingOptions/DEFAULT "tnum")
       :padding  10}
      (make [app-impl]))))
