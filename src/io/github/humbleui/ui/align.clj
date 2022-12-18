(ns io.github.humbleui.ui.align
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]))

(core/deftype+ HAlign [child-coeff coeff]
  :extends core/AWrapper
  
  protocols/IComponent  
  (-draw [_ ctx rect canvas]
    (let [child-size (core/measure child ctx (core/ipoint (:width rect) (:height rect)))
          left       (+ (:x rect)
                       (* (:width rect) coeff)
                       (- (* (:width child-size) child-coeff)))
          child-rect (core/irect-xywh left (:y rect) (:width child-size) (:height rect))]
      (core/draw-child child ctx child-rect canvas))))

(defn halign
  ([coeff child]
   (halign coeff coeff child))
  ([child-coeff coeff child]
   (map->HAlign
     {:child-coeff child-coeff
      :coeff coeff
      :child child})))

(core/deftype+ VAlign [child-coeff coeff]
  :extends core/AWrapper
  
  protocols/IComponent  
  (-draw [_ ctx rect canvas]
    (let [child-size (core/measure child ctx (core/ipoint (:width rect) (:height rect)))
          top        (+ (:y rect)
                       (* (:height rect) coeff)
                       (- (* (:height child-size) child-coeff)))
          child-rect (core/irect-xywh (:x rect) top (:width rect) (:height child-size))]
      (core/draw-child child ctx child-rect canvas))))

(defn valign
  ([coeff child]
   (valign coeff coeff child))
  ([child-coeff coeff child]
   (map->VAlign
     {:child-coeff child-coeff
      :coeff coeff
      :child child})))

(defn center [child]
  (halign 0.5
    (valign 0.5
      child)))
