(ns io.github.humbleui.ui.image-snapshot
  (:require
    [clojure.math :as math]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [io.github.humbleui.types Rect]
    [io.github.humbleui.skija Canvas ColorAlphaType Image ImageInfo Surface]
    [java.lang AutoCloseable]))

(core/deftype+ ImageSnapshot [child ^:mut ^Image image]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))
  
  (-draw [this ctx ^Rect rect ^Canvas canvas]
    (let [m44 (.getLocalToDevice canvas)
          sx  (nth (.getMat m44) 0)
          sy  (nth (.getMat m44) 5)
          w   (math/ceil (* (:width rect) sx))
          h   (math/ceil (* (:height rect) sy))]
      (when (and image
              (or 
                (not= (.getWidth image) w)
                (not= (.getHeight image) h)))
        (.close image)
        (set! image nil))
      (when (nil? image)
        (with-open [surface (Surface/makeRaster (ImageInfo/makeS32 w h ColorAlphaType/PREMUL))]
          (core/draw child ctx (core/irect-xywh 0 0 w h) (.getCanvas surface))
          (protocols/-set! this :image (.makeImageSnapshot surface))))
      (.drawImageRect canvas image (.toRect rect))))

  (-event [_ ctx event])
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn image-snapshot [child]
  (->ImageSnapshot child nil))