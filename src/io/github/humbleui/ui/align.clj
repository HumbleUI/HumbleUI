(in-ns 'io.github.humbleui.ui)

(core/deftype+ Align []
  :extends AWrapperNode  
  protocols/IComponent  
  (-draw-impl [_ ctx rect canvas]
    (let [[_ opts _]     element
          {:keys [x y child-x child-y]} opts
          _              (assert (or (nil? x) (number? x) (#{:left :center :right} x)) (str ":x, expected number or :left/:center/:right, got: " (pr-str x)))
          _              (assert (or (nil? child-x) (number? child-x) (#{:left :center :right} child-x)) (str ":child-x, expected number or :left/:center/:right, got: " (pr-str child-x)))
          _              (assert (or (nil? y) (number? y) (#{:top :center :bottom} y)) (str ":y, expected number or :top/:center/:bottom, got: " (pr-str y)))
          _              (assert (or (nil? child-y) (number? child-y) (#{:top :center :bottom} child-y)) (str ":child-y, expected number or :top/:center/:bottom, got: " (pr-str child-y)))
          _              (assert (or x y) (str "Expected one of: :x, :y, got:" (pr-str opts)))
          x              (condp = x
                           :left 0
                           :center 0.5
                           :right 1
                           x)
          y              (condp = y
                           :top 0
                           :center 0.5
                           :bottom 1
                           y)
          child-x        (or child-x x)
          child-y        (or child-y y)
          child-size     (measure child ctx (core/ipoint (:width rect) (:height rect)))
          left           (when x
                           (+ (:x rect)
                             (* (:width rect) x)
                             (- (* (:width child-size) child-x))))
          top            (when y
                           (+ (:y rect)
                             (* (:height rect) y)
                             (- (* (:height child-size) child-y))))
          child-rect     (cond
                           (and x y)
                           (core/irect-xywh left top (:width child-size) (:height child-size))
                           
                           x
                           (core/irect-xywh left (:y rect) (:width child-size) (:height rect))
                           
                           y
                           (core/irect-xywh (:x rect) top (:width rect) (:height child-size)))]
      (draw-child child ctx child-rect canvas))))

(defn- align-ctor [opts child]
  (map->Align {}))

(defn- center-ctor [child]
  [align {:x 0.5 :y 0.5}
   child])