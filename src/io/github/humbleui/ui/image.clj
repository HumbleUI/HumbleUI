(ns io.github.humbleui.ui.image
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [io.github.humbleui.skija Canvas Image SamplingMode]))

(core/deftype+ AnImage [^Image image opts]
  :extends core/ATerminal
  protocols/IComponent
  (-measure [_ _ctx cs]
    (let [aspect (/ (.getWidth image) (.getHeight image))]
      (core/ipoint (:width cs) (/ (:width cs) aspect))))
  
  (-draw [_ _ctx rect ^Canvas canvas]
    (let [aspect   (/ (.getWidth image) (.getHeight image))
          src-rect (core/rect-xywh 0 0 (.getWidth image) (.getHeight image))
          dst-rect (core/rect-xywh (:x rect) (:y rect) (:width rect) (/ (:width rect) aspect))
          sampling (case (:sampling-mode opts)
                     :nearest     SamplingMode/DEFAULT
                     :linear      SamplingMode/LINEAR
                     :mitchell    SamplingMode/MITCHELL
                     :catmull-rom SamplingMode/CATMULL_ROM
                     nil          SamplingMode/MITCHELL
                     (:sampling-mode opts))]
      (.drawImageRect canvas image src-rect dst-rect sampling #_:paint nil #_:strict false))))

(defn image
  ([src]
   (image {} src))
  ([opts src]
   (let [image (Image/makeFromEncoded (core/slurp-bytes src))]
     (map->AnImage {:image image
                    :opts opts}))))
