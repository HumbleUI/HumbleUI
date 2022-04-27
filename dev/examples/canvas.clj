(ns examples.canvas
  (:require
    [clojure.math :as math]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.types IPoint]
    [io.github.humbleui.skija Canvas Paint]))

(set! *warn-on-reflection* true)

;; https://en.wikipedia.org/wiki/Hilbert_curve
;; https://github.com/nextjournal/clerk-demo/blob/main/notebooks/logo.clj
(defn hilbert-curve-impl
  ([x-size y-size order]
   (hilbert-curve-impl 0 0 x-size 0 0 y-size order))
  ([x y xi xj yi yj n]
   (if (<= n 0)
     [[(+ x (/ (+ xi yi) 2)) (+ y (/ (+ xj yj) 2))]]
     (concat
       (hilbert-curve-impl x                       y                       (/ yi 2)     (/ yj 2)     (/ xi 2)     (/ xj 2)     (dec n))
       (hilbert-curve-impl (+ x (/ xi 2))          (+ y (/ xj 2))          (/ xi 2)     (/ xj 2)     (/ yi 2)     (/ yj 2)     (dec n))
       (hilbert-curve-impl (+ x (/ xi 2) (/ yi 2)) (+ y (/ xj 2) (/ yj 2)) (/ xi 2)     (/ xj 2)     (/ yi 2)     (/ yj 2)     (dec n))
       (hilbert-curve-impl (+ x (/ xi 2) yi)       (+ y (/ xj 2) yj)       (- (/ yi 2)) (- (/ yj 2)) (- (/ xi 2)) (- (/ xj 2)) (dec n))))))

(def hilbert-curve
  (core/memoize-last hilbert-curve-impl))

(def *last-event (atom nil))

(defn on-event [e]
  (when-not (#{:frame :frame-skija} (:event e))
    (reset! *last-event e)
    true))

(defn on-paint [ctx ^Canvas canvas ^IPoint size]
  (let [{:keys [font-ui fill-text leading scale]} ctx
        {:keys [width height]} size]
    
    ;; bg
    (let [size (max width height)]
      (canvas/with-canvas canvas
        (canvas/translate canvas (quot width 2) (quot height 2))
        (canvas/rotate canvas 90)
        (canvas/scale canvas 0.001)
        (with-open [stroke (paint/stroke 0xFFCCCCCC (* scale 1000))]
          (doseq [[[x1 y1] [x2 y2]] (partition 2 (hilbert-curve size size 6))
                  :let [x1'  (- x1 (/ size 2))
                        y1'  (- y1 (/ size 2))
                        x2'  (- x2 (/ size 2))
                        y2'  (- y2 (/ size 2))
                        x1'' (- (* x1' x1') (* y1' y1'))
                        y1'' (* 2 x1' y1')
                        x2'' (- (* x2' x2') (* y2' y2'))
                        y2'' (* 2 x2' y2')]]
            (canvas/draw-line canvas x1'' y1'' x2'' y2'' stroke)))))

    ;; event
    (let [event @*last-event]
      (loop [y  (+ 10 (* 2 leading scale))
             kv (sort-by first event)]
        (when-some [[k v] (first kv)]
          (do
            (canvas/draw-string canvas (str k " " v) (* 10 scale) y font-ui fill-text)
            (recur (+ y (* 2 leading scale)) (next kv))))))))

(def ui
  (ui/valign 0.5
    (ui/halign 0.5
      (ui/canvas {:on-paint on-paint
                  :on-event on-event}))))

; (reset! user/*example "canvas")