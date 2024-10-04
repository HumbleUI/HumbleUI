(ns examples.mirrors
  (:require
   [clojure.math :as math]
   [io.github.humbleui.canvas :as canvas]
   [io.github.humbleui.ui :as ui]
   [io.github.humbleui.util :as util]
   [io.github.humbleui.window :as window])
  (:import
   [io.github.humbleui.skija Canvas Color4f ImageFilter Paint Path PathEffect]
   [java.util Arrays]))

(def *state
  (ui/signal
    {:mouse   {:x 0 :y 0}
     :shape   (vec
                (for [a (range 0 (* 2 math/PI) 0.05)]
                  {:x (+ 100 (* 50 (math/sin a)))
                   :y (+ 250 (* 50 (math/cos a)))}))
     :handles [{:x 150 :y 50}
               {:x 200 :y 150}
               {:x 200 :y 400}
               {:x 150 :y 500}]}))

(defn dist [{x1 :x y1 :y} {x2 :x y2 :y}]
  (math/sqrt
    (+
      (* (- x2 x1) (- x2 x1))
      (* (- y2 y1) (- y2 y1)))))

(defn reflect [{:keys [x y] :as p} path]
  (let [closest (apply min-key #(dist p %) path)
        dx      (- (:x closest) x)
        dy      (- (:y closest) y)]
    {:x (+ (:x closest) dx)
     :y (+ (:y closest) dy)}))

(defn path-points [a b c d]
  (for [t (range 0 1 0.002)]
    {:x (+
          (* (- 1 t) (- 1 t) (- 1 t) (:x a))
          (* 3 (- 1 t) (- 1 t) t (:x b))
          (* 3 (- 1 t) t t (:x c))
          (* t t t (:x d)))
     :y (+
          (* (- 1 t) (- 1 t) (- 1 t) (:y a))
          (* 3 (- 1 t) (- 1 t) t (:y b))
          (* 3 (- 1 t) t t (:y c))
          (* t t t (:y d)))}))

(defn draw-path [^Canvas canvas points paint]
  (with-open [path (Path.)]
    (.moveTo path (-> points first :x) (-> points first :y))
    (doseq [{:keys [x y]} (next points)]
      (.lineTo path x y))
    (.closePath path)
    (.drawPath canvas path paint)))

(defn on-paint [ctx ^Canvas canvas size]
  (let [scale     (:scale ctx)
        [a b c d] (:handles @*state)
        mirror    (path-points a b c d)]
    (canvas/clear canvas 0xFF000000)
    (canvas/with-canvas canvas
      (canvas/scale canvas scale)
      
      (doseq [shape [(:shape @*state)
                     (let [{:keys [x y]} (:mouse @*state)
                           x (/ x scale)
                           y (/ y scale)]
                       (with-meta
                         (map #(array-map :x (+ (:x %) x) :y (+ (:y %) y))
                           [{:x 0 :y 0}
                            {:x 0 :y 15}
                            {:x 4 :y 12}
                            {:x 7 :y 17}
                            {:x 9 :y 16}
                            {:x 7 :y 11}
                            {:x 11 :y 11}])
                         {:invisible? true}))]
              :let  [reflection (mapv #(reflect % mirror) shape)]]
        ;; reflection
        (ui/with-paint ctx [^Paint paint-reflection-fill {:fill "000"}]
          (with-open [filter (ImageFilter/makeDropShadow 0 0 30 30 (unchecked-int 0xFFFF0000))]
            (.setImageFilter paint-reflection-fill filter)
            (draw-path canvas reflection paint-reflection-fill)))
        (ui/with-paint ctx [paint-reflection-stroke {:stroke "C33" :width (/ 2 scale)}]
          (draw-path canvas reflection paint-reflection-stroke))
      
        ;; shape
        (when-not (:invisible? (meta shape))
          (ui/with-paint ctx [paint-shape {:stroke "71DFC2" :width (/ 2 scale)}]
            (draw-path canvas shape paint-shape))))
      
      ;; mirror
      (ui/with-paint ctx [^Paint paint-mirror {:stroke "FFF" :width (/ 1 scale)}]
        (with-open [effect (PathEffect/makeDash (float-array [3 3]) 0)
                    path   (Path.)]
          (.setPathEffect paint-mirror effect)            
          (.moveTo path (:x a) (:y a))
          (.cubicTo path (:x b) (:y b) (:x c) (:y c) (:x d) (:y d))
          (.drawPath canvas path paint-mirror)))
                    
      ;; handles
      (ui/with-paint ctx [paint-handles {:stroke "71DFC2" :width (/ 1 scale)}]
        (canvas/draw-circle canvas (:x a) (:y a) 6 paint-handles)
        (canvas/draw-circle canvas (:x b) (:y b) 6 paint-handles)
        (canvas/draw-circle canvas (:x c) (:y c) 6 paint-handles)
        (canvas/draw-circle canvas (:x d) (:y d) 6 paint-handles)
        (canvas/draw-line canvas (:x a) (:y a) (:x b) (:y b) paint-handles)
        (canvas/draw-line canvas (:x c) (:y c) (:x d) (:y d) paint-handles))
      (window/request-frame (:window ctx)))))

(defn on-event [ctx e]
  (cond
    (= :mouse-move (:event e))
    (swap! *state assoc :mouse {:x (:x e) :y (:y e)})))

(defn handles []
  (for [[idx {:keys [x y]}] (map vector (range) (:handles @*state))]
    [ui/align {:x :left :y :top}     
     [ui/padding {:left (- x 8), :top (- y 8)}
      [ui/draggable
       {:on-drag (fn [e]
                   (swap! *state update :handles
                     (fn [pos]
                       (cond-> pos
                         true (update idx update :x + (-> e :delta-last :x))
                         true (update idx update :y + (-> e :delta-last :y))
                         (= idx 0) (update 1 update :x + (-> e :delta-last :x))
                         (= idx 0) (update 1 update :y + (-> e :delta-last :y))
                         (= idx 3) (update 2 update :x + (-> e :delta-last :x))
                         (= idx 3) (update 2 update :y + (-> e :delta-last :y))))))}
       [ui/gap {:size 16}]]]]))

(ui/defcomp ui []
  [ui/stack
   [ui/canvas
    {:on-paint on-paint
     :on-event on-event}]
   (handles)])
