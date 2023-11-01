(ns examples.framerate
  (:require
    [clojure.math :as math]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.window :as window])
  (:import
    [io.github.humbleui.skija Color4f Paint]))

(def ^Paint stroke
  (paint/stroke 0xFFFFFFFF 0.05))

(defn ease-cubic [t]
  (if (< t 0.5)
    (* 4 t t t)
    (- 1 (-> t (* -2) (+ 2) (math/pow 3) (/ 2)))))

(defn ease-sin [t]
  (-> t
    (* math/PI)
    (math/cos)
    (- 1)
    (/ -2)))

(defn angle [t]
  (let [t (if (<= t 0.5)
            (* t 2)
            (- 2 (* t 2)))
        t (ease-sin t)]
    (+ (* math/PI 0.3) (* t math/PI 1.4))))

(defn on-paint [interpolation fps ctx canvas size]
  (let [{:keys [scale window]} ctx
        {:keys [width height]} size
        t     (-> (System/currentTimeMillis) (mod 1500) (/ 1500))
        t     (-> t (* fps) math/round (/ fps) double)]
    ; (canvas/clear canvas 0xFFFFFFFF)
    (canvas/translate canvas (/ width 2) (/ height 2))
    ; (canvas/draw-string canvas (str fps) 0 0 font fill-text)
    (canvas/scale canvas (/ (min width height) 2.5))
    
    (case interpolation
      :no
      (let [a (angle t)]
        (.setColor stroke (unchecked-int 0xFFFFFFFF))
        (canvas/draw-circle canvas (math/sin a) (math/cos a) 0.1 stroke))
    
      :inbetweens
      (let [frames (max 1 (quot 60 fps))
            alpha  (/ 2 frames)
            color  (Color4f. ^floats (into-array Float/TYPE [1 1 1 alpha]))]
        (.setColor4f stroke color)
        (doseq [frame (range 0 frames)
                :let [t (- t (* frame 1/60))
                      a (angle t)]]
          (canvas/draw-circle canvas (math/sin a) (math/cos a) 0.1 stroke)))
      
      :blur
      (let [a       (angle t)
            [x y]   [(math/sin a) (math/cos a)]
            pa      (angle (- t (/ 1 fps)))
            [px py] [(math/sin pa) (math/cos pa)]
            v       (math/hypot (- x px) (- y py))
            a'      (if (<= t 0.5)
                      (- a (* 0.5 Math/PI))
                      (+ a (* 0.5 Math/PI)))
            [dx dy] [(* v (math/sin a')) (* v (math/cos a'))]
            frames  100
            alpha   (/ 2 frames)
            color   (Color4f. ^floats (into-array Float/TYPE [1 1 1 alpha]))]
        (.setColor4f stroke color)
        (doseq [frame (range 0 frames)
                :let [x' (+ x (* (/ frame frames) dx))
                      y' (+ y (* (/ frame frames) dy))]]
          (canvas/draw-circle canvas x' y' 0.1 stroke))
        (canvas/draw-line canvas x y (+ x dx) (+ y dy) stroke)))
    
    (window/request-frame window)))

(defn panel [interpolation fps]
  (ui/stack
    (ui/canvas {:on-paint #(on-paint interpolation fps %1 %2 %3)})
    (ui/center
      (ui/dynamic ctx [{:keys [font]} ctx]
        (ui/label {:font font} (str fps))))))

(def ui
  (ui/rect (paint/fill 0xFF0D1924)
    (ui/dynamic ctx [{:keys [face-ui scale]} ctx]
      (ui/with-context {:fill-text (paint/fill 0xFFFFFFFF)
                        :font      (font/make-with-cap-height face-ui (* scale 30))}
        (ui/padding 20
          (ui/with-bounds ::bounds
            (ui/dynamic ctx [{:keys [width height]} (::bounds ctx)]
              (let [gap   10
                    table [[(ui/halign 0.5 (ui/valign 1 (ui/gap 0 0)))
                            (ui/halign 0.5 (ui/valign 1 (ui/label "15 fps")))
                            (ui/halign 0.5 (ui/valign 1 (ui/label "30 fps")))
                            (ui/halign 0.5 (ui/valign 1 (ui/label "60 fps")))
                            (ui/halign 0.5 (ui/valign 1 (ui/label "120 fps")))]
                           [(ui/halign 1 (ui/valign 0.5 (ui/label "No interpolation")))
                            (panel :no 15)
                            (panel :no 30)
                            (panel :no 60)
                            (panel :no 120)]
                           [(ui/halign 1 (ui/valign 0.5 (ui/label "Motion blur")))
                            (panel :blur 15)
                            (panel :blur 30)
                            (panel :blur 60)
                            (panel :blur 120)]
                           [(ui/halign 1 (ui/valign 0.5 (ui/label "In-betweens")))
                            (panel :inbetweens 15)
                            (panel :inbetweens 30)
                            (panel :inbetweens 60)
                            (panel :inbetweens 120)]]
                    rows (count table)
                    cols (count (first table))
                    s (min
                        (quot (- width (* gap (dec cols))) cols)
                        (quot (- height (* gap (dec rows))) rows))]
                (ui/grid
                  (->> table
                    (map (fn [row]
                           (->> row
                             (map #(ui/width s (ui/height s %)))
                             (interpose (ui/gap gap 0)))))
                    (interpose [(ui/gap 0 gap)])))))))))))
