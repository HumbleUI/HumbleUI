(ns examples.effects
  (:require
    [clojure.math :as math]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.ui.align :as align]
    [io.github.humbleui.ui.clickable :as clickable]
    [io.github.humbleui.ui.dynamic :as dynamic]
    [io.github.humbleui.ui.padding :as padding]
    [io.github.humbleui.ui.with-context :as with-context]
    [io.github.humbleui.window :as window])
  (:import
    [io.github.humbleui.skija Canvas]))

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
        (core/when-every [{:keys [x y]} event]
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

  (def ui
    (ui/center
      (ui/with-context
        {:hui.button/padding-left   40
         :hui.button/padding-top    20
         :hui.button/padding-right  40
         :hui.button/padding-bottom 20
         :hui.button/border-radius  10}
        (button (fn [] :nop) {}
          (ui/label "Hover ripple")))))
