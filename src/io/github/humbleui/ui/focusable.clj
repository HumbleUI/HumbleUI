(ns io.github.humbleui.ui.focusable
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.ui.clickable :as clickable]
    [io.github.humbleui.ui.key-listener :as key-listener])
  (:import
    [java.lang AutoCloseable]))

(core/deftype+ Focusable [child ^:mut child-rect ^:mut focused?]
  protocols/IContext
  (-context [_ ctx]
    (cond-> ctx focused?
      (assoc :hui/focused? true)))
  
  protocols/IComponent
  (-measure [this ctx cs]
    (core/measure child (protocols/-context this ctx) cs))
  
  (-draw [this ctx ^IRect rect ^Canvas canvas]
    (set! child-rect rect)
    (core/draw-child child (protocols/-context this ctx) child-rect canvas))
  
  (-event [this ctx event]
    (core/event-child child (protocols/-context this ctx) event))
  
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
  ([{:keys [focused?]} child]
   (let [this (->Focusable child nil focused?)]
     (clickable/clickable
       {:on-click-capture
        (fn [_]
          (protocols/-set! this :focused? true))}
       this))))

(core/deftype+ FocusController [child ^:mut child-rect ^:mut focused]
  protocols/IComponent
  (-measure [this ctx cs]
    (core/measure child ctx cs))
  
  (-draw [this ctx ^IRect rect ^Canvas canvas]
    (set! child-rect rect)
    (core/draw-child child ctx child-rect canvas))
  
  (-event [this ctx event]
    (core/event-child child ctx event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn on-click-capture [this]
  (protocols/-iterate this nil
    (fn [comp]
      (when (and (instance? Focusable comp) (:focused? comp))
        (protocols/-set! comp :focused? false)
        true))))

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
      (protocols/-set! focused :focused? false))
    (when-some [prev @*prev]
      (protocols/-set! prev :focused? true))))

(defn focus-next [this]
  (let [*first   (volatile! nil)
        *focused (volatile! nil)
        *next    (volatile! nil)]
    (protocols/-iterate this nil
      (fn [comp]
        (when (instance? Focusable comp)
          (when (nil? @*first)
            (do
              (vreset! *first comp)
              false))
          (if (:focused? comp)
            (do
              (vreset! *focused comp)
              false)
            (when @*focused
              (vreset! *next comp)
              true)))))
    (when-some [focused @*focused]
      (protocols/-set! focused :focused? false))
    (when-some [next (or @*next @*first)]
      (protocols/-set! next :focused? true))))

(defn focus-controller [child]
  (let [this (->FocusController child nil nil)]
    (clickable/clickable
      {:on-click-capture
       (fn [_]
         (on-click-capture this))}
      (key-listener/key-listener
        {:on-key-down
         (fn [e]
           (when (= :tab (:key e))
             (if (:shift (:modifiers e))
               (focus-prev this)
               (focus-next this))))}
        this))))
        