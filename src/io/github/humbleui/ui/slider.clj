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
    [io.github.humbleui.skija Color Font]))

(defn- value-at [slider x]
  (let [{:keys [*state thumb-size my-rect]} slider
        {:keys [min max step delta-x]}      @*state
        {thumb-w :width, thumb-h :heigth}   thumb-size
        half-thumb-w (/ thumb-w 2)
        left   (+ (:x my-rect) half-thumb-w)
        width  (- (:width my-rect) thumb-w)
        ratio  (core/clamp (/ (- x delta-x left) width) 0 1)
        range  (- max min)]
    (-> ratio
      (* (quot range step))
      (math/round)
      (* step)
      (+ min))))

(core/deftype+ SliderThumb []
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [{:hui.slider/keys [thumb-size]} ctx]
      (core/size thumb-size thumb-size)))

  (-draw [this ctx rect ^Canvas canvas]
    (let [{:hui.slider/keys [fill-thumb
                             stroke-thumb
                             fill-thumb-active
                             stroke-thumb-active]
           :hui/keys        [active?]} ctx
          x (+ (:x rect) (/ (:width rect) 2))
          y (+ (:y rect) (/ (:height rect) 2))
          r (/ (:height rect) 2)]
      (canvas/draw-circle canvas x y r (if active? fill-thumb-active fill-thumb))
      (canvas/draw-circle canvas x y r (if active? stroke-thumb-active stroke-thumb))))
  
  (-event [this ctx event])

  (-iterate [this ctx cb]))

(core/deftype+ SliderTrack [fill-key]
  protocols/IComponent
  (-measure [_ ctx cs]
    cs)

  (-draw [this ctx ^IRect rect ^Canvas canvas]
    (let [{:hui.slider/keys [track-height]} ctx
          half-track-height (/ track-height 2)
          x      (- (:x rect) half-track-height)
          y      (+ (:y rect) (/ (:height rect) 2) (- half-track-height))
          w      (+ (:width rect) track-height)
          r      half-track-height
          rect   (core/rrect-xywh x y w track-height r)]
      (canvas/draw-rect canvas rect (ctx fill-key))))
  
  (-event [this ctx event])

  (-iterate [this ctx cb]))

(core/deftype+ Slider [*state
                       track-active
                       track-inactive
                       thumb
                       ^:mut thumb-size
                       ^:mut my-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (assoc cs :height
      (:height (core/measure thumb ctx cs))))
  
  (-draw [this ctx rect ^Canvas canvas]
    (set! my-rect rect)
    (set! thumb-size (core/measure thumb ctx (core/size (:width rect) (:height rect))))
    (let [state @*state
          {:keys [value min max step dragging?]} state
          {x :x, y :y, w :width, h :height} my-rect
          {thumb-w :width, thumb-h :height} thumb-size
          half-thumb-w (/ thumb-w 2)
          {:hui.slider/keys [fill-left
                             fill-right]} ctx
          range   (- max min)
          ratio   (/ (- value min) range)
          thumb-x (+ x half-thumb-w (* ratio (- w thumb-w)))
          thumb-y (+ y (/ h 2))
          ctx'    (cond-> ctx
                    dragging? (assoc :hui/active? true))]
      (core/draw track-active   ctx' (core/irect-ltrb (+ x half-thumb-w)       y thumb-x                  (+ y thumb-h)) canvas)
      (core/draw track-inactive ctx' (core/irect-ltrb thumb-x                  y (+ x w (- half-thumb-w)) (+ y thumb-h)) canvas)
      (core/draw thumb          ctx' (core/irect-xywh (- thumb-x half-thumb-w) y thumb-w thumb-h) canvas)))
  
  (-event [this ctx event]
    (core/eager-or
      (when (and
              (= :mouse-button (:event event))
              (= :primary (:button event))
              (:pressed? event))
        (let [{:keys [value min max step dragging?]} @*state
              {left :x, top :y, width :width, height :height} my-rect
              {thumb-w :width, thumb-h :height} thumb-size
              half-thumb-w (/ thumb-w 2)
              range        (- max min)
              ratio        (/ (- value min) range)
              thumb-x      (+ left half-thumb-w (* ratio (- width thumb-w)))
              thumb-rect   (core/irect-xywh (- thumb-x half-thumb-w) top thumb-w thumb-h)
              point        (core/ipoint (:x event) (:y event))]
          (cond
            (core/rect-contains? thumb-rect point)
            (do
              (swap! *state assoc
                :dragging? true
                :delta-x   (- (:x event) thumb-x))
              true)
            
            (core/rect-contains? my-rect point)
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
   (let [{:keys [track-active
                 track-inactive
                 thumb]
          :or {track-active   (->SliderTrack :hui.slider/fill-track-active)
               track-inactive (->SliderTrack :hui.slider/fill-track-inactive)
               thumb          (->SliderThumb)}} opts]
     (swap! *state
       #(core/merge-some
          {:value     (:min @*state 0)
           :min       0
           :max       100
           :step      1
           :dragging? false
           :delta-x   0}
          %))
     (->Slider *state track-active track-inactive thumb nil nil))))

(comment
  (do
    (require 'examples.slider :reload)
    (reset! user/*example "slider")))