(ns examples.effects
  (:require
    [clojure.math :as math]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.typeface :as typeface]
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.window :as window])
  (:import
    [io.github.humbleui.skija BlendMode Canvas Paint Shader]))

@ui/button ;; FIXME

(util/deftype+ Ripple [center
                       progress-start
                       hovered?]
  :extends ui/AWrapperNode
  
  protocols/IComponent  
  (-draw-impl [_ ctx bounds container-size viewport ^Canvas canvas]
    (let [{:keys [scale window]} ctx
          [_ opts _] (ui/parse-element element)
          {:keys [duration radius state]
           :or {duration 128
                radius 1.5}} opts
          pressed? (:pressed state)
          border-radius 10
          rrect    (util/rrect-xywh (:x bounds) (:y bounds) (:width bounds) (:height bounds) (* scale border-radius))
          max-r    (* radius (Math/hypot (:width bounds) (:height bounds)))
          progress (-> (util/now) (- (or progress-start 0)) (/ duration) (min 1))
          progress (if hovered? progress (- 1 progress))]
      (canvas/with-canvas canvas
        (.clipRRect canvas rrect true)
        
        (when (< progress 1)
          (ui/with-paint ctx [paint (-> ui/button-styles :default :body)]
            (.drawPaint canvas paint)))
        
        (when (> progress 0)
          (ui/with-paint ctx [paint (if pressed?
                                      (-> ui/button-styles :default :body-pressed)
                                      (-> ui/button-styles :default :body-hovered))]
            (canvas/draw-circle canvas (:x center) (:y center) (* max-r progress) paint)))
        
        (let [ctx' (assoc ctx :paint (-> ui/button-styles :default :text))]
          (ui/draw child ctx' bounds container-size viewport canvas))
        
        (when (< 0 progress 1)
          (window/request-frame window)))))
  
  (-event-impl [this ctx event]
    (util/eager-or
      (when (= :mouse-move (:event event))
        (util/when-some+ [{:keys [x y]} event]
          (let [p         (util/ipoint x y)
                hovered?' (util/rect-contains? bounds p)]
            (when (not= hovered? hovered?')
              (set! hovered? hovered?')
              (set! progress-start (util/now))
              (let [[_ opts _] (ui/parse-element element)
                    radius (:radius opts 1.5)
                    cx     (+ (:x bounds) (/ (:width bounds) 2))
                    cy     (+ (:y bounds) (/ (:height bounds) 2))
                    theta  (math/atan2 (- y cy) (- x cx))
                    max-r  (* (- radius 0.5) (Math/hypot (:width bounds) (:height bounds)))
                    px     (-> (math/cos theta) (* max-r) (+ cx))
                    py     (-> (math/sin theta) (* max-r) (+ cy))]
                (set! center (util/point px py))
                true)))))
      (ui/event child ctx event))))

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

(util/deftype+ Card [bg
                     gradient
                     hovered?]
  :extends ui/AWrapperNode
  protocols/IComponent  
  (-draw-impl [_ ctx bounds container-size viewport ^Canvas canvas]
    (let [{:keys [mouse-pos scale]} ctx
          [_ opts _] (ui/parse-element element)
          {:keys [bg gradient]
           :or {bg       {:fill 0xFF202020}
                gradient default-gradient}} opts]
      (ui/with-paint ctx [paint bg]
        (canvas/draw-rect canvas bounds paint))
      (when hovered?
        (canvas/with-canvas canvas
          (canvas/clip-rect canvas bounds)
          (canvas/translate canvas (:x bounds) (:y bounds))
          (let [scale (max (:width bounds) (:height bounds))
                w     (/ (:width bounds) scale)
                h     (/ (:height bounds) scale)]
            (canvas/scale canvas scale scale)
            (with-open [mask   (Shader/makeRadialGradient
                                 (-> (:x mouse-pos) (- (:x bounds)) (/ scale) float)
                                 (-> (:y mouse-pos) (- (:y bounds)) (/ scale) float)
                                 (float 1)
                                 (int-array [(unchecked-int 0xFFFFFFFF)
                                             (unchecked-int 0x00FFFFFF)]))
                        shader (Shader/makeBlend BlendMode/SRC_IN mask gradient)
                        paint  (Paint.)]
              (.setShader paint shader)
              (canvas/draw-rect canvas (util/rect-xywh 0 0 w h) paint)))))
      (ui/draw child ctx bounds container-size viewport canvas)))
  
  (-event-impl [this ctx event]
    (util/eager-or
      (util/when-some+ [{:keys [x y]} event]
        (when (= :mouse-move (:event event))
          (let [hovered?' (util/rect-contains? bounds (util/ipoint x y))]
            (util/eager-or
              (when (not= hovered? hovered?')
                (set! hovered? hovered?')
                true)
              (when hovered?'
                true)))))
      (ui/event child ctx event))))

(defn card [opts child]
  (map->Card {}))

(defn ui []
  [ui/align {:y :center}
   [ui/vscroll
    [ui/align {:x :center}
     [ui/padding {:padding 20}
      [ui/column {:gap 10}
       [button {}
        [ui/label "Hover ripple"]]

       [ui/clip {:radius 10}
        [card {}
         [ui/with-context {:paint {:fill 0xFFFFFFFF}}
          [ui/padding {:horizontal 30
                       :vertical 20}
           [ui/column
            [ui/label {:font-weight :bold} "Card title"]
            [ui/gap {:height 20}]
            [ui/label "Just some text here"]
            [ui/gap {:height 10}]
            [ui/label "to let you see and appreciate"]
            [ui/gap {:height 10}]
            [ui/label "how highlight gradient"]
            [ui/gap {:height 10}]
            [ui/label "works on this card."]]]]]]]]]]])
