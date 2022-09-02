(ns io.github.humbleui.ui.slider
  (:require
    [clojure.java.io :as io]
    [clojure.math :as math]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.window :as window]
    [io.github.humbleui.ui.clickable :as clickable])
  (:import
    [io.github.humbleui.types IPoint IRect RRect]
    [io.github.humbleui.skija Color Font]))

(defn- value-at [slider x]
  (let [{:keys [*state my-rect]}       slider
        {:keys [min max step delta-x]} @*state
        {left :x, width :width}        my-rect
        ratio  (core/clamp (/ (- x delta-x left) width) 0 1)
        range  (- max min)]
    (-> ratio
      (* (quot range step))
      (math/round)
      (* step)
      (+ min))))

(core/deftype+ Slider [*state
                       ^:mut my-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (assoc cs :height (:hui.slider/handle-size ctx)))
  
  (-draw [this ctx rect ^Canvas canvas]
    (set! my-rect rect)
    (let [state @*state
          {:keys [value min max step dragging?]} state
          {x :x, y :y, w :width, h :height} my-rect
          {:keys            [scale]
           :hui.slider/keys [shaft-height
                             fill-in
                             fill-out
                             fill-handle
                             stroke-handle
                             fill-handle-active
                             stroke-handle-active]} ctx
          range         (- max min)
          ratio         (/ (- value min) range)
          handle-x      (+ x (* ratio w))
          handle-y      (+ y (/ h 2))
          shaft-y       (- handle-y (/ shaft-height 2))]
      (canvas/draw-rect canvas
        (RRect/makeLTRB x shaft-y handle-x (+ shaft-y shaft-height) (/ shaft-height 2))
        fill-in)
      (canvas/draw-rect canvas
        (RRect/makeLTRB handle-x shaft-y (+ x w) (+ shaft-y shaft-height) (/ shaft-height 2))
        fill-out)
      (canvas/draw-circle canvas handle-x handle-y (/ h 2) (if dragging? fill-handle-active fill-handle))
      (canvas/draw-circle canvas handle-x handle-y (/ h 2) (if dragging? stroke-handle-active stroke-handle))))
  
  (-event [this ctx event]
    (core/eager-or
      (when (and
              (= :mouse-button (:event event))
              (= :primary (:button event))
              (:pressed? event))
        (let [{:keys [value min max step dragging?]} @*state
              {left :x, top :y, width :width, height :height} my-rect
              {:hui.slider/keys [handle-size]} ctx
              range         (- max min)
              ratio         (/ (- value min) range)
              handle-x      (+ left (* ratio width))
              handle-rect   (IRect/makeXYWH (- handle-x (/ height 2)) top height height)
              point         (IPoint. (:x event) (:y event))]
          (cond
            (.contains handle-rect point)
            (do
              (swap! *state assoc
                :dragging? true
                :delta-x   (- (:x event) handle-x))
              true)
            
            (.contains my-rect point)
            (do
              (swap! *state assoc
                :dragging? true
                :value     (value-at this (:x event)))
              true))))
    
      (when (and
              (:dragging? @*state)
              (= :mouse-button (:event event))
              (= :primary (:button event))
              (not (:pressed? event)))
        (swap! *state assoc
          :dragging? false
          :delta-x   0)
        true)
      
      (when (and
              (:dragging? @*state)
              (= :mouse-move (:event event)))
        (swap! *state assoc
          :value (value-at this (:x event)))
        true)))
  
  (-iterate [this ctx cb]
    (cb this)))

(defn slider
  ([*state]
   (slider nil *state))
  ([opts *state]
   (swap! *state
     #(core/merge-some
        {:value     (:min @*state 0)
         :min       0
         :max       100
         :step      1
         :dragging? false
         :delta-x   0}
        %))
   (->Slider *state nil)))

(comment
  (do
    (require 'examples.slider :reload)
    (reset! user/*example "slider")))