(ns examples.effects
  (:require
    [clojure.math :as math]
    [examples.util :as util]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.typeface :as typeface]
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.window :as window])
  (:import
    [io.github.humbleui.skija BlendMode Canvas Paint Shader]))

@ui/button ;; FIXME

(core/deftype+ Ripple [^:mut center
                       ^:mut progress-start
                       ^:mut hovered?]
  :extends ui/AWrapperNode
  
  protocols/IComponent  
  (-draw-impl [_ ctx rect ^Canvas canvas]
    (let [{:keys [scale window]} ctx
          [_ opts _] (ui/parse-element element)
          {:keys [duration radius state]
           :or {duration 128
                radius 1.5}} opts
          pressed? (:pressed state)
          border-radius 10
          rrect    (core/rrect-xywh (:x rect) (:y rect) (:width rect) (:height rect) (* scale border-radius))
          max-r    (* radius (Math/hypot (:width rect) (:height rect)))
          progress (-> (core/now) (- (or progress-start 0)) (/ duration) (min 1))
          progress (if hovered? progress (- 1 progress))]
      (canvas/with-canvas canvas
        (.clipRRect canvas rrect true)
        
        (when (< progress 1)
          (.drawPaint canvas ui/button-bg))
        
        (when (> progress 0)
          (canvas/draw-circle canvas (:x center) (:y center) (* max-r progress) (if pressed? ui/button-bg-pressed ui/button-bg-hovered)))
        
        (ui/draw-child child ctx rect canvas)
        
        (when (< 0 progress 1)
          (window/request-frame window)))))
  
  (-event-impl [this ctx event]
    (core/eager-or
      (when (= :mouse-move (:event event))
        (core/when-some+ [{:keys [x y]} event]
          (let [p         (core/ipoint x y)
                hovered?' (core/rect-contains? rect p)]
            (when (not= hovered? hovered?')
              (set! hovered? hovered?')
              (set! progress-start (core/now))
              (let [[_ opts _] (ui/parse-element element)
                    radius (:radius opts 1.5)
                    cx     (+ (:x rect) (/ (:width rect) 2))
                    cy     (+ (:y rect) (/ (:height rect) 2))
                    theta  (math/atan2 (- y cy) (- x cx))
                    max-r  (* (- radius 0.5) (Math/hypot (:width rect) (:height rect)))
                    px     (-> (math/cos theta) (* max-r) (+ cx))
                    py     (-> (math/sin theta) (* max-r) (+ cy))]
                (set! center (core/point px py))
                true)))))
      (ui/event-child child (protocols/-context this ctx) event))))

(defn ripple [opts child]
  (map->Ripple {}))

(defn button [opts child]
  [ui/clickable {}
   (fn [state]
     [ripple (assoc opts :state state)
      [ui/padding {:horizontal 40
                   :vertical 20}
       [ui/center
        child]]])])

;; card

(def default-gradient 
  (Shader/makeLinearGradient
    (float 0) (float 0) (float 1) (float 1)
    (int-array [(unchecked-int 0xFF306060)
                (unchecked-int 0xFF603060)])))

(core/deftype+ Card [bg
                     gradient
                     ^:mut hovered?]
  :extends ui/AWrapperNode
  protocols/IComponent  
  (-draw-impl [_ ctx rect ^Canvas canvas]
    (let [{:keys [mouse-pos scale]} ctx
          [_ opts _] (ui/parse-element element)
          {:keys [bg gradient]
           :or {bg       (paint/fill 0xFF202020)
                gradient default-gradient}} opts]
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
      (ui/draw-child child ctx rect canvas)))
  
  (-event-impl [this ctx event]
    (core/eager-or
      (core/when-some+ [{:keys [x y]} event]
        (when (= :mouse-move (:event event))
          (let [hovered?' (core/rect-contains? rect (core/ipoint x y))]
            (core/eager-or
              (when (not= hovered? hovered?')
                (set! hovered? hovered?')
                true)
              (when hovered?'
                true)))))
      (ui/event-child child ctx event))))

(defn card [opts child]
  (map->Card {}))

(defn ui []
  (let [scale (:scale ui/*ctx*)
        font  (font/make-with-cap-height @util/*face-bold (* scale 10))]
    (fn []
      [ui/align {:y :center}
       [ui/vscrollbar
        [ui/align {:x :center}
         [ui/padding {:padding 20}
          [ui/column {:gap 10}
           [button {}
            [ui/label "Hover ripple"]]

           [ui/clip-rrect {:radii [10]}
            [card {}
             [ui/with-context {:fill-text (paint/fill 0xFFFFFFFF)}
              [ui/padding {:horizontal 30
                           :vertical 20}
               [ui/column
                [ui/label {:font font} "Card title"]
                [ui/gap {:height 20}]
                [ui/label "Just some text here"]
                [ui/gap {:height 10}]
                [ui/label "to let you see and appreciate"]
                [ui/gap {:height 10}]
                [ui/label "how highlight gradient"]
                [ui/gap {:height 10}]
                [ui/label "works on this card."]]]]]]]]]]])))
