(in-ns 'io.github.humbleui.ui)

(defn- draggable-child-bounds [draggable]
  (let [{:keys [my-pos child-pos child-size]} draggable]
    (util/irect-xywh
      (+ (:x my-pos) (:x child-pos))
      (+ (:y my-pos) (:y child-pos))
      (:width child-size)
      (:height child-size))))

(util/deftype+ Draggable [on-dragging
                          on-drop
                          my-pos
                          child-pos
                          child-size
                          mouse-start
                          dragged]
  :extends AWrapperNode
  
  (-measure-impl [_ _ctx cs]
    cs)
  
  (-draw-impl [this ctx bounds container-size viewport canvas]
    (set! my-pos (util/ipoint (:x bounds) (:y bounds)))
    (set! child-size (measure child ctx container-size))
    (draw child ctx (draggable-child-bounds this) container-size viewport canvas))
  
  (-event-impl [this ctx event]
    (when (and
            (= :mouse-button (:event event))
            (= :primary (:button event))
            (:pressed? event)
            (util/rect-contains? (draggable-child-bounds this) (util/ipoint (:x event) (:y event))))
      (set! mouse-start
        (util/ipoint
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
        (on-drop (util/ipoint
                   (+ (:x mouse-start) (:x event))
                   (+ (:y mouse-start) (:y event)))))
      (set! dragged false)
      (set! mouse-start nil))
    
    (util/eager-or
      (when (and
              (= :mouse-move (:event event))
              mouse-start)
        (let [p (util/ipoint
                  (+ (:x mouse-start) (:x event))
                  (+ (:y mouse-start) (:y event)))]
          (when on-dragging (on-dragging p))
          (set! dragged true)
          (set! child-pos p))
        true)
      (ui/event child ctx event)))
  
  (-reconcile-opts [_ _ new-element]
    (let [opts (parse-opts new-element)]
      (set! on-dragging (:on-dragging opts))
      (set! on-drop (:on-drop opts)))))

(defn draggable-ctor
  ([child]
   (draggable-ctor {} child))
  ([opts child]
   (let [scale (:scale *ctx*)]
     (map->Draggable
       {:child-pos (or (some-> ^IPoint (:pos opts) (.scale scale)) IPoint/ZERO)
        :dragged   false}))))
