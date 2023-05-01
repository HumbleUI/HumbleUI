(ns examples.canvas-shapes
  (:require
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.types IPoint]
    [io.github.humbleui.skija Canvas]))


(def grid-seq (into []
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
  {:paint-rect
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
           (canvas/draw-rect canvas rect fill)))))})

(def ui
  (ui/vscrollbar
    (ui/halign 0.5
      (ui/grid
        (partition-all 3
          (for [paint-kw [:paint-rect :paint-rect :paint-rect
                          :paint-rect :paint-rect :paint-rect
                          :paint-rect :paint-rect :paint-rect]]
            (ui/padding 10
              (ui/column
                (ui/label (pr-str paint-kw))
                (ui/gap 0 10)
                (ui/width 250
                  (ui/height 250
                    (ui/canvas {:on-paint (fn [ctx ^Canvas canvas ^IPoint size]
                                            (on-paint (paint-kw->fn paint-kw) ctx canvas size))})))))))))))
