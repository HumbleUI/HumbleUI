(in-ns 'io.github.humbleui.ui)

(import
  '[io.github.humbleui.skija ColorAlphaType Image ImageInfo Surface])

(util/deftype+ ImageSnapshot [^:mut ^Image image]
  :extends AWrapperNode
  protocols/IComponent
  (-draw-impl [this ctx bounds ^Canvas canvas]
    (let [[_ opts _] (parse-element element)
          scale   (:scale opts)
          [sx sy] (if (some? scale)
                    ((juxt :x :y) scale)
                    (let [m44 (.getMat (.getLocalToDevice canvas))]
                      [(nth m44 0) (nth m44 5)]))
          w (int (math/ceil (* (:width bounds) sx)))
          h (int (math/ceil (* (:height bounds) sy)))]
      (when (and image
              (or 
                (not= (.getWidth image) w)
                (not= (.getHeight image) h)))
        (.close image)
        (set! image nil))
      (when (nil? image)
        (with-open [surface (Surface/makeRaster (ImageInfo/makeS32 w h ColorAlphaType/PREMUL))]
          (draw-child child ctx (util/irect-xywh 0 0 w h) (.getCanvas surface))
          (protocols/-set! this :image (.makeImageSnapshot surface))))
      (.drawImageRect canvas image (util/rect bounds)))))

(defn image-snapshot-ctor
  ([child]
   (image-snapshot-ctor {} child))
  ([opts child]
   (map->ImageSnapshot {})))
