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

(defn rerender [comp]
  (core/set!! comp :dirty? true)
  (state/request-frame))

(def padding
  10)

(def ctor-border
  (paint/stroke 0x80FF00FF 2))

(defn flatten [xs]
  (mapcat #(if (and (not (vector? %)) (sequential? %)) (flatten %) [%]) xs))

(defn maybe-opts [vals]
  (if (map? (first vals))
    [(first vals) (next vals)]
    [{} vals]))

(defn make [el]
  (if (satisfies? protocols/IComponent el)
    el
    (binding [*comp* (map->FnComponent {})]
      (let [[f & args] el
            res  (apply f args)
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
               
                   (satisfies? protocols/IComponent res)
                   res
               
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
   ^:mut key]
  
  protocols/IComponent
  (-measure [this ctx cs]
    (protocols/-measure-impl this ctx cs))
    
  (-draw [this ctx rect canvas]
    (set! self-rect rect)
    (protocols/-draw-impl this ctx rect canvas)
    (when-not mounted?
      (canvas/draw-rect canvas (-> ^IRect rect .toRect (.inflate 4)) ctor-border)
      (set! mounted? true)))
  
  (-event [this ctx event]
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
  (-measure-impl [_ ctx cs]
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

(defn maybe-render [comp ctx]
  (binding [*ctx* ctx]
    (when (:dirty? comp)
      (protocols/-reconcile-impl comp (:el comp))
      (core/set!! comp :dirty? false))))

(core/deftype+ FnComponent [^:mut render
                            ^:mut on-unmount
                            ^:mut child
                            ^:mut dirty?]
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

(defn label [& args]
  (let [[opts texts] (maybe-opts args)
        font         (or (:font opts) (:font-ui *ctx*))
        features     (or (:features opts) (:features *ctx*) ShapingOptions/DEFAULT)
        line         (.shapeLine core/shaper (str/join texts) font features)
        size         (core/ipoint
                       (math/ceil (.getWidth line))
                       (* (:scale *ctx*) (:leading *ctx*)))]
    (map->Label {:line line})))

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

(defn center [child-el]
  (map->Center {:child (make child-el)}))

(core/deftype+ OnClick []
  :extends AWrapper4
  protocols/IComponent  
  (-event-impl [this ctx event]
    (when (and
            (= :mouse-button (:event event))
            (:pressed? event)
            (core/rect-contains? self-rect (core/ipoint (:x event) (:y event))))
      ((nth el 1) event))
    (core/event-child child ctx event))
  
  protocols/IVDom
  (-reconcile-impl [_ el']
    (let [[_ _ child-el] el'
          [child'] (reconcile [child] [child-el])]
      (set! child child')
      (set! el el'))))

(defn on-click [cb child-el]
  (map->OnClick
    {:child (make child-el)}))

(core/deftype+ Column []
  :extends AContainer4
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [gap (* (:scale ctx) padding)]
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
    (let [gap   (* (:scale ctx) padding)
          width (:width rect)]
      (loop [children children
             top      (:y rect)]
        (when-some [child (first children)]
          (let [size (protocols/-measure child ctx (core/isize (:width rect) (:height rect)))
                x    (+ (:x rect) (/ (- width (:width size)) 2))]
            (protocols/-draw child ctx (core/irect-xywh x top (:width size) (:height size)) canvas)
            (recur (next children) (+ top (:height size) gap))))))))

(defn column [& child-els]
  (map->Column {:children (mapv make (flatten child-els))}))

(core/deftype+ Row []
  :extends AContainer4
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [gap (* (:scale ctx) padding)]
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
    (let [gap (* (:scale ctx) padding)
          height   (:height rect)]
      (loop [children children
             left     (:x rect)]
        (when-some [child (first children)]
          (let [size (protocols/-measure child ctx (core/isize (:width rect) (:height rect)))
                y    (+ (:y rect) (/ (- height (:height size)) 2))]
            (protocols/-draw child ctx (core/irect-xywh left y (:width size) (:height size)) canvas)
            (recur (next children) (+ left (:width size) gap))))))))

(defn row [& child-els]
  (map->Row {:children (mapv make (flatten child-els))}))

(defn ratom [init]
  (let [comp *comp*
        res  (atom init)]
    (add-watch res ::rerender
      (fn [_ _ old new]
        (when (not= old new)
          (rerender comp))))
    res))

(defn timer [init]
  (let [*state (ratom init)
        timer  (core/schedule
                 #(swap! *state inc) 0 1000)]
    {:on-unmount
     (fn [] (timer))
     :render
     (fn [_]
       [label "Timer: " @*state " sec"])}))

(defn item [*state id]
  (println "mount" id)
  {:on-unmount (fn [] (println "unmount" id))
   :render
   (fn [*state id]
     [row
      [label "Row: " id ", clicks: " (@*state id)]
      [on-click (fn [_] (swap! *state update id inc)) [label "[ INC ]"]]
      [on-click (fn [_] (swap! *state dissoc id)) [label "[ DEL ]"]]])})

(defn app-impl []
  (let [*state (ratom (into (sorted-map) (map #(vector % 0) (range 3))))]
    {:render
     (fn []
       [center
        [column
         [timer 0]
         (for [k (keys @*state)]
           ^{:key k} [item *state k])
         [on-click (fn [_] (swap! *state assoc (inc (reduce max 0 (keys @*state))) 0))
          [label "[ ADD ]"]]]])}))

(def app
  (ui/default-theme
    {:cap-height 10}
    (ui/with-context
      {:features (.withFeatures ShapingOptions/DEFAULT "tnum")}
      (make [app-impl]))))
