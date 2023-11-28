;; Run with ./script/vdom.sh

(ns vdom
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

(declare reconciler ensure-children)

;; Constants

(def padding
  20)


;; Utils

(defn flatten [xs]
  (mapcat #(if (and (not (vector? %)) (sequential? %)) % [%]) xs))

(defn single [xs]
  (assert (<= (count xs) 1) (str "Expected one, got: " (doall xs)))
  (first xs))

(defn some-vec [& xs]
  (filterv some? xs))


;; base classes

(def ctor-border
  (paint/stroke 0x80FF00FF 2))

(core/defparent AComponent3
  [^:mut desc
   ^:mut props
   ^:mut children
   ^:mut mounted?
   ^:mut self-rect]
  protocols/IComponent
  (-measure [this ctx cs]
    (protocols/-measure-impl this ctx cs))
  
  (-measure-impl [this ctx cs]
    (core/measure (single children) ctx cs))
  
  (-draw [this ctx rect canvas]
    (set! self-rect rect)
    (protocols/-draw-impl this ctx rect canvas)
    (when-not mounted?
      (canvas/draw-rect canvas (-> ^IRect rect .toRect (.inflate 4)) ctor-border)
      (set! mounted? true)))

  (-draw-impl [this ctx rect canvas]
    (core/draw-child (single children) ctx rect canvas))
  
  (-event [this ctx event]
    (protocols/-event-impl this ctx event))
  
  (-event-impl [this ctx event]
    (reduce #(core/eager-or %1 (protocols/-event %2 ctx event)) nil children)))


;; parse-desc

(defn parse-desc [[tag & body]]
  (if (map? (first body))
    {:tag      tag
     :props    (first body)
     :children (next body)}
    {:tag      tag
     :children body}))

;; compatible?

(defmulti compatible-impl? (fn [past desc] (first desc)))

(defmethod compatible-impl? :default [past desc]
  true)

(defn compatible? [past desc]
  (and past desc
    (let [{:keys [tag]} (parse-desc desc)]
      (cond
        (identical? (:desc past) desc)
        true
        
        (class? tag)
        (and
          (= (class past) tag)
          (compatible-impl? past desc))
      
        (ifn? tag)
        (= (:ctor past) tag)
      
        :else
        (throw (ex-info "I’m confused" {:past past, :desc desc}))))))

;; upgrade

(defn upgrade [past desc]
  (let [{:keys [tag props]} (parse-desc desc)]
    (protocols/-set! past :desc desc)
    (protocols/-set! past :props props))
  past)

;; ctor

(defn ctor [desc]
  (let [{:keys [tag props children]} (parse-desc desc)]
    (cond
      (class? tag)
      (let [sym  (symbol (str "map->" (.getSimpleName ^Class tag)))
            ctor (ns-resolve 'vdom sym)]
        (ctor {:desc desc
               :props props}))
      
      (ifn? tag)
      (reconciler desc)
      
      :else
      (throw (ex-info "I’m confused" {:desc desc})))))


;; components

;; Label

(defn ensure-line [label ctx]
  (when (nil? (:line label))
    (let [props    (:props label)
          text     (:text props)
          font     (:font-ui ctx)
          features (or (:features props) (:features ctx) ShapingOptions/DEFAULT)]
      (protocols/-set! label :line (.shapeLine core/shaper text font features)))))

(core/deftype+ Label [^:mut ^TextLine line]
  :extends AComponent3
  protocols/IComponent
  (-measure-impl [this ctx cs]
    (ensure-line this ctx)
    (core/ipoint
      (math/ceil (.getWidth line))
      (* (:scale ctx) (:leading ctx))))
  
  (-draw-impl [this ctx rect ^Canvas canvas]
    (ensure-line this ctx)
    (.drawTextLine canvas
      line
      (:x rect)
      (+ (:y rect) (* (:scale ctx) (:leading ctx)))
      (:fill-text ctx)))
  
  protocols/ILifecycle
  (-on-unmount-impl [_]
    (.close line)))
  
(defmethod compatible-impl? Label [past desc]
  (= (:props past) (:props (parse-desc desc))))


;; Center

(core/deftype+ Center []
  :extends AComponent3
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    cs)

  (-draw-impl [this ctx rect canvas]
    (let [child      (single children)
          w          (:width rect)
          h          (:height rect)
          child-size (protocols/-measure child ctx (core/isize w h))
          cw         (:width child-size)
          ch         (:height child-size)
          rect'      (core/irect-xywh
                       (-> (:x rect) (+ (/ w 2)) (- (/ cw 2)))
                       (-> (:y rect) (+ (/ h 2)) (- (/ ch 2)))
                       cw ch)]
      (protocols/-draw child ctx rect' canvas))))


;; OnClick

(core/deftype+ OnClick []
  :extends AComponent3
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (protocols/-measure (single children) ctx cs))

  (-draw-impl [this ctx rect canvas]
    (protocols/-draw (single children) ctx rect canvas))
  
  (-event-impl [this ctx event]
    (when (and
            (= :mouse-button (:event event))
            (:pressed? event)
            (core/rect-contains? self-rect (core/ipoint (:x event) (:y event))))
      ((:on-click props) event))
    (core/event-child (single children) ctx event)))


;; Padding

(core/deftype+ Padding []
  :extends AComponent3
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [scale  (:scale ctx)
          left   (* scale (or (:left props)   (:horizontal props) (:padding props) 0))
          right  (* scale (or (:right props)  (:horizontal props) (:padding props) 0))
          top    (* scale (or (:top props)    (:vertical props)   (:padding props) 0))
          bottom (* scale (or (:bottom props) (:vertical props)   (:padding props) 0))
          cs'    (core/ipoint
                   (- (:width cs) left right)
                   (- (:height cs) top bottom))
          size'  (core/measure (single children) ctx cs')]
      (core/ipoint
        (+ (:width size') left right)
        (+ (:height size') top bottom))))

  (-draw-impl [this ctx rect canvas]
    (let [scale  (:scale ctx)
          left   (* scale (or (:left props)   (:horizontal props) (:padding props) 0))
          right  (* scale (or (:right props)  (:horizontal props) (:padding props) 0))
          top    (* scale (or (:top props)    (:vertical props)   (:padding props) 0))
          bottom (* scale (or (:bottom props) (:vertical props)   (:padding props) 0))
          rect'  (core/irect-ltrb
                   (+ (:x rect) left)
                   (+ (:y rect) top)
                   (- (:right rect) right)
                   (- (:bottom rect) bottom))]
      (protocols/-draw (single children) ctx rect' canvas))))


;; Rect

(core/deftype+ Rect []
  :extends AComponent3
  protocols/IComponent  
  (-draw-impl [this ctx rect canvas]
    (canvas/draw-rect canvas rect (:fill props))
    (core/draw-child (single children) ctx rect canvas)))


;; Column

(core/deftype+ Column []
  :extends AComponent3
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

;; Row

(core/deftype+ Row []
  :extends AComponent3
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

;; App

(defn unmount [ctx comp]
  (doseq [child (:children comp)]
    (unmount ctx child))
  (doseq [*state (:state comp)
          :let [[type _ on-unmount] @*state]
          :when (= :effect type)
          :when on-unmount]
    (on-unmount)))

(defn reconcile [ctx past current]
  (loop [past-coll    (remove #(:key (:props %)) past)
         past-keyed   (into {}
                        (keep #(when-some [key (:key (:props %))] [key %]) past))
         current-coll (flatten current)
         unmount-coll []
         future-coll  []]
    (if (empty? current-coll)
      (do
        (doseq [comp (concat unmount-coll (vals past-keyed) past-coll)]
          (unmount ctx comp))
        future-coll)
      (let [current           (first current-coll)
            {:keys [tag props children]} (parse-desc current)
            current-coll'     (next current-coll)
            past              (past-keyed (:key props))
              
            [past past-coll' past-keyed' unmount-coll']
            (core/cond+
              ;; key match
              (and past (compatible? past current))
              [past past-coll (dissoc past-keyed (:key props)) unmount-coll]
              
              ;; key but no match
              past
              [nil past-coll (dissoc past-keyed (:key props)) (conj unmount-coll past)]
                                  
              ;; full match
              (compatible? (first past-coll) current)
              [(first past-coll) (next past-coll) past-keyed unmount-coll]
                                
              ;; inserted new widget
              (compatible? (first past-coll) (fnext current-coll))
              [nil past-coll past-keyed unmount-coll]
                                
              ;; deleted a widget
              (compatible? (fnext past-coll) current)
              [(fnext past-coll) (nnext past-coll) past-keyed (conj unmount-coll (first past-coll))]
                                
              ;; no match
              :else
              [nil (next past-coll) past-keyed (conj unmount-coll (first past-coll))])
              
            [future skip?]
            (cond
              (nil? past)
              [(ctor current) false]
                                  
              (identical? (:desc past) current)
              [past true]
                                  
              :else
              [(upgrade past current) false])]
        (cond 
          skip?
          :nop
                
          (class? tag)
          (protocols/-set! future :children (reconcile ctx (:children past) children))
            
          (ifn? tag)
          (ensure-children ctx future)
            
          :else
          (throw (ex-info "I’m confused" {:past past, :desc current})))
        (recur past-coll' past-keyed' current-coll' unmount-coll' (conj future-coll future))))))

(def ^:dynamic *ctx*)

(def ^:dynamic *reconciler*)

(def ^:dynamic *state-idx*)

(defn ensure-children [ctx reconciler]
  (binding [*ctx*        ctx
            *reconciler* reconciler
            *state-idx*  (volatile! 0)]
    (let [children' (reconcile
                      ctx
                      (:children reconciler)
                      [((:ctor reconciler)
                        (:props reconciler)
                        (:children-desc reconciler))])]
      (protocols/-set! reconciler :children children')))
  reconciler)

(core/deftype+ Reconciler [^:mut desc
                           ^:mut ctor
                           ^:mut props
                           ^:mut children
                           ^:mut state
                           ^:mut dirty?
                           ^:mut children-desc]
  protocols/IComponent
  (-measure [this ctx cs]
    (core/measure (single children) ctx cs))
  
  (-draw [this ctx rect canvas]
    (when dirty?
      (ensure-children ctx this)
      (set! dirty? false))
    (core/draw-child (single children) ctx rect canvas))

  (-event [this ctx event]
    (core/event-child (single children) ctx event))

  (-iterate [this ctx cb]
    (or
      (cb this)
      (core/iterate-child (single children) ctx cb))))

(defn reconciler [desc]
  (let [{:keys [tag props children]} (parse-desc desc)]
    (map->Reconciler
      {:desc          desc
       :ctor          tag
       :props         props
       :dirty?        true
       :children-desc children})))

;; Hooks

(defn use-ref-impl [init-fn]
  (let [comp  *reconciler*
        state (:state comp)
        idx   @*state-idx*
        _     (cond
                (nil? state)
                (protocols/-set! comp :state [(volatile! (init-fn))])
                
                (< (dec (count state)) idx)
                (protocols/-set! comp :state (conj state (volatile! (init-fn)))))
        *a    (nth (:state comp) idx)]
    (vswap! *state-idx* inc)
    *a))

(defn use-state [init]
  (let [*a      (use-ref-impl (fn [] [:state init]))
        comp    *reconciler*
        [_ val] @*a]
    [val #(do
            (vreset! *a [:state %])
            (protocols/-set! comp :dirty? true)
            (state/request-frame))]))

(defn use-memo [f deps]
  (let [*a (use-ref-impl (fn [] [:memo deps (apply f deps)]))
        [_ old-deps old-val] @*a]
    (if (= old-deps deps)
      old-val
      (let [new-val (apply f deps)]
        (vreset! *a [:memo deps new-val])
        new-val))))

(defn use-effect [f deps]
  (let [*a                      (use-ref-impl (fn [] nil))
        [_ old-deps on-unmount] @*a
        comp                    *reconciler*]
    (when (or (nil? deps) (nil? old-deps) (not= old-deps deps))
      (when on-unmount
        (on-unmount))
      (let [on-unmount' (f)]
        (if (ifn? on-unmount')
          (vreset! *a [:effect deps on-unmount'])
          (vreset! *a [:effect deps nil]))))))

(defn use-callback
  ([f]
   (use-callback f []))
  ([f deps]
   (use-memo (fn [] f) deps)))

;; API

(defn render [mount desc]
  (let [{:keys [tag props children]} (parse-desc desc)]
    (protocols/-set! mount :desc          desc)
    (protocols/-set! mount :ctor          tag)
    (protocols/-set! mount :props         props)
    (protocols/-set! mount :dirty?        true)
    (protocols/-set! mount :children-desc children)
    (state/request-frame)))

(def app-root
  (reconciler nil))

(def app
  (ui/default-theme
    {:cap-height 15}
    (ui/with-context
      {:features (.withFeatures ShapingOptions/DEFAULT "tnum")}
      app-root)))

;; User app

(def *state
  (atom
    (sorted-map 0 0, 1 0, 2 0)))

(add-watch *state ::redraw
  (fn [_ _ _ _]
    (render app-root [App])))

(defn Button [{:keys [on-click]} children]
  [OnClick {:on-click on-click}
   [Rect {:fill (:hui.button/bg *ctx*)}
    [Padding {:padding 10}
     children]]])

(defn Item [{:keys [id count]} _]
  (let [[local1 set-local1] (use-state 0)
        [local2 set-local2] (use-state 0)]
    (use-effect
      #(do
         (println "mount" id)
         (fn []
           (println "unmount" id)))
      [])
    (use-effect #(println "render" id) nil)
    (use-effect #(println "change" id count) [id count])
    [Row
     [Label {:text (str "Id: " id)}]
     [Button {:on-click (fn [_] (swap! *state dissoc id))}
      [Label {:text "DEL"}]]
     [Label {:text (str "Global: " count)}]
     [Button {:on-click (fn [_] (swap! *state update id inc))}
      [Label {:text "INC"}]]
     [Label {:text (str "Local 1: " local1)}]
     [Button {:on-click (fn [_] (set-local1 (inc local1)))}
      [Label {:text "INC"}]]
     [Label {:text (str "Local 2: " local2)}]
     [Button {:on-click (fn [_] (set-local2 (inc local2)))}
      [Label {:text "INC"}]]]))

(defn MemoItem [{:keys [id count]} _]
  (use-memo
    (fn [id count]
      [Item {:id id, :count count}])
    [id count]))

(defn App [_ _]
  [Center
   [Column
    (for [[id count] @*state]
      [MemoItem {:key id, :id id, :count count}])
    (let [cb (use-callback
               (fn [_]
                 (swap! *state assoc ((fnil inc -1) (last (keys @*state))) 0)))]
      [Row {:key :last}
       [Button {:on-click cb}
        [Label {:text "ADD"}]]])]])

(render app-root [App])

