(ns examples.canvas
  (:require
    [clojure.math :as math]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.types IPoint]
    [io.github.humbleui.skija Canvas PaintStrokeCap PaintStrokeJoin Path]))

(def *last-event
  (atom nil))

(def *paths
  (atom []))

(def *events
  (atom clojure.lang.PersistentQueue/EMPTY))

(def queue-size
  300)

(defn log-event [e]
  (swap! *events #(conj (if (> (count %) queue-size) (pop %) %) (assoc e :time (System/nanoTime)))))

(defn on-event [_ctx e]
  (reset! *last-event e)
    
  (when (= :mouse-move (:event e))
    (log-event e))
    
  (when (and
          (= :mouse-button (:event e))
          (= :primary (:button e))
          (= true (:pressed? e)))
    (swap! *paths conj (doto (Path.) (.moveTo (:x e) (:y e)))))
    
  (when (and
          (= :mouse-move (:event e))
          (= #{:primary} (:buttons e)))
    (swap! *paths util/update-last #(.lineTo ^Path % (:x e) (:y e))))
  
  true)

(defn on-paint [ctx ^Canvas canvas ^IPoint size]
  (log-event {:event :frame})
  
  (let [font-ui (ui/get-font nil ctx)
        {:keys [fill-text scale]} ctx
        {:keys [width height]} size]
    
    ;; paths
    (with-open [paint (doto (paint/stroke 0x80000000 (* scale 5))
                        (.setStrokeJoin PaintStrokeJoin/ROUND)
                        (.setStrokeCap PaintStrokeCap/ROUND))]
      (doseq [path @*paths]
        (.drawPath canvas path paint)))
    
    ;; mouse
    (let [events       @*events
          mouse-events (filter #(= :mouse-move (:event %)) events)]
      ;; current position
      (util/when-some+ [[e] (take-last 1 mouse-events)]
        (with-open [paint (paint/fill 0xFFCC3333)]
          (canvas/draw-rect canvas (util/rect-xywh (- (:x e) (* 3 scale)) (- (:y e) (* 3 scale)) (* 6 scale) (* 6 scale)) paint)))
      
      ;; projection
      (util/when-some+ [[e2 e] (take-last 2 mouse-events)]
        (let [r (math/hypot (- (:x e) (:x e2)) (- (:y e) (:y e2)))]
          (with-open [paint (paint/stroke 0xFFCC3333 (* scale 2))]
            (canvas/draw-circle canvas (:x e) (:y e) r paint))))
      
      ;; event graph
      (let [point
            (fn [idx dt]
              (util/point
                (-> width float (/ queue-size) (* idx))
                (- height (-> dt (/ 1000000.0) (* 2 scale)))))
            
            [after-mouse after-frame]
            (util/loopr [after-mouse (transient [])
                         after-frame (transient [])
                         last-mouse  nil
                         last-frame  nil
                         idx         0]
              [e events]
              (case (:event e)
                :frame
                (recur after-mouse after-frame last-mouse e idx)
                         
                :mouse-move
                (recur
                  (cond-> after-mouse
                    last-mouse (conj! (point idx (- (:time e) (:time last-mouse)))))
                  (cond-> after-frame
                    last-frame (conj! (point idx (- (:time e) (:time last-frame)))))
                  e
                  last-frame
                  (inc idx)))
              [(persistent! after-mouse)
               (persistent! after-frame)])]
        (with-open [paint (paint/stroke 0xFFDDDDDD (* scale 1))]
          (doseq [dt (range 0 34000000 (/ 1000000000 120))]
            (canvas/draw-line canvas (point 0 dt) (point queue-size dt) paint)))

        (with-open [paint (paint/stroke 0xFF808080 (* scale 1))]
          (canvas/draw-polygon canvas after-frame paint))
        (with-open [paint (paint/stroke 0xFF8080FF (* scale 1))]
          (canvas/draw-polygon canvas after-mouse paint))))

    ;; event
    (let [event @*last-event]
      (loop [y  (+ 10 (* 20 scale))
             kv (sort-by first event)]
        (when-some [[k v] (first kv)]
          (canvas/draw-string canvas (str k " " v) (* 10 scale) y font-ui fill-text)
          (recur (+ y (* 20 scale)) (next kv)))))))

(ui/defcomp ui []
  [ui/stack
   [ui/canvas
    {:on-paint on-paint
     :on-event on-event}]
   [ui/padding {:padding 10}
    [ui/align {:x :right :y :top}
     [ui/button {:on-click (fn [_] (reset! *paths []))}
      "Clear"]]]])
