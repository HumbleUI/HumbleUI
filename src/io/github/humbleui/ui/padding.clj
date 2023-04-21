(ns io.github.humbleui.ui.padding
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]))

(core/deftype+ Padding [left top right bottom]
  :extends core/AWrapper
  
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [left'      (core/dimension left cs ctx)
          right'     (core/dimension right cs ctx)
          top'       (core/dimension top cs ctx)
          bottom'    (core/dimension bottom cs ctx)
          child-cs   (core/ipoint (- (:width cs) left' right') (- (:height cs) top' bottom'))
          child-size (core/measure child ctx child-cs)]
      (core/ipoint
        (+ (:width child-size) left' right')
        (+ (:height child-size) top' bottom'))))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [left'      (core/dimension left rect ctx)
          right'     (core/dimension right rect ctx)
          top'       (core/dimension top rect ctx)
          bottom'    (core/dimension bottom rect ctx)
          width'     (- (:width rect) left' right')
          height'    (- (:height rect) top' bottom')
          child-rect (core/irect-xywh (+ (:x rect) left') (+ (:y rect) top') (max 0 width') (max 0 height'))]
      (core/draw-child child ctx child-rect canvas))))

(defn padding
  ([p child]
   (if (map? p)
     (map->Padding (assoc p :child child))
     (map->Padding
       {:left   p
        :top    p
        :right  p
        :bottom p
        :child  child})))
  ([w h child]
   (map->Padding
       {:left   w
        :top    h
        :right  w
        :bottom h
        :child  child}))
  ([l t r b child]
   (map->Padding
       {:left   l
        :top    t
        :right  r
        :bottom b
        :child  child})))
