(in-ns 'io.github.humbleui.ui)

(import '[io.github.humbleui.skija AnimationFrameInfo Bitmap Codec Image SamplingMode])

(defn- img-measure [opts width height ctx cs]
  (case (or (:scale opts) :fit)
    :content (core/ipoint
               (math/ceil (* width (:scale ctx)))
               (math/ceil (* height (:scale ctx))))
    :fit     (let [aspect (/ width height)]
               (core/ipoint
                 (min (:width cs) (* (:height cs) aspect))
                 (min (/ (:width cs) aspect) (:height cs))))
    :fill    cs
    #_else   cs))

(defn- img-rects [opts width height ctx ^IRect bounds]
  (let [{:keys [scale xpos ypos]} opts
        scale      (or scale :fit)
        xpos       (or xpos 0.5)
        ypos       (or ypos 0.5)
        wscale     (/ (:width bounds) width)
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
        img-rect   (core/rect-xywh img-left img-top img-width img-height)
        dst-rect   (.intersect (.toRect bounds) img-rect)]
    (when dst-rect
      (let [src-rect (-> dst-rect
                       (.offset (- img-left) (- img-top))
                       (.scale (/ 1 img-scale)))]
        [src-rect dst-rect]))))

(defn- img-sampling [opts]
  (case (:sampling opts)
    nil          SamplingMode/MITCHELL
    :nearest     SamplingMode/DEFAULT
    :linear      SamplingMode/LINEAR
    :mitchell    SamplingMode/MITCHELL
    :catmull-rom SamplingMode/CATMULL_ROM
    (:sampling opts)))

(core/deftype+ AnImage [^Image image width height aspect]
  :extends ATerminalNode
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[_ opts _] (parse-element element)]
      (img-measure opts width height ctx cs)))
  
  (-draw-impl [this ctx bounds ^Canvas canvas]
    (let [[_ opts _]  (parse-element element)]
      (when-some [[src-rect dst-rect] (img-rects opts width height ctx bounds)]
        (.drawImageRect canvas image src-rect dst-rect (img-sampling opts) #_:paint nil #_:strict false))))
  
  (-should-reconcile? [_this ctx new-element]
    (let [[_ _ [src]] (parse-element element)
          [_ _ [new-src]] (parse-element new-element)]
      (= src new-src)))
  
  (-unmount-impl [this]
    (core/close image)))

(defn- image-ctor
  ([src]
   (image-ctor {} src))
  ([opts src]
   (let [image (try
                 (Image/makeFromEncoded (core/slurp-bytes src))
                 (catch Exception e
                   ; (core/log-error e)
                   (Image/makeFromEncoded 
                     (core/slurp-bytes
                       (io/resource "io/github/humbleui/ui/image/not_found.png")))))
         width  (.getWidth ^Image image)
         height (.getHeight ^Image image)]
     (map->AnImage {:image  image
                    :width  width
                    :height height
                    :aspect (/ width height)}))))

(core/deftype+ Animation [width height durations images start]
  :extends ATerminalNode
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (let [[_ opts _] (parse-element element)]
      (img-measure opts width height ctx cs)))
  
  (-draw-impl [_ ctx rect ^Canvas canvas]
    (let [[_ opts _]     (parse-element element)
          total-duration (reduce + 0 durations)
          offset         (mod (- (core/now) start) total-duration)
          frame          (loop [durations durations
                                time      0
                                frame     0]
                           (if (>= time offset)
                             (dec frame)
                             (recur (next durations) (long (+ time (first durations))) (inc frame))))
          frame          (core/clamp frame 0 (dec (count durations)))
          next-offset    (reduce + 0 (take (inc frame) durations))]
      (when-some [[src-rect dst-rect] (img-rects opts width height ctx rect)]
        (.drawImageRect canvas (nth images frame) src-rect dst-rect (img-sampling opts) #_:paint nil #_:strict false))
      (core/schedule #(window/request-frame (:window ctx)) (- next-offset offset))))

  (-should-reconcile? [_this ctx new-element]
    (let [[_ _ [src]] (parse-element element)
          [_ _ [new-src]] (parse-element new-element)]
      (= src new-src)))

  (-unmount-impl [this]
    (doseq [image images]
      (core/close image))))

(defn- animation-ctor
  ([src]
   (animation-ctor {} src))
  ([opts src]
   (with-open [codec (Codec/makeFromData (Data/makeFromBytes (core/slurp-bytes src)))]
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
          :start     (core/now)})))))
