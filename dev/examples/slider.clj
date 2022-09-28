(ns examples.slider
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.types IRect]))

(set! *warn-on-reflection* true)

(def *state0
  (atom {:value 500
         :max   1000}))

(def *state1
  (atom {:value 12
         :min   -66
         :max   66
         :step  3}))

(def *state2
  (atom {:value 2
         :min 0
         :max 10}))

(core/deftype+ SquareThumb []
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [{:keys [scale]} ctx]
      (core/size (* scale 32) (* scale 32))))

  (-draw [this ctx rect ^Canvas canvas]
    (let [{:hui.slider/keys [fill-thumb
                             stroke-thumb
                             fill-thumb-active
                             stroke-thumb-active]
           :hui/keys        [active?]} ctx]
      (canvas/draw-rect canvas rect (if active? fill-thumb-active fill-thumb))
      (canvas/draw-rect canvas rect (if active? stroke-thumb-active stroke-thumb))))
  
  (-event [this ctx event])

  (-iterate [this ctx cb]))

(core/deftype+ WideTrackLeft []
  protocols/IComponent
  (-measure [_ ctx cs]
    cs)

  (-draw [this ctx ^IRect rect ^Canvas canvas]
    (let [{:keys [scale]}   ctx
          track-height      (+ (:height rect) (* 2 scale))
          half-track-height (/ track-height 2)
          x      (- (:x rect) half-track-height)
          y      (+ (:y rect) (/ (:height rect) 2) (- half-track-height))
          w      (+ (:width rect) half-track-height)
          r      half-track-height
          rect   (core/rrect-xywh x y w track-height r 0 0 r)]
      (canvas/draw-rect canvas rect (:hui.slider/fill-track-active ctx))))
  
  (-event [this ctx event])

  (-iterate [this ctx cb]))

(core/deftype+ WideTrackRight []
  protocols/IComponent
  (-measure [_ ctx cs]
    cs)

  (-draw [this ctx ^IRect rect ^Canvas canvas]
    (let [{:keys [scale]}   ctx
          track-height      (+ (:height rect) (* 2 scale))
          half-track-height (/ track-height 2)
          x      (:x rect)
          y      (+ (:y rect) (/ (:height rect) 2) (- half-track-height))
          w      (+ (:width rect) half-track-height)
          r      half-track-height
          rect   (core/rrect-xywh x y w track-height 0 r r 0)]
      (canvas/draw-rect canvas rect (:hui.slider/fill-track-inactive ctx))))
  
  (-event [this ctx event])

  (-iterate [this ctx cb]))

(def ui
  (ui/padding 20 20
    (ui/valign 0.5
      (ui/column
        (ui/row
          [:stretch 1
           (ui/rect (paint/fill 0xFFFFFFFF)
             (ui/slider *state0))]
          (ui/valign 0.5
            (ui/width 100
              (ui/halign 1
                (ui/dynamic _ [value (:value @*state0)]
                  (ui/label (str value " / 1000")))))))
        
        (ui/gap 0 10)
        
        (ui/row
          [:stretch 1
           (ui/rect (paint/fill 0xFFFFFFFF)
             (ui/slider {:thumb (->SquareThumb)} *state0))]
          (ui/valign 0.5
            (ui/width 100
              (ui/halign 1
                (ui/dynamic _ [value (:value @*state0)]
                  (ui/label (str value " / 1000")))))))
        
        (ui/gap 0 10)
        
        (ui/row
          [:stretch 1
           (ui/rect (paint/fill 0xFFFFFFFF)
             (ui/slider {:track-active (->WideTrackLeft)} *state0))]
          (ui/valign 0.5
            (ui/width 100
              (ui/halign 1
                (ui/dynamic _ [value (:value @*state0)]
                  (ui/label (str value " / 1000")))))))
        
        (ui/gap 0 10)
        
        (ui/row
          [:stretch 1
           (ui/rect (paint/fill 0xFFFFFFFF)
             (ui/slider {:track-active (->WideTrackLeft)
                         :track-inactive (->WideTrackRight)} *state0))]
          (ui/valign 0.5
            (ui/width 100
              (ui/halign 1
                (ui/dynamic _ [value (:value @*state0)]
                  (ui/label (str value " / 1000")))))))
        
        (ui/gap 0 10)
        
        (ui/row
          [:stretch 1 (ui/slider *state1)]
          (ui/valign 0.5
            (ui/width 100
              (ui/halign 1
                (ui/dynamic _ [value (:value @*state1)]
                  (ui/label (str value " / 66")))))))
        
        (ui/gap 0 10)
        
        (ui/row
          [:stretch 1 (ui/slider *state2)]
          (ui/valign 0.5
            (ui/width 100
              (ui/dynamic _ [value (:value @*state2)]
                (ui/halign 1
                  (ui/label (str value " / 10")))))))))))

; (reset! user/*example "slider")

(comment
  (reset! user/*example "slider"))