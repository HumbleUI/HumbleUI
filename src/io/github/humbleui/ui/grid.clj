(ns io.github.humbleui.ui.grid
  (:require
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]))

(defn- measure [rows cols children cs ctx]
  (core/loopr [heights (vec (repeat rows 0))
               widths  (vec (repeat cols 0))
               row     0
               col     0]
    [child children]
    (let [[row' col'] (if (< col (dec cols))
                        [row (inc col)]
                        [(inc row) 0])]
      (if child
        (let [size (core/measure child ctx cs)]
          (recur
            (update heights row max (:height size))
            (update widths  col max (:width size))
            row' col'))
        (recur heights widths row' col')))
    {:widths  widths
     :heights heights}))

(core/deftype+ Grid [rows cols]
  :extends core/AContainer
  
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [{:keys [widths heights]} (measure rows cols children cs ctx)]
      (core/ipoint
        (reduce + widths)
        (reduce + heights))))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [cs (core/ipoint (:width rect) (:height rect))
          {:keys [widths heights]} (measure rows cols children cs ctx)]
      (core/loopr [x (:x rect)
                   y (:y rect)]
        [row (range rows)
         col (range cols)]
        (let [height (nth heights row)
              width  (nth widths col)]
          (when-some [child (nth children (+ col (* row cols)))]
            (let [child-rect (core/irect-xywh x y width height)]
              (core/draw-child child ctx child-rect canvas)))
          (let [[x' y'] (if (< col (dec cols))
                          [(+ x width) y]
                          [(:x rect) (+ y height)])]
            (recur x' y')))))))

(defn- right-pad [n val xs]
  (concat
    xs
    (repeat (- n (count xs)) val)))

(defn grid [rows]
  (let [cols     (reduce max 0 (map count rows))
        children (for [row   rows
                       child (right-pad cols nil row)]
                   child)]
    (map->Grid
      {:rows     (count rows)
       :cols     cols
       :children children})))
