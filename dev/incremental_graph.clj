(ns incremental-graph
  (:require
    [clojure.math :as math]
    [clojure.string :as str]
    [incremental :as i]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.signal :as s]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.typeface :as typeface]
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.window :as window]
    [state])
  (:import
    [io.github.humbleui.types Point IRect]
    [io.github.humbleui.skija Canvas Color Paint Path TextLine]
    [io.github.humbleui.skija.shaper ShapingOptions]
    [java.util HashMap Map WeakHashMap]))

(declare window)

(def *frame
  (atom 0))

(core/deftype+ Node [name line mass ^Point ^:mut pos ^Point ^:mut force ^:mut frame ^:mut last-value]
  Object
  (toString [_]
    (str "#Node[name=" name " pos=" (:x pos) "," (:y pos) " force=" (:x force) "," (:y force) "]")))

(defmethod print-method Node [o, ^java.io.Writer w]
  (.write w (str o)))

(defn randf [from to]
  (+ from (rand-int (- to from))))

(defn make-node [s]
  (let [name  (str (:name s))
        line  (.shapeLine core/shaper name @i/*font-ui ShapingOptions/DEFAULT)
        mass  5
        pos   (core/point (randf 0 1600) (randf 0 1200))
        force (core/point 0 0)]
    (->Node name line mass pos force @*frame nil)))

(def *nodes
  (atom (WeakHashMap.)))

(def show-effects?
  false)

(defn sources []
  (concat
    [#_i/*scale
     #_i/*face-ui
     #_i/*color-text
     #_i/*color-button
     #_i/*padding
     i/*filter
     i/*todos]
    @i/*todos))

(defn collect-signals [acc s]
  (if (and (not show-effects?) (= :eager (:type s)))
    acc
    (reduce
      collect-signals
      (conj! acc s)
      (keep s/read-ref (:outputs s)))))

(defn update-nodes [nodes]
  (let [signals' (persistent! (reduce collect-signals (transient #{}) (sources)))]
    (reduce
      (fn [^Map m s]
        (let [node (or (.get ^Map nodes s) (make-node s))
              value (:value s)]
          (when (not= value (:last-value node))
            (protocols/-set! node :last-value value)
            (protocols/-set! node :frame @*frame))
          (.put m s node)
          m))
      (WeakHashMap.) signals')))

(defn collect-edges 
  ([nodes]
   (reduce #(collect-edges nodes %1 %2) {} (sources)))
  ([^Map nodes acc s]
   (cond
     (and (not show-effects?) (= :eager (:type s)))
     acc
     
     (nil? (.get nodes s))
     acc
     
     :else
     (let [outs (cond->> (:outputs s)
                  true (keep s/read-ref)
                  (not show-effects?) (remove #(= :eager (:type %)))
                  true (filter #(.containsKey nodes %)))]
       (reduce
         #(collect-edges nodes %1 %2)
         (assoc acc s outs)
         outs)))))

(def *focus?
  (atom true))

(def fill-text
  (paint/fill 0xFF000000))

(def ^Paint fill-node-new
  (paint/fill 0xFFF9D404))

(def fill-node
  (paint/fill 0xFFEEEEEE))

(def stroke-edge
  (paint/stroke 0x40000000 2))

(def stroke-fill
  (paint/stroke 0x10000000 2))

(def fill-edge
  (paint/fill 0x40000000))

(def g
  0.3)

(def repulsion-x
  10000)

(def repulsion-y
  10000)

(def strings
  0.1)

(defn apply-forces! [nodes ^Point center]
  ;; gravity
  (doseq [[_ n] nodes]
    (let [gf (-> ^Point (:pos n) (.offset (.inverse center)) (.inverse) (.scale g))]
      (protocols/-set! n :force gf)))
  ;; repulsion
  (let [nodes (vec (vals nodes))]
    (doseq [i (range 0 (dec (count nodes)))
            j (range (inc i) (count nodes))]
      (let [from  ^Node (nth nodes i)
            to    ^Node (nth nodes j)
            dir   ^Point (-> ^Point (:pos from) (.offset (.inverse ^Point (:pos to))))
            len   (math/sqrt (+ (* (:x dir) (:x dir)) (* (:y dir) (:y dir))))
            force ^Point (-> ^Point dir (.scale (/ repulsion-x len len) (/ repulsion-y len len)))]
        (protocols/-update! from :force #(.offset ^Point % force))
        (protocols/-update! to :force #(.offset ^Point % (.inverse force))))))
  ;; edges
  (doseq [[from tos] (collect-edges nodes)
          to tos]
    (let [from  ^Node (get nodes from)
          to    ^Node (get nodes to)
          dir   ^Point (-> ^Point (:pos from) (.offset (.inverse ^Point (:pos to))))
          force ^Point (-> ^Point dir (.scale strings))]
      (protocols/-update! from :force #(.offset ^Point % (.inverse force)))
      (protocols/-update! to :force #(.offset ^Point % force))))
  ;; apply forces
  (doseq [[_ n] nodes]
    (let [v    (-> ^Point (:force n) (.scale (/ 1 (:mass n))))
          pos' (-> ^Point (:pos n) (.offset v))]
      (protocols/-set! n :pos pos'))))

(def ^Path arrow
  (doto (Path.)
    (.moveTo 0 0)
    (.lineTo 20 -10)
    ; (.lineTo 15 0)
    (.lineTo 20 10)
    (.closePath)))

(defn draw-arrow [^Canvas canvas ^Point from ^Point to]
  (canvas/with-canvas canvas
    (canvas/translate canvas (:x to) (:y to))
    (let [dx (- (:x from) (:x to))
          dy (- (:y from) (:y to))
          len (math/hypot dx dy)
          angle (math/to-degrees (math/atan2 dy dx))]
      (canvas/rotate canvas angle)
      (canvas/translate canvas 40 0)
      (.drawPath canvas arrow fill-edge)
      (canvas/draw-line canvas 20 0 (- len 80) 0 stroke-edge))))

(defn substring [s]
  (if (> (count s) 25)
    (str (subs s 0 25) "...")
    s))

(defn on-paint [ctx ^Canvas canvas size]
  (let [{:keys [font-ui scale]} ctx
        {:keys [width height]} size
        nodes (swap! *nodes update-nodes)]
    (apply-forces! nodes (core/point (/ width 2) (/ height 2)))
    (canvas/clear canvas 0xFFFFFFFF)
        
    ; draw nodes
    (doseq [[_ n] nodes]
      (let [{^TextLine line :line
             {x :x y :y} :pos
             frame :frame
             val   :last-value} n]
        (with-open [val-line (.shapeLine core/shaper (substring (str val)) @i/*font-ui ShapingOptions/DEFAULT)]
          (let [w    (-> (.getWidth line) (max (.getWidth val-line)) (/ 2.0) math/ceil (* 2))
                h    (-> @i/*font-ui-cap-height (/ 2.0) math/ceil (* 2))
                pad  (* 5 scale)
                left (math/round (- x (/ w 2)))
                top  (math/round (- y (* h 1.5)))
                rate (/ (- @*frame frame) 120)
                fill (if (<= 0 rate 1)
                       (doto fill-node-new
                         (.setColor (Color/makeLerp (unchecked-int 0xFFEEEEEE) (unchecked-int 0xFFF9D404) rate)))
                       fill-node)]
            (canvas/draw-rect canvas (core/rect-xywh (- left pad) (- top pad) (+ w pad pad) (+ (* h 3) pad pad)) fill)
            (canvas/draw-rect canvas (core/rect-xywh (- left pad) (- top pad) (+ w pad pad) (+ (* h 3) pad pad)) stroke-fill)
            (.drawTextLine canvas line left (+ top h) fill-text)
            (.drawTextLine canvas val-line left (+ top (* 3 h)) fill-text)))))
    
    ; draw edges
    (doseq [[from tos] (collect-edges nodes)
            to tos
            :let [from (get nodes from)
                  to   (get nodes to)]]
      (draw-arrow canvas (:pos from) (:pos to)))
    
    (when (and (bound? #'window) #_@*focus?)
      (window/request-frame window))
    
    (swap! *frame inc)))

(def app
  (ui/canvas
    {:on-paint #'on-paint
     :on-event (fn [_ e]
                 (case (:event e)
                   :window-focus-in (reset! *focus? true)
                   :window-focus-out (reset! *focus? false)
                   nil))}))

; (defonce window
;   (app/doui
;     (let [screen (last (app/screens))
;           scale  (:scale screen)
;           {:keys [width height]} (:work-area screen)]
;       (ui/window
;         {:title  "Visualizer"
;          :screen (:id screen)
;          :width  (/ width scale)
;          :height (/ height scale)
;          :exit-on-close? false}
;         #'app))))