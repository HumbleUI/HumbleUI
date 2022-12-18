(ns io.github.humbleui.ui.image
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [io.github.humbleui.skija Canvas Image]))

(core/deftype+ AnImage [^Image image]
  :extends core/ATerminal
  protocols/IComponent
  (-measure [_ _ctx _cs]
    (core/ipoint (.getWidth image) (.getHeight image)))
  
  (-draw [_ _ctx rect ^Canvas canvas]
    (.drawImageRect canvas image (core/rect rect))))

(defn image [src]
  (let [image (Image/makeFromEncoded (core/slurp-bytes src))]
    (map->AnImage {:image image})))
