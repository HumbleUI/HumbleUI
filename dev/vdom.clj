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

;; Constants

(def padding
  10)

;; Utils

(defn flatten [xs]
  (mapcat #(if (and (not (vector? %)) (sequential? %)) % [%]) xs))

;; base classes

(core/defparent AComponent3
  [^:mut props ^:mut mounted?]
  protocols/IComponent
  (-measure [this ctx cs]
    (protocols/-measure-impl this ctx cs))
    
  (-draw [this ctx rect canvas]
    (protocols/-draw-impl this ctx rect canvas))
  
  (-event [this ctx event]
    (protocols/-event-impl this ctx event)))

(core/defparent ATerminal3 []
  :extends AComponent3
  protocols/IComponent
  (-event-impl [this ctx event]))

(core/defparent AWrapper3 [^:mut child]
  :extends AComponent3
  protocols/IComponent
  (-measure-impl [this ctx cs]
    (core/measure child ctx cs))
  
  (-event-impl [this ctx event]
    (core/event-child child ctx event)))

(core/defparent AContainer3 [^:mut children]
  :extends AComponent3
  protocols/IComponent  
  (-event-impl [this ctx event]
    (reduce #(core/eager-or %1 (protocols/-event %2 ctx event)) nil children)))

(derive ::AWrapper3 ::ATerminal3)
(derive ::AContainer3 ::AWrapper3)

;; parse-desc

(defmulti parse-desc (fn [desc] (first desc)))

(defmethod parse-desc ::ATerminal3 [[tag & body]]
  (if (map? (first body))
    {:tag   tag
     :props (assoc (first body)
              :args (next body))}
    {:tag   tag
     :props {:args body}}))

(defmethod parse-desc ::AWrapper3 [[tag & body]]
  (if (map? (first body))
    {:tag   tag
     :props (first body)
     :child (second body)}
    {:tag   tag
     :child (first body)}))

(defmethod parse-desc ::AContainer3 [[tag & body]]
  (if (map? (first body))
    {:tag      tag
     :props    (first body)
     :children (next body)}
    {:tag      tag
     :children body}))

;; compatible?

(defmulti compatible? (fn [ctx past desc] (first desc)))

(defmethod compatible? :default [ctx past desc]
  (let [{:keys [tag props]} (parse-desc desc)]
    (and
      (= (class past) tag)
      (= (:props past) props))))

;; ctor

(defmulti ctor (fn [ctx desc] (first desc)))

(defmethod ctor ::AWrapper3 [ctx desc]
  (let [{:keys [tag props]} (parse-desc desc)
        sym  (symbol (str "map->" (.getSimpleName ^Class tag)))
        ctor (ns-resolve 'vdom sym)]
    (ctor props)))

;; components

;; Label

(core/deftype+ Label [^:mut text
                      ^:mut font
                      ^:mut ^TextLine line]
  :extends ATerminal3
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

(derive Label ::ATerminal3)
  
(defmethod ctor Label [ctx past desc]
  (let [props    (parse-desc desc)
        text     (str/join (:args props))
        font     (:font-ui ctx)
        features (:features props ShapingOptions/DEFAULT)]
    (map->Label
      {:props props
       :font  font
       :line  (.shapeLine core/shaper text font features)})))
        
;; Center

(core/deftype+ Center []
  :extends AWrapper3
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

(derive Center ::AWrapper3)

;; Column

(core/deftype+ Column []
  :extends AContainer3
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

(derive Column ::AContainer3)

;; Row

(core/deftype+ Row []
  :extends AContainer3
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

(derive Row ::AContainer3)

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
                                (compatible? (first past-coll) current)
                                [(first past-coll) (next past-coll)]
              
                                ;; inserted new widget
                                (compatible? (first past-coll) (fnext current-coll))
                                [nil past-coll]
                                
                                ;; deleted a widget
                                (compatible? (fnext past-coll) current)
                                [(fnext past-coll) (nnext past-coll)]
                                
                                ;; no match
                                :else
                                [nil (next past-coll)])
            future            (or
                                past
                                (ctor (:ctor current) (:props current) ctx))
            children          (reconcile ctx (:children past) (:children current))]
        (protocols/-set! future :children children)
        (recur past-coll' current-coll' (conj future-coll future))))))

(defn ensure-child [reconciler ctx]
  (let [[child'] (reconcile ctx [(:child reconciler)] [((:ctor reconciler))])]
    (protocols/-set! reconciler :child child')))

(core/deftype+ Reconciler [ctor ^:mut child]
  protocols/IComponent
  (-measure [this ctx cs]
    (ensure-child this ctx)
    (core/measure child ctx cs))
  
  (-draw [this ctx rect canvas]
    (ensure-child this ctx)
    (core/draw-child child ctx rect canvas)
    (state/request-frame))

  (-event [this ctx event]
    (ensure-child this ctx)
    (core/event-child child ctx event))

  (-iterate [this ctx cb]
    (ensure-child this ctx)
    (or
      (cb this)
      (core/iterate-child child ctx cb))))

(defn reconciler [ctor]
  (map->Reconciler
    {:ctor ctor}))

(def *state
  (atom
    [{:id 0 :count 0}
     {:id 1 :count 0}
     {:id 2 :count 0}]))

(defn app []
  [Center
   [Column
    (for [% @*state]
      [Row
       [Label (str "id: " (:id %) ", count: " (:count %))]])]])
