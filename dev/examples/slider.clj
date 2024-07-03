(ns examples.slider
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.types IRect]))

(core/deftype+ SquareThumb []
  :extends ui/ATerminalNode
  protocols/IComponent
  (-measure-impl [_ ctx _cs]
    (let [{:keys [scale]} ctx]
      (core/isize (* scale 32) (* scale 32))))

  (-draw-impl [_ ctx rect canvas]
    (let [{:hui.slider/keys [fill-thumb
                             stroke-thumb
                             fill-thumb-active
                             stroke-thumb-active]
           :hui/keys        [active?]} ctx]
      (canvas/draw-rect canvas rect (if active? fill-thumb-active fill-thumb))
      (canvas/draw-rect canvas rect (if active? stroke-thumb-active stroke-thumb)))))

(defn square-thumb []
  (map->SquareThumb {}))

(defn line-thumb []
  (let [paint (paint/fill 0xFF0080FF)]
    {:measure
     (fn [_ cs]
       (let [{:keys [scale]} ui/*ctx*]
         (core/isize (* scale 4) (* scale 32))))
     :draw
     (fn [_ ^IRect rect canvas]
       (let [{:keys [scale]} ui/*ctx*
             rrect (-> rect .toRect (.withRadii (* scale 2.0)))]
         (canvas/draw-rrect canvas rrect paint)))}))

(core/deftype+ WideTrackLeft []
  :extends ui/ATerminalNode
  protocols/IComponent
  (-measure-impl [_ _ctx cs]
    cs)

  (-draw-impl [_ ctx rect canvas]
    (let [{:keys [scale]}   ctx
          track-height      (+ (:height rect) (* 2 scale))
          half-track-height (/ track-height 2)
          x      (- (:x rect) half-track-height)
          y      (+ (:y rect) (/ (:height rect) 2) (- half-track-height))
          w      (+ (:width rect) half-track-height)
          r      half-track-height
          rect   (core/rrect-xywh x y w track-height r 0 0 r)]
      (canvas/draw-rect canvas rect (:hui.slider/fill-track-active ctx)))))

(core/deftype+ WideTrackRight []
  :extends ui/ATerminalNode
  protocols/IComponent
  (-measure-impl [_ _ctx cs]
    cs)

  (-draw-impl [_ ctx rect canvas]
    (let [{:keys [scale]}   ctx
          track-height      (+ (:height rect) (* 2 scale))
          half-track-height (/ track-height 2)
          x      (:x rect)
          y      (+ (:y rect) (/ (:height rect) 2) (- half-track-height))
          w      (+ (:width rect) half-track-height)
          r      half-track-height
          rect   (core/rrect-xywh x y w track-height 0 r r 0)]
      (canvas/draw-rect canvas rect (:hui.slider/fill-track-inactive ctx)))))

(ui/defcomp with-slider [slider]
  (let [{:keys [max *value]} (second slider)]
    [ui/row
     ^{:stretch 1}
     [ui/rect {:paint (paint/fill 0xFFFFFFFF)}
      slider]
     [ui/valign {:position 0.5}
      [ui/size {:width 100}
       [ui/halign {:position 1}
        [ui/label *value " / " max]]]]]))

(ui/defcomp ui []
  [ui/padding {:padding 20}
   [ui/valign {:position 0.5}
    (let [*state0 (signal/signal 500)]
      [ui/column {:gap 10}
       [with-slider 
        [ui/slider
         {:*value *state0
          :max    1000}]]
     
       [with-slider 
        [ui/slider
         {:*value *state0
          :max    1000
          :thumb (map->SquareThumb {})}]]
       
       [with-slider 
        [ui/slider
         {:*value *state0
          :max    1000
          :thumb  [square-thumb]}]]
       
       [with-slider 
        [ui/slider
         {:*value *state0
          :max    1000
          :thumb  [line-thumb]}]]
     
       [with-slider 
        [ui/slider
         {:*value       *state0
          :max          1000
          :track-active (map->WideTrackLeft {})}]]
     
       [with-slider 
        [ui/slider
         {:*value         *state0
          :max            1000
          :track-active   (map->WideTrackLeft {})
          :track-inactive (map->WideTrackRight {})}]]
     
       (let [*state1 (signal/signal 12)]
         [with-slider 
          [ui/slider
           {:*value *state1
            :min    -66
            :max    66
            :step   3}]])

       (let [*state2 (signal/signal 2)]
         [with-slider 
          [ui/slider
           {:*value *state2
            :max    10}]])])]])
