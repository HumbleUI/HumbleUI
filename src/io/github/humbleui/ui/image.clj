(ns io.github.humbleui.ui.image
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [io.github.humbleui.skija Canvas Image]))

(core/deftype+ AnImage [^Image image]
  :extends core/ATerminal
  protocols/IComponent
  (-measure [_ _ctx cs]
    (let [aspect (/ (.getWidth image) (.getHeight image))]
      (core/ipoint (:width cs) (/ (:width cs) aspect))))
  
  (-draw [_ _ctx rect ^Canvas canvas]
    (let [aspect (/ (.getWidth image) (.getHeight image))
          rect'  (core/rect-xywh (:x rect) (:y rect) (:width rect) (/ (:width rect) aspect))]
      (.drawImageRect canvas image rect'))))

(defn image [src]
  (let [image (Image/makeFromEncoded (core/slurp-bytes src))]
    (map->AnImage {:image image})))
