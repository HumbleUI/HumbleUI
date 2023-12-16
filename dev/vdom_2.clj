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

(defn maybe-render [comp ctx]
  (binding [*ctx* ctx]
    (when (:dirty? comp)
      (protocols/-reconcile-impl comp (:el comp))
      (core/set!! comp :dirty? false))))

(defn make-record [^Class class]
  (let [ns   (str/replace (.getPackageName class) "_" "-")
        name (str "map->" (.getSimpleName class))
        ctor (ns-resolve (symbol ns) (symbol name))]
    (ctor {})))

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
                       :render     (:render res)
                       :on-unmount (:on-unmount res)
                       :dirty?     true)
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
    #(core/when-every [{:keys [on-unmount]} %]
       (on-unmount))))

(defn reconcile [old-comps new-els]
  (loop [old-comps-keyed (->> old-comps
                           (filter :key)
                           (map #(vector (:key %) %))
                           (into {}))
         old-comps       (remove :key old-comps)
         new-els   new-els
         res       []]
    (core/cond+
      (empty? new-els)
      (do
        (doseq [[_ comp] (concat old-comps old-comps-keyed)]
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
        (protocols/-reconcile-impl old-comp new-el)
        (recur old-comps-keyed old-comps' new-els' (conj res old-comp)))
      
      ;; old-comp was dropped
      (compatible? (first old-comps') new-el)
      (let [_ (unmount old-comp)
            [old-comp & old-comps'] old-comps']
        (protocols/-reconcile-impl old-comp new-el)
        (recur old-comps-keyed (next old-comps') new-els' (conj res old-comp)))
      
      ;; new-el was inserted
      (compatible? old-comp (first new-els'))
      (let [new-comp (make new-el)]
        (recur old-comps-keyed old-comps new-els' (conj res new-comp)))
      
      ;; just incompatible
      :else
      (let [new-comp (make new-el)]
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
  protocols/IComponent
  (-measure-impl [this ctx cs]
    (core/measure child ctx cs))

  (-draw-impl [this ctx rect canvas]
    (core/draw child ctx rect canvas))
  
  (-event-impl [this ctx event]
    (core/event child ctx event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (core/iterate-child child ctx cb)))
  
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
                            ^:mut on-unmount
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

(defmacro defcomponent [name & rest]
  `(do
     (core/deftype+ ~name ~@rest)
     (defn ~(symbol (str/lower-case (str name))) [& args#]
       (~(symbol (str "map->" name)) {}))))

(defcomponent Label [^:mut ^TextLine line]
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

(declare row)

(defn label [& children]
  (if (= 1 (count children))
    [a-label (first children)]
    [row
     (map #(vector a-label %) children)]))

(defcomponent Center []
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

(defcomponent Clickable []
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

(defcomponent Padding []
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

(defcomponent Rect []
  :extends AWrapper4
  protocols/IComponent  
  (-draw-impl [this ctx rect canvas]
    (let [[opts _] (maybe-opts (next el))]
      (canvas/draw-rect canvas rect (:fill opts))
      (core/draw-child child ctx rect canvas))))

(defcomponent Column []
  :extends AContainer4
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [gap (* (:scale ctx) (:padding ctx))]
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
    (let [gap   (* (:scale ctx) (:padding ctx))
          width (:width rect)]
      (loop [children children
             top      (:y rect)]
        (when-some [child (first children)]
          (let [size (protocols/-measure child ctx (core/isize (:width rect) (:height rect)))
                x    (+ (:x rect) (/ (- width (:width size)) 2))]
            (protocols/-draw child ctx (core/irect-xywh x top (:width size) (:height size)) canvas)
            (recur (next children) (+ top (:height size) gap))))))))

(defcomponent Row []
  :extends AContainer4
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [gap (* (:scale ctx) (:padding ctx))]
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
    (let [gap (* (:scale ctx) (:padding ctx))
          height   (:height rect)]
      (loop [children children
             left     (:x rect)]
        (when-some [child (first children)]
          (let [size (protocols/-measure child ctx (core/isize (:width rect) (:height rect)))
                y    (+ (:y rect) (/ (- height (:height size)) 2))]
            (protocols/-draw child ctx (core/irect-xywh left y (:width size) (:height size)) canvas)
            (recur (next children) (+ left (:width size) gap))))))))

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

(defn timer [init]
  (let [*state (ratom init)
        timer  (core/schedule
                 #(swap! *state inc) 0 1000)]
    {:on-unmount
     (fn [] (timer))
     :render
     (fn [_]
       [label "Timer" @*state "sec"])}))

(defn item [*state id]
  (println "mount" id)
  {:on-unmount (fn [] (println "unmount" id))
   :render
   (fn [*state id]
     [row
      [label "Id" id "Clicks" (@*state id)]
      [button {:on-click (fn [_] (swap! *state update id inc))}
       [label "INC"]]
      [button {:on-click (fn [_] (swap! *state dissoc id))}
       [label "DEL"]]])})

(defn app-impl []
  (let [*state (ratom (into (sorted-map) (map #(vector % 0) (range 3))))]
    (fn []
      [center
       [column
        [timer 0]
        (for [k (keys @*state)]
          ^{:key k} [item *state k])
        [button {:on-click (fn [_] (swap! *state assoc (inc (reduce max 0 (keys @*state))) 0))}
         [label "ADD"]]]])))

(def app
  (ui/default-theme
    {:cap-height 10}
    (ui/with-context
      {:features (.withFeatures ShapingOptions/DEFAULT "tnum")
       :padding  10}
      (make [app-impl]))))
