(ns examples.canvas-shapes
  (:require
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.types IPoint Point]
    [io.github.humbleui.skija Canvas]))

(def grid-seq
  (into []
    cat
    (for [y (range 11) x (range 11)
          :let [x (* x 50)
                y (* y 50)]]
      [[[x y] [x (inc y)]] [[x y] [(inc x) y]]])))

(defn on-paint [paint-shape ctx ^Canvas canvas ^IPoint size]
  (let [{:keys [scale]} ctx
        {:keys [width height]} size]

    (let [size (max width height)
          zoom 1000
          h-size (/ size 2)]
      (canvas/with-canvas canvas
        (canvas/translate canvas (quot width 2) (quot height 2))
        (with-open [stroke (paint/stroke 0xFFCCCCCC (* scale zoom))]
          (doseq [[[x1 y1] [x2 y2]] grid-seq
                  :let [x1'  (- x1 h-size)
                        y1'  (- y1 h-size)
                        x2'  (- x2 h-size)
                        y2'  (- y2 h-size)]]
            (canvas/draw-line canvas x1' y1' x2' y2' stroke)))))

    (let [size (max width height)]
      (paint-shape ctx canvas width height scale size))))

(def paint-kw->fn
  {:paint-point
   (fn paint-point [ctx ^Canvas canvas width height scale size]
     (let [{:keys [font-ui fill-text]} ctx
           p (core/ipoint 50 100)
           [x y] [250 450]]
       (canvas/with-canvas canvas
         (with-open [stroke (paint/stroke 0xFF00CCCC 8)]
           (canvas/draw-string canvas "point [[50 100]]" (:x p) (- (:y p) 15) font-ui fill-text)
           (canvas/draw-point canvas p stroke))
         (with-open [stroke (paint/stroke 0xFFCC00CC 8)]
           (canvas/draw-string canvas "point [250 450]" x (- y 15) font-ui fill-text)
           (canvas/draw-point canvas x y stroke)))))
   :paint-points
   (fn paint-points [ctx ^Canvas canvas width height scale size]
     (let [{:keys [font-ui fill-text]} ctx
           points-coll (mapv (fn [[x y]] (Point. x y)) [[50 50] [100 100] [150 50] [200 100] [250 50]])
           ipoints-coll (mapv (fn [[x y]] (IPoint. x y)) [[50 150] [100 200] [150 150] [200 200] [250 150]])
           floats-coll [50 250 100 300 150 250 200 300 250 250]
           floats-arr (float-array [50 350 100 400 150 350 200 400 250 350])
           points-arr (into-array Point (map (fn [[x y]] (Point. x y)) [[50 450] [100 500] [150 450] [200 500] [250 450]]))
           ipoints-arr (into-array IPoint (map (fn [[x y]] (IPoint. x y)) [[50 550] [100 600] [150 550] [200 600] [250 550]]))]
       (canvas/with-canvas canvas
         (with-open [stroke (paint/stroke 0xFF00CCCC 5)]
           (canvas/draw-string canvas (str "points coll [[50 50] [100 100] [150 50] [200 100] [250 50]]") (:x (first points-coll)) (- (:y (first points-coll)) 8) font-ui fill-text)
           (canvas/draw-points canvas points-coll stroke))
         (with-open [stroke (paint/stroke 0xFFCC00CC 5)]
           (canvas/draw-string canvas (str "ipoints coll [[50 150] [100 200] [150 150] [200 200] [250 150]]") (:x (first ipoints-coll)) (- (:y (first ipoints-coll)) 8) font-ui fill-text)
           (canvas/draw-points canvas ipoints-coll stroke))
         (with-open [stroke (paint/stroke 0xFFCCCC00 5)]
           (canvas/draw-string canvas "floats coll [50 250 100 300 150 250 200 300 250 250]" (first floats-coll) (- (second floats-coll) 8) font-ui fill-text)
           (canvas/draw-points canvas floats-coll stroke))
         (with-open [stroke (paint/stroke 0xFFCC0000 5)]
           (canvas/draw-string canvas "floats arr #F[50 350 100 400 150 350 200 400 250 350]" (first floats-arr) (- (second floats-arr) 8) font-ui fill-text)
           (canvas/draw-points canvas floats-arr stroke))
         (with-open [stroke (paint/stroke 0xFF00CC00 5)]
           (canvas/draw-string canvas "points arr [[50 450] [100 500] [150 450] [200 500] [250 450]]" (:x (first points-arr)) (- (:y (first points-arr)) 8) font-ui fill-text)
           (canvas/draw-points canvas points-arr stroke))
         (with-open [stroke (paint/stroke 0xFF0000CC 5)]
           (canvas/draw-string canvas "ipoints arr [[50 550] [100 600] [150 550] [200 600] [250 550]]" (:x (first ipoints-arr)) (- (:y (first ipoints-arr)) 8) font-ui fill-text)
           (canvas/draw-points canvas ipoints-arr stroke)))))
   :paint-lines
   (fn paint-lines [ctx ^Canvas canvas width height scale size]
     (let [{:keys [font-ui fill-text]} ctx
           points-coll (mapv (fn [[x y]] (Point. x y)) [[50 50] [100 100] [150 50] [200 100] [250 50]])
           ipoints-coll (mapv (fn [[x y]] (IPoint. x y)) [[50 150] [100 200] [150 150] [200 200] [250 150]])
           floats-coll [50 250 100 300 150 250 200 300 250 250]
           floats-arr (float-array [50 350 100 400 150 350 200 400 250 350])
           points-arr (into-array Point (map (fn [[x y]] (Point. x y)) [[50 450] [100 500] [150 450] [200 500] [250 450]]))
           ipoints-arr (into-array IPoint (map (fn [[x y]] (IPoint. x y)) [[50 550] [100 600] [150 550] [200 600] [250 550]]))]
       (canvas/with-canvas canvas
         (with-open [stroke (paint/stroke 0xFF00CCCC 5)]
           (canvas/draw-string canvas (str "points coll [[50 50] [100 100] [150 50] [200 100] [250 50]]") (:x (first points-coll)) (- (:y (first points-coll)) 8) font-ui fill-text)
           (canvas/draw-lines canvas points-coll stroke))
         (with-open [stroke (paint/stroke 0xFFCC00CC 5)]
           (canvas/draw-string canvas (str "ipoints coll [[50 150] [100 200] [150 150] [200 200] [250 150]]") (:x (first ipoints-coll)) (- (:y (first ipoints-coll)) 8) font-ui fill-text)
           (canvas/draw-lines canvas ipoints-coll stroke))
         (with-open [stroke (paint/stroke 0xFFCCCC00 5)]
           (canvas/draw-string canvas "floats coll [50 250 100 300 150 250 200 300 250 250]" (first floats-coll) (- (second floats-coll) 8) font-ui fill-text)
           (canvas/draw-lines canvas floats-coll stroke))
         (with-open [stroke (paint/stroke 0xFFCC0000 5)]
           (canvas/draw-string canvas "floats arr #F[50 350 100 400 150 350 200 400 250 350]" (first floats-arr) (- (second floats-arr) 8) font-ui fill-text)
           (canvas/draw-lines canvas floats-arr stroke))
         (with-open [stroke (paint/stroke 0xFF00CC00 5)]
           (canvas/draw-string canvas "points arr [[50 450] [100 500] [150 450] [200 500] [250 450]]" (:x (first points-arr)) (- (:y (first points-arr)) 8) font-ui fill-text)
           (canvas/draw-lines canvas points-arr stroke))
         (with-open [stroke (paint/stroke 0xFF0000CC 5)]
           (canvas/draw-string canvas "ipoints arr [[50 550] [100 600] [150 550] [200 600] [250 550]]" (:x (first ipoints-arr)) (- (:y (first ipoints-arr)) 8) font-ui fill-text)
           (canvas/draw-lines canvas ipoints-arr stroke)))))
   :paint-polygon
   (fn paint-polygon [ctx ^Canvas canvas width height scale size]
     (let [{:keys [font-ui fill-text]} ctx
           points-coll (mapv (fn [[x y]] (Point. x y)) [[50 50] [100 100] [150 50] [200 100] [250 50]])
           ipoints-coll (mapv (fn [[x y]] (IPoint. x y)) [[50 150] [100 200] [150 150] [200 200] [250 150]])
           floats-coll [50 250 100 300 150 250 200 300 250 250]
           floats-arr (float-array [50 350 100 400 150 350 200 400 250 350])
           points-arr (into-array Point (map (fn [[x y]] (Point. x y)) [[50 450] [100 500] [150 450] [200 500] [250 450]]))
           ipoints-arr (into-array IPoint (map (fn [[x y]] (IPoint. x y)) [[50 550] [100 600] [150 550] [200 600] [250 550]]))]
       (canvas/with-canvas canvas
         (with-open [stroke (paint/stroke 0xFF00CCCC 5)]
           (canvas/draw-string canvas (str "points coll [[50 50] [100 100] [150 50] [200 100] [250 50]]") (:x (first points-coll)) (- (:y (first points-coll)) 8) font-ui fill-text)
           (canvas/draw-polygon canvas points-coll stroke))
         (with-open [stroke (paint/stroke 0xFFCC00CC 5)]
           (canvas/draw-string canvas (str "ipoints coll [[50 150] [100 200] [150 150] [200 200] [250 150]]") (:x (first ipoints-coll)) (- (:y (first ipoints-coll)) 8) font-ui fill-text)
           (canvas/draw-polygon canvas ipoints-coll stroke))
         (with-open [stroke (paint/stroke 0xFFCCCC00 5)]
           (canvas/draw-string canvas "floats coll [50 250 100 300 150 250 200 300 250 250]" (first floats-coll) (- (second floats-coll) 8) font-ui fill-text)
           (canvas/draw-polygon canvas floats-coll stroke))
         (with-open [stroke (paint/stroke 0xFFCC0000 5)]
           (canvas/draw-string canvas "floats arr #F[50 350 100 400 150 350 200 400 250 350]" (first floats-arr) (- (second floats-arr) 8) font-ui fill-text)
           (canvas/draw-polygon canvas floats-arr stroke))
         (with-open [stroke (paint/stroke 0xFF00CC00 5)]
           (canvas/draw-string canvas "points arr [[50 450] [100 500] [150 450] [200 500] [250 450]]" (:x (first points-arr)) (- (:y (first points-arr)) 8) font-ui fill-text)
           (canvas/draw-polygon canvas points-arr stroke))
         (with-open [stroke (paint/stroke 0xFF0000CC 5)]
           (canvas/draw-string canvas "ipoints arr [[50 550] [100 600] [150 550] [200 600] [250 550]]" (:x (first ipoints-arr)) (- (:y (first ipoints-arr)) 8) font-ui fill-text)
           (canvas/draw-polygon canvas ipoints-arr stroke)))))
   :paint-line
   (fn paint-line [ctx ^Canvas canvas width height scale size]
     (let [{:keys [font-ui fill-text]} ctx
           p1 (core/ipoint 100 450)
           p2 (core/ipoint 300 200)
           [x1 y1 x2 y2] [250 150 200 50]]
       (canvas/with-canvas canvas
         (with-open [stroke (paint/stroke 0xFF00CCCC 1)]
           (canvas/draw-string canvas "line [[100 450] [300 200]]" (+ (:x p1) 32) (:y p1) font-ui fill-text)
           (canvas/draw-line canvas p1 p2 stroke))
         (with-open [stroke (paint/stroke 0xFFCC00CC 1)]
           (canvas/draw-string canvas "line [250 150 200 50]" x1 (- y1 8) font-ui fill-text)
           (canvas/draw-line canvas x1 y1 x2 y2 stroke)))))
   :paint-arc
   (fn paint-arc [ctx ^Canvas canvas width height scale size]
     (let [{:keys [font-ui fill-text]} ctx
           arc-1 {:rect (core/irect-xywh 50 100 150 50) :start-angle 45 :sweep-angle 135 :use-center true}
           arc-2 {:rect (core/rrect-xywh 250 150 100 50 5) :start-angle 90 :sweep-angle -135 :use-center true}
           arc-3 {:rect (core/rect-xywh 300 300 150 100) :start-angle 135 :sweep-angle 135 :use-center false}
           arc-4 {:left 100 :top 350 :right 150 :bottom 500 :start-angle 180 :sweep-angle -135 :use-center false}]
       (canvas/with-canvas canvas
         (with-open [fill (paint/fill 0xFF00CCCC)]
           (canvas/draw-string canvas "irect arc " (-> arc-1 :rect :x) (-> arc-1 :rect :y (- 8)) font-ui fill-text)
           (canvas/draw-arc canvas (:rect arc-1) (:start-angle arc-1) (:sweep-angle arc-1) (:use-center arc-1) fill))
         (with-open [fill (paint/fill 0xFFCC00CC)]
           (canvas/draw-string canvas "rrect arc " (-> arc-2 :rect :x) (-> arc-2 :rect :y (- 8)) font-ui fill-text)
           (canvas/draw-arc canvas (:rect arc-2) (:start-angle arc-2) (:sweep-angle arc-2) (:use-center arc-2) fill))
         (with-open [fill (paint/fill 0xFFCCCC00)]
           (canvas/draw-string canvas "rect arc " (-> arc-3 :rect :x) (-> arc-3 :rect :y (- 8)) font-ui fill-text)
           (canvas/draw-arc canvas (:rect arc-3) (:start-angle arc-3) (:sweep-angle arc-3) (:use-center arc-3) fill))
         (with-open [fill (paint/fill 0xFFCC0000)]
           (canvas/draw-string canvas "arc " (:left arc-4) (- (:top arc-4) 8) font-ui fill-text)
           (canvas/draw-arc canvas (:left arc-4) (:top arc-4) (:right arc-4) (:bottom arc-4) (:start-angle arc-4) (:sweep-angle arc-4) (:use-center arc-4) fill)))))
   :paint-rect
   (fn paint-rect [ctx ^Canvas canvas width height scale size]
     (let [{:keys [font-ui fill-text]} ctx
           irect (core/irect-xywh 100 50 150 50)
           rrect (core/rrect-xywh 50 150 100 50 5)
           rect (core/rect-xywh 100 250 150 100)]
       (canvas/with-canvas canvas
         (with-open [fill (paint/fill 0xFF00CCCC)]
           (canvas/draw-string canvas "irect [100 50 150 50]" (:x irect) (- (:y irect) 8) font-ui fill-text)
           (canvas/draw-rect canvas irect fill))
         (with-open [fill (paint/fill 0xFFCC00CC)]
           (canvas/draw-string canvas "rrect [50 150 100 50 5]" (:x rrect) (- (:y rrect) 8) font-ui fill-text)
           (canvas/draw-rect canvas rrect fill))
         (with-open [fill (paint/fill 0xFFCCCC00)]
           (canvas/draw-string canvas "rect [100 250 150 100]" (:x rect) (- (:y rect) 8) font-ui fill-text)
           (canvas/draw-rect canvas rect fill)))))
   :paint-oval
   (fn paint-oval [ctx ^Canvas canvas width height scale size]
     (let [{:keys [font-ui fill-text]} ctx
           irect (core/irect-xywh 100 50 150 50)
           rrect (core/rrect-xywh 50 150 100 50 5)
           rect (core/rect-xywh 100 250 150 100)]
       (canvas/with-canvas canvas
         (with-open [fill (paint/fill 0xFF00CCCC)]
           (canvas/draw-string canvas "irect [100 50 150 50]" (:x irect) (- (:y irect) 8) font-ui fill-text)
           (canvas/draw-oval canvas irect fill))
         (with-open [fill (paint/fill 0xFFCC00CC)]
           (canvas/draw-string canvas "rrect [50 150 100 50 5]" (:x rrect) (- (:y rrect) 8) font-ui fill-text)
           (canvas/draw-oval canvas rrect fill))
         (with-open [fill (paint/fill 0xFFCCCC00)]
           (canvas/draw-string canvas "rect [100 250 150 100]" (:x rect) (- (:y rect) 8) font-ui fill-text)
           (canvas/draw-oval canvas rect fill)))))
   :paint-circle
   (fn paint-circle [ctx ^Canvas canvas width height scale size]
     (let [{:keys [font-ui fill-text]} ctx
           [x y r] [150 100 50]]
       (canvas/with-canvas canvas
         (with-open [fill (paint/fill 0xFF00CCCC)]
           (canvas/draw-string canvas "circle [150 100 50]" x (- y r 8) font-ui fill-text)
           (canvas/draw-circle canvas x y r fill)))))
   :paint-rrect
   (fn paint-oval [ctx ^Canvas canvas width height scale size]
     (let [{:keys [font-ui fill-text]} ctx
           rrect (core/rrect-xywh 50 150 100 50 5)]
       (with-open [fill (paint/fill 0xFFCC00CC)]
         (canvas/draw-string canvas "rrect [50 150 100 50 5]" (:x rrect) (- (:y rrect) 8) font-ui fill-text)
         (canvas/draw-rrect canvas rrect fill))))
   :paint-drrect
   (fn paint-oval [ctx ^Canvas canvas width height scale size]
     (let [{:keys [font-ui fill-text]} ctx
           rrect-1 (core/rrect-xywh 50 150 100 50 5)
           rrect-2 (core/rrect-xywh 60 160 50 30 5)
           srrect-1 (core/rrect-xywh 50 350 100 50 5)
           srrect-2 (core/rrect-xywh 60 360 50 30 5)]
       (canvas/with-canvas canvas
         (with-open [fill (paint/fill 0xFFCC00CC)
                     stroke (paint/stroke 0xFFCC00CC 1)]
           (canvas/draw-string canvas "outer [50 150 100 50 5]" (:x rrect-1) (- (:y rrect-1) 36) font-ui fill-text)
           (canvas/draw-string canvas "inner [60 160 50 30 5]" (:x rrect-1) (- (:y rrect-1) 8) font-ui fill-text)
           (canvas/draw-rrect canvas rrect-1 stroke)
           (canvas/draw-rrect canvas rrect-2 stroke)
           (canvas/draw-string canvas "shifted outer [50 350 100 50 5]" (:x srrect-1) (- (:y srrect-1) 36) font-ui fill-text)
           (canvas/draw-string canvas "shifted inner [60 360 50 30 5]" (:x srrect-1) (- (:y srrect-1) 8) font-ui fill-text)
           (canvas/draw-double-rrect canvas srrect-1 srrect-2 fill)))))})

(defn ui []
  [ui/vscrollbar
   [ui/halign {:position 0.5}
    [ui/grid {:cols 3}
     (for [paint-kw [:paint-point :paint-points :paint-lines
                     :paint-polygon :paint-line :paint-arc
                     :paint-rect :paint-oval :paint-circle
                     :paint-rrect :paint-drrect]]
       [ui/padding {:padding 10}
        [ui/column {:gap 10}
         [ui/label (pr-str paint-kw)]
         [ui/size {:width 250, :height 250}
          [ui/canvas
           {:on-paint
            (fn [ctx ^Canvas canvas ^IPoint size]
              (on-paint (paint-kw->fn paint-kw) ctx canvas size))}]]]])]]])
