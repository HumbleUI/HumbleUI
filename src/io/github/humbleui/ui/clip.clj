(ns io.github.humbleui.ui.clip
  (:require
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [io.github.humbleui.skija Canvas]))

(core/deftype+ Clip []
  :extends core/AWrapper
  
  protocols/IComponent  
  (-draw [_ ctx rect ^Canvas canvas]
    (canvas/with-canvas canvas
      (canvas/clip-rect canvas rect)
      (core/draw child ctx rect canvas))))

(defn clip [child]
  (map->Clip
    {:child child}))

(core/deftype+ ClipRRect [radii]
  :extends core/AWrapper
  
  protocols/IComponent  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [{:keys [scale]} ctx
          rrect  (core/rrect-complex-xywh (:x rect) (:y rect) (:width rect) (:height rect) (map #(* scale %) radii))]
      (canvas/with-canvas canvas
        (.clipRRect canvas rrect true)
        (core/draw child ctx rect canvas)))))

(defn clip-rrect [r child]
  (map->ClipRRect
    {:radii [r]
     :child child}))