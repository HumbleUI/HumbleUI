(in-ns 'io.github.humbleui.ui)

(import
  '[io.github.humbleui.skija ColorAlphaType Image ImageInfo Surface])

(util/deftype+ ImageSnapshot [^:mut scale
                              ^:mut ^Image image]
  :extends AWrapperNode
  
  (-draw-impl [this ctx bounds viewport ^Canvas canvas]
    (let [[sx sy] (if (some? scale)
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
      (when (and (pos? w) (pos? h))
        (when (nil? image)
          (with-open [surface (Surface/makeRaster (ImageInfo/makeS32 w h ColorAlphaType/PREMUL))]
            (draw child ctx (util/irect-xywh 0 0 w h) viewport (.getCanvas surface))
            (protocols/-set! this :image (.makeImageSnapshot surface))))
        (.drawImageRect canvas image (util/rect bounds)))))
  
  (-reconcile-opts [this _ctx new-element]
    (let [opts (parse-opts new-element)]
      (set! scale (:scale opts)))))

(defn image-snapshot-ctor
  ([child]
   (image-snapshot-ctor {} child))
  ([opts child]
   (map->ImageSnapshot {})))
