(ns io.github.humbleui.ui.scroll
  (:require
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]))

(core/deftype+ VScroll [^:mut offset ^:mut self-rect ^:mut child-size]
  :extends core/AWrapper
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [child-cs   (assoc cs :height Integer/MAX_VALUE)]
      (set! child-size (protocols/-measure child ctx child-cs))
      (core/ipoint
        (:width child-size)
        (min
          (:height child-size)
          (:height cs)))))
  
  (-draw [_ ctx rect canvas]
    (set! child-size (protocols/-measure child ctx (core/ipoint (:width rect) Integer/MAX_VALUE)))
    (set! self-rect rect)
    (set! offset (core/clamp offset (- (:height rect) (:height child-size)) 0))
    (canvas/with-canvas canvas
      (canvas/clip-rect canvas rect)
      (let [child-rect (-> rect
                         (update :y + offset)
                         (assoc :height Integer/MAX_VALUE))]
        (core/draw child ctx child-rect canvas))))
  
  (-event [_ ctx event]
    (cond
      (= :mouse-scroll (:event event))
      (when (core/rect-contains? self-rect (core/ipoint (:x event) (:y event)))
        (or
          (core/event-child child ctx event)
          (let [offset' (-> offset
                          (+ (:delta-y event))
                          (core/clamp (- (:height self-rect) (:height child-size)) 0))]
            (when (not= offset offset')
              (set! offset offset')
              true))))
      
      (= :mouse-button (:event event))
      (when (core/rect-contains? self-rect (core/ipoint (:x event) (:y event)))
        (core/event-child child ctx event))
      
      :else
      (core/event-child child ctx event)))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  java.lang.AutoCloseable
  (close [_]
    (core/child-close child)))

(defn vscroll [child]
  (map->VScroll
    {:child child
     :offset 0}))

(core/deftype+ VScrollbar [fill-track fill-thumb]
  :extends core/AWrapper
  
  protocols/IComponent  
  (-draw [_ ctx rect ^Canvas canvas]
    (core/draw-child child ctx rect canvas)
    (when (> (:height (:child-size child)) (:height rect))
      (let [{:keys [scale]} ctx
            content-y (- (:offset child))
            content-h (:height (:child-size child))
            scroll-y  (:y rect)
            scroll-h  (:height rect)
            
            padding (* 4 scale)
            track-w (* 4 scale)
            track-x (+ (:x rect) (:width rect) (- track-w) (- padding))
            track-y (+ scroll-y padding)
            track-h (- scroll-h (* 2 padding))
            track   (core/rrect-xywh track-x track-y track-w track-h (* 2 scale))
            
            thumb-w       (* 4 scale)
            min-thumb-h   (* 16 scale)
            thumb-y-ratio (/ content-y content-h)
            thumb-y       (-> (* track-h thumb-y-ratio) (core/clamp 0 (- track-h min-thumb-h)) (+ track-y))
            thumb-b-ratio (/ (+ content-y scroll-h) content-h)
            thumb-b       (-> (* track-h thumb-b-ratio) (core/clamp min-thumb-h track-h) (+ track-y))
            thumb         (core/rrect-ltrb track-x thumb-y (+ track-x thumb-w) thumb-b (* 2 scale))]
        (canvas/draw-rect canvas track fill-track)
        (canvas/draw-rect canvas thumb fill-thumb)))))

(defn vscrollbar [child]
  (map->VScrollbar
    {:child      (vscroll child)
     :fill-track (paint/fill 0x10000000)
     :fill-thumb (paint/fill 0x60000000)}))
