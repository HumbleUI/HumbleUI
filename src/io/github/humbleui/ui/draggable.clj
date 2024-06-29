(in-ns 'io.github.humbleui.ui)

(defn- draggable-child-rect [draggable]
  (let [{:keys [my-pos child-pos child-size]} draggable]
    (core/irect-xywh
      (+ (:x my-pos) (:x child-pos))
      (+ (:y my-pos) (:y child-pos))
      (:width child-size)
      (:height child-size))))

(core/deftype+ Draggable [^:mut my-pos
                          ^:mut child-pos
                          ^:mut child-size
                          ^:mut mouse-start
                          ^:mut dragged]
  :extends AWrapperNode
  protocols/IComponent
  (-measure-impl [_ _ctx cs]
    cs)
  
  (-draw-impl [this ctx rect canvas]
    (set! my-pos (core/ipoint (:x rect) (:y rect)))
    (set! child-size (measure child ctx (core/ipoint (:width rect) (:height rect))))
    (draw-child child ctx (draggable-child-rect this) canvas))
  
  (-event-impl [this ctx event]
    (let [[_ opts _] (parse-element element)
          {:keys [on-dragging on-drop]} opts]
      (when (and
              (= :mouse-button (:event event))
              (= :primary (:button event))
              (:pressed? event)
              (core/rect-contains? (draggable-child-rect this) (core/ipoint (:x event) (:y event))))
        (set! mouse-start
          (core/ipoint
            (- (:x child-pos) (:x event))
            (- (:y child-pos) (:y event)))))
    
      (when (and
              (= :mouse-button (:event event))
              (= :primary (:button event))
              (not (:pressed? event)))
        (when (and
                on-drop
                mouse-start
                dragged)
          (on-drop (core/ipoint
                     (+ (:x mouse-start) (:x event))
                     (+ (:y mouse-start) (:y event)))))
        (set! dragged false)
        (set! mouse-start nil))
    
      (core/eager-or
        (when (and
                (= :mouse-move (:event event))
                mouse-start)
          (let [p (core/ipoint
                    (+ (:x mouse-start) (:x event))
                    (+ (:y mouse-start) (:y event)))]
            (when on-dragging (on-dragging p))
            (set! dragged true)
            (set! child-pos p))
          true)
        (event-child child ctx event)))))

(defn draggable-ctor
  ([child]
   (draggable-ctor {} child))
  ([opts child]
   (let [scale (:scale *ctx*)]
     (map->Draggable
       {:child-pos (or (some-> ^IPoint (:pos opts) (.scale scale)) IPoint/ZERO)
        :dragged   false}))))
