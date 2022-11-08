(ns io.github.humbleui.ui.slider
  (:require
    [clojure.math :as math]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]))

(defn- value-at [slider x]
  (let [{:keys [*state thumb-size my-rect]} slider
        {:keys [min max step delta-x]}      @*state
        {thumb-w :width} thumb-size
        half-thumb-w     (/ thumb-w 2)
        left             (+ (:x my-rect) half-thumb-w)
        width            (- (:width my-rect) thumb-w)
        ratio            (core/clamp (/ (- x delta-x left) width) 0 1)
        range            (- max min)]
    (-> ratio
      (* (quot range step))
      (math/round)
      (* step)
      (+ min))))

(core/deftype+ SliderThumb []
  protocols/IComponent
  (-measure [_ ctx _cs]
    (let [{:hui.slider/keys [thumb-size]} ctx]
      (core/isize thumb-size thumb-size)))

  (-draw [_ ctx rect canvas]
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
  
  (-event [_ _ctx _event])

  (-iterate [_ _ctx _cb]))

(core/deftype+ SliderTrack [fill-key]
  protocols/IComponent
  (-measure [_ _ctx cs]
    cs)

  (-draw [_ ctx rect canvas]
    (let [{:hui.slider/keys [track-height]} ctx
          half-track-height (/ track-height 2)
          x      (- (:x rect) half-track-height)
          y      (+ (:y rect) (/ (:height rect) 2) (- half-track-height))
          w      (+ (:width rect) track-height)
          r      half-track-height
          rect   (core/rrect-xywh x y w track-height r)]
      (canvas/draw-rect canvas rect (ctx fill-key))))
  
  (-event [_ _ctx _event])

  (-iterate [_ _ctx _cb]))

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
  
  (-draw [_ ctx rect canvas]
    (set! my-rect rect)
    (set! thumb-size (core/measure thumb ctx (core/isize (:width rect) (:height rect))))
    (let [state @*state
          {:keys [value min max dragging?]} state
          {x :x, y :y, w :width} my-rect
          {thumb-w :width, thumb-h :height} thumb-size
          half-thumb-w (/ thumb-w 2)
          range   (- max min)
          ratio   (/ (- value min) range)
          thumb-x (+ x half-thumb-w (* ratio (- w thumb-w)))
          ctx'    (cond-> ctx
                    dragging? (assoc :hui/active? true))]
      (core/draw track-active   ctx' (core/irect-ltrb (+ x half-thumb-w)       y thumb-x                  (+ y thumb-h)) canvas)
      (core/draw track-inactive ctx' (core/irect-ltrb thumb-x                  y (+ x w (- half-thumb-w)) (+ y thumb-h)) canvas)
      (core/draw thumb          ctx' (core/irect-xywh (- thumb-x half-thumb-w) y thumb-w thumb-h) canvas)))
  
  (-event [this _ctx event]
    (core/eager-or
      (when (and
              (= :mouse-button (:event event))
              (= :primary (:button event))
              (:pressed? event))
        (let [{:keys [value min max]} @*state
              {left :x, top :y, width :width} my-rect
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
  
  (-iterate [this _ctx cb]
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
