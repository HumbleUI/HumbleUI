(ns io.github.humbleui.ui.animation
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.window :as window])
  (:import
    [io.github.humbleui.skija AnimationFrameInfo Bitmap Canvas Codec Data Image]
    [java.lang AutoCloseable]))

(core/deftype+ Animation [width height durations images start]
  protocols/IComponent
  (-measure [_ _ctx _cs]
    (core/ipoint width height))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [total-duration (reduce + 0 durations)
          offset         (mod (- (core/now) start) total-duration)
          frame          (loop [durations durations
                                time      0
                                frame     0]
                           (if (>= time offset)
                             (dec frame)
                             (recur (next durations) (long (+ time (first durations))) (inc frame))))
          frame          (core/clamp frame 0 (dec (count durations)))
          next-offset    (reduce + 0 (take (inc frame) durations))]
      (.drawImageRect canvas (nth images frame) (core/rect rect))
      (core/schedule #(window/request-frame (:window ctx)) (- next-offset offset))))
  
  (-event [_ _ctx _event])
  
  (-iterate [this _ctx cb]
    (cb this))
  
  AutoCloseable
  (close [_]))
    ; (doseq [image images]
    ;   (.close image))))

(defn animation [src]
  (with-open [codec  (Codec/makeFromData (Data/makeFromBytes (core/slurp-bytes src)))]
    (let [frames    (.getFrameCount codec)
          durations (mapv #(.getDuration ^AnimationFrameInfo %) (.getFramesInfo codec))
          info      (.getImageInfo codec)
          images    (mapv
                      (fn [frame]
                        (with-open [bitmap (doto (Bitmap.)
                                             (.allocPixels info))]
                          (.readPixels codec bitmap frame)
                          (.setImmutable bitmap)
                          (Image/makeFromBitmap bitmap)))
                      (range frames))]
      (->Animation (.getWidth codec) (.getHeight codec) durations images (core/now)))))
