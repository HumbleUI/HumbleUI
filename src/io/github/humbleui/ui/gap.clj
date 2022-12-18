(ns io.github.humbleui.ui.gap
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]))

(core/deftype+ Gap [width height]
  :extends core/ATerminal
  protocols/IComponent
  (-measure [_ ctx _cs]
    (let [{:keys [scale]} ctx]
      (core/ipoint (core/iceil (* scale width)) (core/iceil (* scale height))))))

(defn gap [width height]
  (map->Gap
    {:width  width
     :height height}))
