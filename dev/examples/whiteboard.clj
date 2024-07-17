(ns examples.whiteboard
  (:require
    [clojure.math :as math]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.util :as util])
  (:import
    [io.github.humbleui.skija Canvas PaintStrokeCap PaintStrokeJoin Path]))

(def *state
  (signal/signal
    {:x      0
     :y      0
     :zoom   1
     :drag-x nil
     :drag-y nil}))

(def *paths
  (signal/signal []))

(defn on-paint [state ctx canvas size]
  (let [zoom           (* (:zoom state) (:scale ctx))
        visible-width  (/ (:width size) zoom)
        visible-left   (- (:x state) (/ visible-width 2))
        visible-right  (+ (:x state) (/ visible-width 2))
        visible-height (/ (:height size) zoom)
        visible-top    (- (:y state) (/ visible-height 2))
        visible-bottom (+ (:y state) (/ visible-height 2))
        grid-step      20
        dot-radius     1]
    (canvas/with-canvas canvas
      (canvas/scale canvas zoom)
      (canvas/translate canvas (/ visible-width 2) (/ visible-height 2))
      (canvas/translate canvas (- (:x state)) (- (:y state)))
      (with-open [dot-fill    (paint/fill 0x80808080)
                  line-stroke (paint/stroke 0x80808080 1)
                  center-fill (paint/fill 0x80FF0000)
                  path-stroke (doto (paint/stroke 0xFFFF0080 5)
                                (.setStrokeJoin PaintStrokeJoin/ROUND)
                                (.setStrokeCap PaintStrokeCap/ROUND))]
        
        ;; dots
        (doseq [x (range (-> visible-left (- dot-radius) (quot grid-step) (* grid-step)) (+ visible-right dot-radius) grid-step)
                :when (not= x 0.0)
                y (range (-> visible-top (- dot-radius) (quot grid-step) (* grid-step)) (+ visible-bottom dot-radius) grid-step)
                :when (not= y 0.0)]
          (canvas/draw-circle canvas x y 1 dot-fill))
        
        ;; axes
        (when (<= visible-top 0 visible-bottom)
          (canvas/draw-line canvas visible-left 0 visible-right 0 line-stroke))
        (when (<= visible-left 0 visible-right)
          (canvas/draw-line canvas 0 visible-top 0 visible-bottom line-stroke))
        
        ;; center
        ; (canvas/draw-rect canvas (util/rect-xywh (- (:x state) 10) (- (:y state) 10) 20 20) center-fill)
        
        ;; paths
        (doseq [path @*paths]
          (.drawPath ^Canvas canvas path path-stroke))))))

(defn on-event [ctx event]
  (util/cond+
    :let [size  (:bounds ui/*node*)
          state @*state
          zoom  (* (:zoom state) (:scale ctx))
          x-abs (some-> (:x event)
                  (- (/ (:width size) 2))
                  (/ zoom)
                  (+ (:x state)))
          y-abs (some-> (:y event)
                  (- (/ (:height size) 2))
                  (/ zoom)
                  (+ (:y state)))]
    (and
      (= :mouse-button (:event event))
      (= :primary (:button event))
      (= true (:pressed? event)))
    (swap! *paths conj (doto (Path.) (.moveTo x-abs y-abs)))
    
    (and
      (= :mouse-move (:event event))
      (= #{:primary} (:buttons event)))
    (swap! *paths util/update-last #(.lineTo ^Path % x-abs y-abs))
    
    (and
      (= :mouse-button (:event event))
      (= :middle (:button event))
      (:pressed? event))
    (swap! *state assoc :drag-x (:x event) :drag-y (:y event))
    
    (and
      (= :mouse-button (:event event))
      (= :middle (:button event))
      (not (:pressed? event)))
    (swap! *state assoc :drag-x nil :drag-y nil)
    
    (and
      (= :mouse-move (:event event))
      :let [drag-x (:drag-x state)
            drag-y (:drag-y state)]
      drag-x
      drag-y)
    (swap! *state
      #(let [zoom (* (:zoom state) (:scale ctx))]
         (assoc %
           :x (-> drag-x (- (:x event)) (/  zoom) (+ (:x %)))
           :y (-> drag-y (- (:y event)) (/  zoom) (+ (:y %)))
           :drag-x (:x event)
           :drag-y (:y event))))
    
    (and
      (= :mouse-scroll (:event event))
      ((:modifiers event) (if (= :macos app/platform) :mac-command :ctrl)))
    (swap! *state update :zoom * (+ 1 (/ (:delta-y event) 1600)))
    
    (= :mouse-scroll (:event event))
    (swap! *state
      #(let [zoom (* (:zoom state) (:scale ctx))]
         (assoc %
           :x (-> (:delta-x event) - (/ zoom) (+ (:x %)))
           :y (-> (:delta-y event) - (/ zoom) (+ (:y %))))))))

(ui/defcomp button [on-click child]
  [ui/clickable
   {:on-click (fn [_] (on-click))}
   [ui/shadow {:dy 1 :blur 3 :color 0x40000000}
    [ui/rect {:paint  (paint/fill 0xFFFFFFFF)
              :radius 4}
     [ui/padding {:padding 10}
      child]]]])

(ui/defcomp ui []
  (let [state @*state
        paths @*paths]
    [ui/stack
     [ui/canvas
      {:on-paint (partial on-paint state)
       :on-event on-event}]
     [ui/padding {:padding 10}
      [ui/align {:x :left :y :top}
       [ui/row {:gap 5}
        [button #(swap! *state assoc :x 0 :y 0 :zoom 1)
         [ui/label "Zoom: " (-> (:zoom state) (* 100) (math/round)) "%"]]
        [button #(swap! *state update :zoom * 0.9) "−"]
        [button #(swap! *state update :zoom * 1.1) "+"]
        [button #(reset! *paths []) "Clear"]]]]]))
