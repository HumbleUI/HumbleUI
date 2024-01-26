(ns examples.effects
  (:require
    [clojure.math :as math]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.typeface :as typeface]
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.ui.align :as align]
    [io.github.humbleui.ui.clickable :as clickable]
    [io.github.humbleui.ui.dynamic :as dynamic]
    [io.github.humbleui.ui.padding :as padding]
    [io.github.humbleui.ui.with-context :as with-context]
    [io.github.humbleui.window :as window])
  (:import
    [io.github.humbleui.skija BlendMode Canvas Paint Shader]))

(core/deftype+ Ripple [^:mut center
                       ^:mut progress-start
                       ^:mut hovered?
                       duration
                       radius]
  :extends core/AWrapper
  
  protocols/IComponent  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (let [{:keys            [scale window]
           :hui/keys        [active?]
           :hui.button/keys [bg bg-hovered bg-active border-radius]} ctx
          rrect    (core/rrect-xywh (:x rect) (:y rect) (:width rect) (:height rect) (* scale border-radius))
          max-r    (* radius (Math/hypot (:width rect) (:height rect)))
          progress (-> (core/now) (- progress-start) (/ duration) (min 1))
          progress (if hovered? progress (- 1 progress))]
      (canvas/with-canvas canvas
        (.clipRRect canvas rrect true)
        
        (when (< progress 1)
          (.drawPaint canvas bg))
        
        (when (> progress 0)
          (canvas/draw-circle canvas (:x center) (:y center) (* max-r progress) (if active? bg-active bg-hovered)))
        
        (core/draw child ctx rect canvas)
        
        (when (< 0 progress 1)
          (window/request-frame window)))))
  
  (-event [this ctx event]
    (core/eager-or
      (when (= :mouse-move (:event event))
        (core/when-some+ [{:keys [x y]} event]
          (let [p         (core/ipoint x y)
                hovered?' (core/rect-contains? child-rect p)]
            (when (not= hovered? hovered?')
              (set! hovered? hovered?')
              (set! progress-start (core/now))
              (let [rect  child-rect
                    cx    (+ (:x rect) (/ (:width rect) 2))
                    cy    (+ (:y rect) (/ (:height rect) 2))
                    theta (math/atan2 (- y cy) (- x cx))
                    max-r (* (- radius 0.5) (Math/hypot (:width rect) (:height rect)))
                    px    (-> (math/cos theta) (* max-r) (+ cx))
                    py    (-> (math/sin theta) (* max-r) (+ cy))]
                (set! center (core/point px py))
                true)))))
      (core/event-child child (protocols/-context this ctx) event))))

(defn ripple [opts child]
  (map->Ripple
    (merge
      {:progress-start 0
       :duration       200
       :radius         1.5}
      opts
      {:child child})))

(defn button [on-click opts child]
  (clickable/clickable
    {:on-click (when on-click
                 (fn [_] (on-click)))}
    (ripple opts
      (dynamic/dynamic ctx [{:hui.button/keys [padding-left padding-top padding-right padding-bottom]} ctx]
        (padding/padding padding-left padding-top padding-right padding-bottom
          (align/center
            (with-context/with-context
              {:hui/active?  false
               :hui/hovered? false}
              child)))))))

(core/deftype+ Card [bg gradient ^:mut hovered?]
  :extends core/AWrapper
  
  protocols/IComponent  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (let [{:keys [mouse-pos scale]} ctx]
      (canvas/draw-rect canvas rect bg)
      (when hovered?
        (canvas/with-canvas canvas
          (canvas/clip-rect canvas rect)
          (canvas/translate canvas (:x rect) (:y rect))
          (let [scale (max (:width rect) (:height rect))
                w     (/ (:width rect) scale)
                h     (/ (:height rect) scale)]
            (canvas/scale canvas scale scale)
            (with-open [mask   (Shader/makeRadialGradient
                                 (-> (:x mouse-pos) (- (:x rect)) (/ scale) float)
                                 (-> (:y mouse-pos) (- (:y rect)) (/ scale) float)
                                 (float 1)
                                 (int-array [(unchecked-int 0xFFFFFFFF)
                                             (unchecked-int 0x00FFFFFF)]))
                        shader (Shader/makeBlend BlendMode/SRC_IN mask gradient)
                        paint  (Paint.)]
              (.setShader paint shader)
              (canvas/draw-rect canvas (core/rect-xywh 0 0 w h) paint)))))
      (core/draw child ctx rect canvas)))
  
  (-event [this ctx event]
    (core/eager-or
      (core/when-some+ [{:keys [x y]} event]
        (when (= :mouse-move (:event event))
          (let [hovered?' (core/rect-contains? child-rect (core/ipoint x y))]
            (core/eager-or
              (when (not= hovered? hovered?')
                (set! hovered? hovered?')
                true)
              (when hovered?'
                true)))))
      (core/event-child child ctx event))))

(def gradient 
  (Shader/makeLinearGradient
    (float 0) (float 0) (float 1) (float 1)
    (int-array [(unchecked-int 0xFF306060)
                (unchecked-int 0xFF603060)])))

(defn card [opts child]
  (map->Card
    (merge
      {:bg       (paint/fill 0xFF202020)
       :gradient gradient}
      opts
      {:hovered? false
       :child    child})))

(def face-bold
  (typeface/make-from-resource "io/github/humbleui/fonts/Inter-Bold.ttf"))

(def ui
  (ui/center
    (ui/column
      (ui/with-context
        {:hui.button/padding-left   40
         :hui.button/padding-top    20
         :hui.button/padding-right  40
         :hui.button/padding-bottom 20
         :hui.button/border-radius  10}
        (button (fn [] :nop) {}
          (ui/label "Hover ripple")))
      (ui/gap 0 10)
      (ui/clip-rrect 10
        (card {}
          (ui/with-context
            {:fill-text (paint/fill 0xFFFFFFFF)}
            (ui/padding 20 30
              (ui/column
                (ui/with-scale scale
                  (ui/with-context
                    {:font-ui (font/make-with-cap-height face-bold (* scale 10))}
                    (ui/label "Card title")))
                (ui/gap 0 20)
                (ui/label "Just some text here")
                (ui/gap 0 10)
                (ui/label "to let you see and appreciate")
                (ui/gap 0 10)
                (ui/label "how highlight gradient")
                (ui/gap 0 10)
                (ui/label "works on this card.")))))))))
