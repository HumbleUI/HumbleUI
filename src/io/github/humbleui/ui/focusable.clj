(ns io.github.humbleui.ui.focusable
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.ui.listeners :as listeners])
  (:import
    [java.lang AutoCloseable]))

(core/deftype+ Focusable [child
                          on-focus
                          on-blur
                          ^:mut child-rect
                          ^:mut focused?]
  protocols/IContext
  (-context [_ ctx]
    (cond-> ctx focused?
      (assoc :hui/focused? true)))
  
  protocols/IComponent
  (-measure [this ctx cs]
    (core/measure child (protocols/-context this ctx) cs))
  
  (-draw [this ctx rect canvas]
    (some-> (::*focused ctx)
      (cond->
        focused? (vswap! conj this)))
    (set! child-rect rect)
    (core/draw-child child (protocols/-context this ctx) child-rect canvas))
  
  (-event [this ctx event]
    (core/eager-or
      (when (and
              (= :mouse-button (:event event))
              (:pressed? event)
              (not focused?)
              (core/rect-contains? child-rect (core/ipoint (:x event) (:y event))))
        (set! focused? true)
        (when on-focus
          (on-focus))
        true)
      (let [event' (cond-> event
                     focused? (assoc :hui/focused? true))]
        (core/event-child child (protocols/-context this ctx) event'))))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child (protocols/-context this ctx) cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn focusable
  ([child]
   (focusable child false))
  ([{:keys [focused? on-focus on-blur]} child]
   (->Focusable child on-focus on-blur nil focused?)))

(defn focused [this]
  (let [*acc (volatile! [])]
    (protocols/-iterate this nil
      (fn [comp]
        (when (and (instance? Focusable comp) (:focused? comp))
          (vswap! *acc conj comp)
          false)))
    @*acc))

(core/deftype+ FocusController [child ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))
  
  (-draw [_ ctx rect canvas]
    (set! child-rect rect)
    (let [*focused (volatile! [])
          ctx'     (assoc ctx ::*focused *focused)
          res      (core/draw-child child ctx' child-rect canvas)]
      (doseq [comp (butlast @*focused)]
        (protocols/-set! comp :focused? false)
        (core/invoke (:on-blur comp)))
      res))
  
  (-event [this ctx event]
    (if (and
          (= :mouse-button (:event event))
          (:pressed? event)
          (core/rect-contains? child-rect (core/ipoint (:x event) (:y event))))
      (let [focused-before (focused this)
            res            (core/event-child child ctx event)
            focused-after  (focused this)]
        (when (< 1 (count focused-after))
          (doseq [comp focused-before]
            (protocols/-set! comp :focused? false)
            (core/invoke (:on-blur comp))))
        (or
          res
          (< 1 (count focused-after))))
      (core/event-child child ctx event)))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn focus-prev [this]
  (let [*prev    (volatile! nil)
        *focused (volatile! nil)]
    (protocols/-iterate this nil
      (fn [comp]
        (when (instance? Focusable comp)
          (if (:focused? comp)
            (do
              (vreset! *focused comp)
              (some? @*prev))
            (do
              (vreset! *prev comp)
              false)))))
    (when-some [focused @*focused]
      (protocols/-set! focused :focused? false)
      (core/invoke (:on-blur focused)))
    (when-some [prev @*prev]
      (protocols/-set! prev :focused? true)
      (core/invoke (:on-focus prev)))))

(defn focus-next [this]
  (let [*first   (volatile! nil)
        *focused (volatile! nil)
        *next    (volatile! nil)]
    (protocols/-iterate this nil
      (fn [comp]
        (when (instance? Focusable comp)
          (when (nil? @*first)
            (vreset! *first comp)
            false)
          (if (:focused? comp)
            (do
              (vreset! *focused comp)
              false)
            (when @*focused
              (vreset! *next comp)
              true)))))
    (when-some [focused @*focused]
      (protocols/-set! focused :focused? false)
      (core/invoke (:on-blur focused)))
    (when-some [next (or @*next @*first)]
      (protocols/-set! next :focused? true)
      (core/invoke (:on-focus next)))))

(defn focus-controller [child]
  (let [this (->FocusController child nil)]
    (listeners/key-listener
      {:on-key-down
       (fn [e]
         (when (= :tab (:key e))
           (if (:shift (:modifiers e))
             (focus-prev this)
             (focus-next this))))}
      this)))
        