(in-ns 'io.github.humbleui.ui)

(import '[io.github.humbleui.skija AnimationFrameInfo Bitmap Codec Image SamplingMode])

(defn- img-measure [scale width height ctx cs]
  (case scale
    :content (util/ipoint
               (math/ceil (* width (:scale ctx)))
               (math/ceil (* height (:scale ctx))))
    :fit     (let [aspect (/ width height)]
               (util/ipoint
                 (min (:width cs) (* (:height cs) aspect))
                 (min (/ (:width cs) aspect) (:height cs))))
    :fill    cs
    #_else   cs))

(defn- img-rects [scale xpos ypos width height ctx ^IRect bounds]
  (let [wscale     (/ (:width bounds) width)
        hscale     (/ (:height bounds) height)
        img-scale  (case (or scale :fit)
                     :content (:scale ctx)
                     :fit     (min wscale hscale)
                     :fill    (max wscale hscale)
                     #_else   (* (:scale ctx) scale))
        img-width  (* width img-scale)
        img-height (* height img-scale)
        img-left   (+ (:x bounds)
                     (* (:width bounds) xpos)
                     (- (* img-width xpos)))
        img-top    (+ (:y bounds)
                     (* (:height bounds) ypos)
                     (- (* img-height ypos)))
        img-rect   (util/rect-xywh img-left img-top img-width img-height)
        dst-rect   (.intersect (.toRect bounds) img-rect)]
    (when dst-rect
      (let [src-rect (-> dst-rect
                       (.offset (- img-left) (- img-top))
                       (.scale (/ 1 img-scale)))]
        [src-rect dst-rect]))))

(defn- img-reconcile-opts [this new-element]
  (let [opts   (parse-opts new-element)
        scale' (or (util/checked-get-optional opts :scale #(or (= :fit %) (= :fill %) (= :content %) (number? %))) :fit)]
    (when (not= scale scale')
      (util/set!! this :scale scale')
      (invalidate-size this))
    (util/set!! this :xpos  (or (util/checked-get-optional opts :xpos number?) 0.5))
    (util/set!! this :ypos  (or (util/checked-get-optional opts :ypos number?) 0.5))
    (util/set!! this :sampling
      (case (:sampling opts)
        nil          SamplingMode/MITCHELL
        :nearest     SamplingMode/DEFAULT
        :linear      SamplingMode/LINEAR
        :mitchell    SamplingMode/MITCHELL
        :catmull-rom SamplingMode/CATMULL_ROM
        (:sampling opts)))))

(util/deftype+ AnImage [^:mut scale
                        ^:mut xpos
                        ^:mut ypos
                        ^:mut ^SamplingMode sampling
                        ^Image image
                        width
                        height]
  :extends ATerminalNode
  
  (-measure-impl [_ ctx cs]
    (img-measure scale width height ctx cs))
  
  (-draw-impl [this ctx bounds viewport ^Canvas canvas]
    (when-some [[src-rect dst-rect] (img-rects scale xpos ypos width height ctx bounds)]
      (.drawImageRect canvas image src-rect dst-rect sampling #_:paint nil #_:strict false)))
  
  (-should-reconcile? [_this ctx new-element]
    (opts-match? [:src] element new-element))
  
  (-reconcile-opts [this _ctx new-element]
    (img-reconcile-opts this new-element))
  
  (-unmount-impl [this]
    (util/close image)))

(defn- image-ctor [opts]
  (let [src   (util/checked-get opts :src util/slurpable?)
        image (try
                (Image/makeFromEncoded (util/slurp-bytes src))
                (catch Exception e
                  ; (util/log-error e)
                  (Image/makeFromEncoded 
                    (util/slurp-bytes
                      (io/resource "io/github/humbleui/ui/image/not_found.png")))))
        width  (.getWidth ^Image image)
        height (.getHeight ^Image image)]
    (map->AnImage
      {:image  image
       :width  width
       :height height})))

(util/deftype+ Animation [^:mut scale
                          ^:mut xpos
                          ^:mut ypos
                          ^:mut ^SamplingMode sampling
                          width
                          height
                          durations
                          images
                          start]
  :extends ATerminalNode
  
  (-measure-impl [_ ctx cs]
    (img-measure scale width height ctx cs))
  
  (-draw-impl [_ ctx bounds viewport ^Canvas canvas]
    (let [total-duration (reduce + 0 durations)
          offset         (mod (- (util/now) start) total-duration)
          frame          (loop [durations durations
                                time      0
                                frame     0]
                           (if (>= time offset)
                             (dec frame)
                             (recur (next durations) (long (+ time (first durations))) (inc frame))))
          frame          (util/clamp frame 0 (dec (count durations)))
          next-offset    (reduce + 0 (take (inc frame) durations))]
      (when-some [[src-rect dst-rect] (img-rects scale xpos ypos width height ctx bounds)]
        (.drawImageRect canvas (nth images frame) src-rect dst-rect sampling #_:paint nil #_:strict false))
      (util/schedule #(window/request-frame (:window ctx)) (- next-offset offset))))

  (-should-reconcile? [_this ctx new-element]
    (opts-match? [:src] element new-element))

  (-reconcile-opts [this _ctx new-element]
    (img-reconcile-opts this new-element))

  (-unmount-impl [this]
    (doseq [image images]
      (util/close image))))

(defn- animation-ctor [opts]
  (let [src (util/checked-get opts :src util/slurpable?)]
    (with-open [codec (Codec/makeFromData (Data/makeFromBytes (util/slurp-bytes src)))]
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
        (map->Animation
          {:width     (.getWidth codec)
           :height    (.getHeight codec)
           :durations durations
           :images    images
           :start     (util/now)})))))
