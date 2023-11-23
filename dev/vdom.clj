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
    [io.github.humbleui.ui.theme :as theme]
    [io.github.humbleui.window :as window]
    [state :as state])
  (:import
    [io.github.humbleui.types IRect]
    [io.github.humbleui.skija Canvas TextLine]
    [io.github.humbleui.skija.shaper ShapingOptions]))

(declare reconciler)

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

(defn after-draw [comp ctx rect canvas]
  (when-not (:mounted? comp)
    (canvas/draw-rect canvas (-> ^IRect rect .toRect (.inflate 4)) (paint/stroke 0x80FF00FF 2)))
  (protocols/-set! comp :mounted? true))

(core/defparent AComponent3
  [^:mut props ^:mut children ^:mut mounted?]
  protocols/IComponent
  (-measure [this ctx cs]
    (protocols/-measure-impl this ctx cs))
    
  (-draw [this ctx rect canvas]
    (protocols/-draw-impl this ctx rect canvas)
    (after-draw this ctx rect canvas))
  
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

(defmulti compatible? (fn [ctx past desc] (first desc)))

(defmethod compatible? :default [ctx past desc]
  (and past desc
    (let [{:keys [tag]} (parse-desc desc)]
      (cond
        (class? tag)
        (= (class past) tag)
      
        (ifn? tag)
        (= (:ctor past) tag)
      
        :else
        (throw (ex-info "I’m confused" {:past past, :desc desc}))))))

    
;; upgrade

(defmulti upgrade (fn [ctx past desc] (first desc)))

(defmethod upgrade :default [ctx past desc]
  (let [{:keys [tag props]} (parse-desc desc)]
    (cond
      (class? tag)
      (doseq [[k v] props]
        (protocols/-set! past k v))
      
      (ifn? tag)
      (protocols/-set! past :props props)
      
      :else
      (throw (ex-info "I’m confused" {:past past, :desc desc}))))
  past)

;; ctor

(defmulti ctor (fn [ctx desc] (first desc)))

(defmethod ctor :default [ctx desc]
  (println "ctor" desc)
  (let [{:keys [tag props]} (parse-desc desc)]
    (cond
      (class? tag)
      (let [sym  (symbol (str "map->" (.getSimpleName ^Class tag)))
            ctor (ns-resolve 'vdom sym)]
        (ctor props))
      
      (ifn? tag)
      (reconciler tag props)
      
      :else
      (throw (ex-info "I’m confused" {:desc desc})))))


;; components

;; Label

(core/deftype+ Label [^:mut text
                      ^:mut font
                      ^:mut ^TextLine line]
  :extends AComponent3
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (core/ipoint
      (math/ceil (.getWidth line))
      (* (:scale ctx) (:leading ctx))))
  
  (-draw-impl [this ctx rect ^Canvas canvas]
    (.drawTextLine canvas
      line
      (:x rect)
      (+ (:y rect) (* (:scale ctx) (:leading ctx)))
      (:fill-text ctx)))
  
  protocols/ILifecycle
  (-on-unmount-impl [_]
    (.close line)))
  
(defmethod ctor Label [ctx desc]
  (println "ctor" desc)
  (let [props    (:props (parse-desc desc))
        text     (:text props)
        font     (:font-ui ctx)
        features (:features props ShapingOptions/DEFAULT)]
    (map->Label
      {:props props
       :font  font
       :line  (.shapeLine core/shaper text font features)})))

(defmethod compatible? Label [ctx past desc]
  (and
    past
    desc
    (= (class past) (first desc))
    (= (:props past) (:props (parse-desc desc)))))
        

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

(core/deftype+ OnClick [^:mut on-click ^:mut child-rect]
  :extends AComponent3
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (protocols/-measure (single children) ctx cs))

  (-draw-impl [this ctx rect canvas]
    (set! child-rect rect)
    (protocols/-draw (single children) ctx rect canvas))
  
  (-event-impl [this ctx event]
    (when (and
            (= :mouse-button (:event event))
            (:pressed? event)
            (core/rect-contains? child-rect (core/ipoint (:x event) (:y event))))
      (on-click event))
    (core/event-child (single children) ctx event)))

(derive OnClick ::AWrapper3)


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
    (let [gap (* (:scale ctx) padding)]
      (loop [children children
             top      (:y rect)]
        (when-some [child (first children)]
          (let [size (protocols/-measure child ctx (core/isize (:width rect) (:height rect)))]
            (protocols/-draw child ctx (core/irect-xywh (:x rect) top (:width size) (:height size)) canvas)
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
    (let [gap (* (:scale ctx) padding)]
      (loop [children children
             left     (:x rect)]
        (when-some [child (first children)]
          (let [size (protocols/-measure child ctx (core/isize (:width rect) (:height rect)))]
            (protocols/-draw child ctx (core/irect-xywh left (:y rect) (:width size) (:height size)) canvas)
            (recur (next children) (+ left (:width size) gap))))))))

;; App

(defn reconcile [ctx past current]
  (loop [past-coll    past
         current-coll (flatten current)
         future-coll  []]
    (if (empty? current-coll)
      future-coll
      (let [current           (first current-coll)
            current-coll'     (next current-coll)
            [past past-coll'] (core/cond+                                
                                ;; full match
                                (compatible? ctx (first past-coll) current)
                                [(first past-coll) (next past-coll)]
                                
                                ;; inserted new widget
                                (compatible? ctx (first past-coll) (fnext current-coll))
                                [nil past-coll]
                                
                                ;; deleted a widget
                                (compatible? ctx (fnext past-coll) current)
                                [(fnext past-coll) (nnext past-coll)]
                                
                                ;; no match
                                :else
                                [nil (next past-coll)])
            future            (if past
                                (do
                                  (println "Upgrade" past "->" current)
                                  (upgrade ctx past current))
                                (ctor ctx current))
            {:keys [tag children]} (parse-desc current)]
        (when (class? tag)
          (protocols/-set! future :children (reconcile ctx (:children past) children)))
        (recur past-coll' current-coll' (conj future-coll future))))))

(defn ensure-children [reconciler ctx]
  (let [children' (reconcile ctx
                    (:children reconciler)
                    [((:ctor reconciler) (:props reconciler))])]
    (protocols/-set! reconciler :children children')))

(core/deftype+ Reconciler [^:mut ctor
                           ^:mut props
                           ^:mut children]
  protocols/IComponent
  (-measure [this ctx cs]
    (ensure-children this ctx)
    (core/measure (single children) ctx cs))
  
  (-draw [this ctx rect canvas]
    (ensure-children this ctx)
    (core/draw-child (single children) ctx rect canvas))

  (-event [this ctx event]
    (core/event-child (single children) ctx event))

  (-iterate [this ctx cb]
    (or
      (cb this)
      (core/iterate-child (single children) ctx cb))))

(defn reconciler
  ([ctor]
   (reconciler ctor {}))
  ([ctor props]
   (map->Reconciler
     {:ctor ctor
      :props props})))

(def *state
  (atom
    [{:id 0 :count 0}
     {:id 1 :count 0}
     {:id 2 :count 0}]))

(add-watch *state ::redraw
  (fn [_ _ _ _]
    (state/request-frame)))

(defn del [xs i]
  (vec (concat (take i xs) (drop (inc i) xs))))

(defn row [{:keys [idx]}]
  (let [{:keys [id count]} (nth @*state idx)]
    [Row
     [OnClick {:on-click (fn [_] (swap! *state del idx))}
      [Label {:text "DEL"}]]
     [Label {:text (str "id: " id ", count: " count)}]
     [OnClick {:on-click (fn [_] (swap! *state update idx update :count inc))}
      [Label {:text "INC"}]]
     [OnClick {:on-click (fn [_] (swap! *state update idx update :count dec))}
      [Label {:text "DEC"}]]]))

(defn root [_]
  [Center
   [Column
    (for [idx (range (count @*state))
          :let [{:keys [id count]} (nth @*state idx)]]
      [row {:idx idx}])
    [Row
     [OnClick {:on-click (fn [_] (swap! *state conj {:id (inc (:id (last @*state) -1)) :count 0}))}
      [Label {:text "ADD"}]]]]])

(def app
  (theme/default-theme
    {:cap-height 15}
    (reconciler root)))

(comment
  (-> app
    :child
    :child
    :child
    :children)
  )