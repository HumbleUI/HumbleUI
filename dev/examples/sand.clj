(ns examples.sand
  (:require
    [clojure.math :as math]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.window :as window])
  (:import
    [io.github.humbleui.skija Color4f Paint]
    [java.util Arrays]))

(def cell-size
  8)

(def tick-ms
  17)

(def *state
  (ui/signal
    {:data        (short-array 0)
     :cols        0
     :rows        0
     :last-render 0
     :producing   nil
     :tool        :sand}))

(defn lch->rgb ^Color4f [c]
  (-> c
    (#'ui/paint-color)
    (#'ui/color->srgb :oklch)))

(def ^Paint paint
  (ui/paint {:fill "000"}))

(defn cell-idx [x y]
  (+ x (* y (:cols @*state))))

(defn cell-type [^short/1 data x y]
  (let [d (aget data (cell-idx x y))]
    (case (bit-and 0xF00 d)
      0x000 nil
      0x100 :sand
      0x200 :water
      0x300 :wall)))

(defn cell-color [^short/1 data x y]
  (let [d     (aget data (cell-idx x y))
        value (bit-and 0xFF d)
        lch   (case (cell-type data x y)
                nil    [0 0 0]
                :sand  [(-> value (/ 255) (* 0.4) (+ 0.5)) 0.1 90]
                :water [(-> 1 (- (/ y (:rows @*state))) (* 0.4) (+ 0.31)) 0.13 260]
                :wall  [(-> value (/ 255) (* 0.2) (+ 0.42)) 0.17 22])]
    (lch->rgb lch)))

(defn swap [^short/1 data x1 y1 x2 y2]
  (let [d1 (aget data (cell-idx x1 y1))]
    (aset data (cell-idx x1 y1) (aget data (cell-idx x2 y2)))
    (aset data (cell-idx x2 y2) d1)
    data))

(defn heavier? [t1 t2]
  (cond
    (and
      (= :wall t1)
      (not= :wall t2))
    true
    
    (and
      (= :sand t1)
      (contains? #{:water nil} t2))
    true
    
    (and
      (= :water t1)
      (= nil t2))
    true
    
    :else
    false))

(defn tick [[x y]]
  (let [{:keys [^short/1 data cols rows tool]} @*state
        data' (Arrays/copyOf data (alength data))]
    (when (and x y)
      (aset data' (cell-idx x y)
        (short
          (case tool
            :sand  (bit-or 0x100 (rand-int 256))
            :water 0x200
            :wall  (bit-or 0x300 (rand-int 256))))))
    (doseq [y (range 0 rows)
            x (range 0 cols)]
      (util/cond+
        :let [type (cell-type data x y)
              [dx1 dx2] (shuffle [1 -1])]
        ;; straight down
        :let [y' (+ y 1)]
        (and
          (< y (dec rows))
          (#{:sand :water} type)
          (heavier? type (cell-type data' x y')))
        (swap data' x y x y')
              
        ;; down and to the side
        :let [x' (+ x dx1)]
        (and
          (< y (dec rows))
          (#{:sand :water} type)
          (<= 0 x' (dec cols))
          (heavier? type (cell-type data' x' y')))
        (swap data' x y x' y')
        
        :let [x' (+ x dx2)]
        (and
          (< y (dec rows))
          (#{:sand :water} type)
          (<= 0 x' (dec cols))
          (heavier? type (cell-type data' x' y')))
        (swap data' x y x' y')
            
        ;; to the side
        :let [x' (+ x dx1)]
        (and
          (<= 0 x' (dec cols))
          (#{:water} type)
          (heavier? type (cell-type data' x' y)))
        (swap data' x y x' y)
          
        :let [x' (+ x dx2)]
        (and
          (<= 0 x' (dec cols))
          (#{:water} type)
          (heavier? type (cell-type data' x' y)))
        (swap data' x y x' y)))
    (swap! *state assoc :data data')))

(defn resize [state cols' rows']
  (let [{:keys [^short/1 data cols rows]} state
        data' (short-array (* cols' rows'))]
    (if (= 0 cols rows)
      ;; init
      (doseq [y (range 0 rows')
              x (range 0 cols')]
        (aset data' (+ x (* cols' y))
          (short
            (bit-or
              (rand-nth [0x200 0x100 0x100 0 0 0 0 0 0 0 0 0 0])
              (rand-int 256)))))
      ;; resize
      (doseq [y (range 0 (min rows rows'))
              x (range 0 (min cols cols'))]
        (aset data' (+ x (* cols' y)) (aget data (+ x (* cols y))))))
    (assoc state
      :cols cols'
      :rows rows'
      :data data')))

(defn on-paint [ctx canvas size]
  (let [dim   (math/round (* (:scale ctx) cell-size))
        cols' (-> size :width (quot dim))
        rows' (-> size :height (quot dim))]
    ;; resize
    (when (not= (juxt [:cols :rows] @*state) [cols' rows'])
      (swap! *state resize cols' rows'))
    
    ;; tick
    (let [dt (- (System/nanoTime) (:last-render @*state))]
      (when (> (/ dt 1000000) tick-ms)
        (tick (some->> @*state :producing (mapv #(quot % dim))))
        (swap! *state assoc :last-render (System/nanoTime))))
    
    ;; render
    (canvas/clear canvas 0xFF000000)
    (canvas/with-canvas canvas
      (canvas/scale canvas dim)
      (doseq [:let [{:keys [^short/1 data cols rows]} @*state]
              y (range 0 rows)
              x (range 0 cols)]
        (.setColor4f paint (cell-color data x y))
        (canvas/draw-rect canvas (util/rect-xywh x y 1 1) paint)))
  
    (window/request-frame (:window ctx))))

(defn on-event [ctx e]
  (cond
    (and
      (= :mouse-button (:event e))
      (:pressed? e))
    (swap! *state assoc :producing [(:x e) (:y e)])
    
    (and
      (= :mouse-move (:event e))
      (:producing @*state))
    (swap! *state assoc :producing [(:x e) (:y e)])
    
    (and
      (= :mouse-button (:event e))
      (not (:pressed? e)))
    (swap! *state assoc :producing nil)))

(ui/defcomp tools []
  (let [selected (:tool @*state)]
    [ui/padding {:padding 10}
     [ui/align {:x :left :y :top}
      [ui/row {:gap 10}
       (for [[tool color] [[:sand  [0.7 0.1 90]]
                           [:water [0.37 0.13 260]]
                           [:wall  [0.52 0.17 22]]]]
         [ui/clickable
          {:on-click (fn [_]
                       (swap! *state assoc :tool tool))}
          [ui/rect {:paint [{:fill color :model :oklch}
                            (when (= tool selected)
                              {:stroke "FFF" :width 4})]}
           [ui/gap {:size 32}]]])]]]))

(ui/defcomp ui []
  [ui/stack
   [ui/canvas
    {:on-paint on-paint
     :on-event on-event}]
   [tools]])
