(ns io.github.humbleui.ui.focusable
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.ui.listeners :as listeners])
  (:import
    [java.lang AutoCloseable]))

(core/deftype+ Focusable [on-focus
                          on-blur
                          ^:mut focused]
  :extends core/AWrapper
  
  protocols/IContext
  (-context [_ ctx]
    (cond-> ctx
      focused (assoc :hui/focused? true)))
  
  protocols/IComponent
  (-draw [this ctx rect canvas]
    (some-> (::*focused ctx)
      (cond->
        focused (vswap! conj this)))
    (set! child-rect rect)
    (core/draw-child child (protocols/-context this ctx) child-rect canvas))
  
  (-event [this ctx event]
    (core/eager-or
      (when (and
              (= :mouse-button (:event event))
              (:pressed? event)
              (not focused)
              (core/rect-contains? child-rect (core/ipoint (:x event) (:y event))))
        (set! focused (core/now))
        (when on-focus
          (on-focus))
        true)
      (let [event' (cond-> event
                     focused (assoc :hui/focused? true))]
        (core/event-child child (protocols/-context this ctx) event')))))

(defn focusable
  ([child]
   (map->Focusable
     {:child child}))
  ([{:keys [focused on-focus on-blur] :as opts} child]
   (map->Focusable
     (assoc opts :child child))))

(defn focused [this ctx]
  (let [*acc (volatile! [])]
    (protocols/-iterate this ctx
      (fn [comp]
        (when (and (instance? Focusable comp) (:focused comp))
          (vswap! *acc conj comp)
          false)))
    @*acc))

(core/deftype+ FocusController []
  :extends core/AWrapper
  
  protocols/IComponent
  (-draw [_ ctx rect canvas]
    (set! child-rect rect)
    (let [*focused (volatile! [])
          ctx'     (assoc ctx ::*focused *focused)
          res      (core/draw-child child ctx' child-rect canvas)
          focused  (sort-by :focused @*focused)]
      (doseq [comp (butlast focused)]
        (protocols/-set! comp :focused nil)
        (core/invoke (:on-blur comp)))
      res))
  
  (-event [this ctx event]
    (if (and
          (= :mouse-button (:event event))
          (:pressed? event)
          (core/rect-contains? child-rect (core/ipoint (:x event) (:y event))))
      (let [focused-before (focused this ctx)
            res            (core/event-child child ctx event)
            focused-after  (focused this ctx)]
        (when (< 1 (count focused-after))
          (doseq [comp focused-before]
            (protocols/-set! comp :focused nil)
            (core/invoke (:on-blur comp))))
        (or
          res
          (< 1 (count focused-after))))
      (core/event-child child ctx event))))

(defn focus-prev [this ctx]
  (let [*prev    (volatile! nil)
        *focused (volatile! nil)]
    (protocols/-iterate this ctx
      (fn [comp]
        (when (instance? Focusable comp)
          (if (:focused comp)
            (do
              (vreset! *focused comp)
              (some? @*prev))
            (do
              (vreset! *prev comp)
              false)))))
    (when-some [focused @*focused]
      (protocols/-set! focused :focused nil)
      (core/invoke (:on-blur focused)))
    (when-some [prev @*prev]
      (protocols/-set! prev :focused (core/now))
      (core/invoke (:on-focus prev)))))

(defn focus-next [this ctx]
  (let [*first   (volatile! nil)
        *focused (volatile! nil)
        *next    (volatile! nil)]
    (protocols/-iterate this ctx
      (fn [comp]
        (when (instance? Focusable comp)
          (when (nil? @*first)
            (vreset! *first comp)
            false)
          (if (:focused comp)
            (do
              (vreset! *focused comp)
              false)
            (when @*focused
              (vreset! *next comp)
              true)))))
    (when-some [focused @*focused]
      (protocols/-set! focused :focused nil)
      (core/invoke (:on-blur focused)))
    (when-some [next (or @*next @*first)]
      (protocols/-set! next :focused (core/now))
      (core/invoke (:on-focus next)))))

(defn focus-controller [child]
  (let [this (map->FocusController
               {:child child})]
    (listeners/event-listener {:capture? true} :key
      (fn [e ctx]
        (when (and
                (:pressed? e)
                (= :tab (:key e)))
          (if (:shift (:modifiers e))
            (focus-prev this ctx)
            (focus-next this ctx))
          true))
      this)))
