(in-ns 'io.github.humbleui.ui)

(import '[io.github.humbleui.skija Image SamplingMode])

(core/deftype+ AnImage [^Image image width height aspect]
  :extends ATerminalNode
  protocols/IComponent
  (-measure-impl [_ _ctx cs]
    (if (< (/ (:width cs) aspect) (:height cs))
      (core/ipoint (:width cs) (math/ceil (/ (:width cs) aspect)))
      (core/ipoint (math/ceil (* (:height cs) aspect)) (:height cs))))
  
  (-draw-impl [this ctx rect ^Canvas canvas]
    (let [src-rect    (core/rect-xywh 0 0 width height)
          [_ opts _]  (parse-element element)
          {w :width
           h :height} (protocols/-measure-impl this ctx rect)
          dst-rect    (core/rect-xywh (:x rect) (:y rect) w h)
          sampling    (case (:sampling-mode opts)
                        :nearest     SamplingMode/DEFAULT
                        :linear      SamplingMode/LINEAR
                        :mitchell    SamplingMode/MITCHELL
                        :catmull-rom SamplingMode/CATMULL_ROM
                        nil          SamplingMode/MITCHELL
                        (:sampling-mode opts))]
      (.drawImageRect canvas image src-rect dst-rect sampling #_:paint nil #_:strict false)))
  
  (-should-reconcile? [_this ctx new-element]
    (let [[_ _ [src]] (parse-element element)
          [_ _ [new-src]] (parse-element new-element)]
      (= src new-src)))
  
  (-unmount-impl [this]
    (.close image)))

(defn image
  ([src]
   (image {} src))
  ([opts src]
   (let [image  (Image/makeFromEncoded (core/slurp-bytes src))
         width  (.getWidth image)
         height (.getHeight image)]
     (map->AnImage {:image  image
                    :width  width
                    :height height
                    :aspect (/ width height)}))))
