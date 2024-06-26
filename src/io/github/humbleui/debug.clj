(ns io.github.humbleui.debug
  (:require
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.signal :as signal]))

(defonce *graphs?
  (signal/signal false))

(defonce *outlines?
  (signal/signal false))

(defonce *continuous-render?
  (signal/signal false))

(def width
  100)

(def height
  50)

(def max-time-ms
  50)

(def ^{:tag "[D"} frame-starts
  (double-array width))

(def ^{:tag "[D"} frame-lengths
  (double-array width))

(def *frame-idx
  (volatile! 0))

(defmacro frame-get [array idx]
  `(aget ~array (mod (+ ~idx @*frame-idx) width)))

(def ^{:tag "[D"} event-starts
  (double-array width))

(def *event-idx
  (volatile! 0))

(defmacro event-get [array idx]
  `(aget ~array (mod (+ ~idx @*event-idx) width)))

(defmacro measure [& body]
  `(if @*graphs?
     (let [t#   (System/nanoTime)
           res# (do ~@body)
           dt#  (- (System/nanoTime) t#)]
       (aset frame-starts  @*frame-idx (/ t# 1000000.0))
       (aset frame-lengths @*frame-idx (/ dt# 1000000.0))
       (vswap! *frame-idx #(-> % inc (mod width)))
       res#)
     (do ~@body)))

(defmacro on-event-start []
  `(when @*graphs?
     (let [t# (System/nanoTime)]
       (aset event-starts @*event-idx (/ t# 1000000.0))
       (vswap! *event-idx #(-> % inc (mod width))))))

(defn draw-frames-impl [canvas]
  (with-open [bg   (paint/fill 0x4033CC33)
              fg   (paint/fill 0xFF33CC33)
              font (font/make-with-size nil 11)]
    (let [ms->y #(- height (-> % (* height) (/ max-time-ms)))
          last  (frame-get frame-lengths (dec width))]
      ;; paint times
      (canvas/draw-rect canvas (core/irect-ltrb 0 0 width height) bg)
      (canvas/draw-rect canvas (core/irect-ltrb 0 (ms->y 1000/120) width (+ (ms->y 1000/120) 1)) bg)
      (canvas/draw-rect canvas (core/irect-ltrb 0 (ms->y 1000/60) width (+ (ms->y 1000/60) 1)) bg)
      (canvas/draw-rect canvas (core/irect-ltrb 0 (ms->y 1000/30) width (+ (ms->y 1000/30) 1)) bg)
      (canvas/draw-string canvas (format "paint %.2f ms" last) 4 12 font fg)
      (doseq [i    (range width)
              :let [ms (frame-get frame-lengths i)]]
        (canvas/draw-rect canvas (core/irect-ltrb i (ms->y ms) (+ i 1) height) fg))
      
      ;; fps
      (let [fps   (loop [i (dec width)]
                    (if (<= i 1)
                      0
                      (let [dt (- (frame-get frame-starts i) (frame-get frame-starts (dec i)))]
                        (if (> dt 50)
                          (recur (dec i))
                          (/ 1000.0 dt)))))]
        (canvas/translate canvas (+ width 10) 0)
        (canvas/draw-rect canvas (core/irect-ltrb 0 0 width height) bg)
        (canvas/draw-rect canvas (core/irect-ltrb 0 (ms->y 1000/120) width (+ (ms->y 1000/120) 1)) bg)
        (canvas/draw-rect canvas (core/irect-ltrb 0 (ms->y 1000/60) width (+ (ms->y 1000/60) 1)) bg)
        (canvas/draw-rect canvas (core/irect-ltrb 0 (ms->y 1000/30) width (+ (ms->y 1000/30) 1)) bg)
        (canvas/draw-string canvas (format "fps %.0f" fps) 4 12 font fg)
        (doseq [i     (range 1 width)
                :let  [dt (- (frame-get frame-starts i) (frame-get frame-starts (dec i)))
                       y  (ms->y dt)]
                :when (< dt 50)]
          (canvas/draw-rect canvas (core/irect-ltrb i (- y 1) (+ i 1) (+ y 1)) fg)))
      
      ;; events
      (canvas/translate canvas (+ width 10) 0)
      (canvas/draw-rect canvas (core/irect-ltrb 0 0 width height) bg)
      (canvas/draw-rect canvas (core/irect-ltrb 0 (ms->y 1000/120) width (+ (ms->y 1000/120) 1)) bg)
      (canvas/draw-rect canvas (core/irect-ltrb 0 (ms->y 1000/60) width (+ (ms->y 1000/60) 1)) bg)
      (canvas/draw-rect canvas (core/irect-ltrb 0 (ms->y 1000/30) width (+ (ms->y 1000/30) 1)) bg)
      (canvas/draw-string canvas "events" 4 12 font fg)
      (doseq [i     (range 1 width)
              :let  [dt (- (event-get event-starts i) (event-get event-starts (dec i)))
                     y  (ms->y dt)]
              :when (< dt 50)]
        (canvas/draw-rect canvas (core/irect-ltrb i (- y 1) (+ i 1) (+ y 1)) fg)))))
      

(defmacro draw-frames [canvas]
  `(when @*graphs?
     (draw-frames-impl ~canvas)))
