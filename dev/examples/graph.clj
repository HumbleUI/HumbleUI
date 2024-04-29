(ns examples.graph
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Canvas]
    [java.io File]))

(def ^File file
  #_(io/file "/ws/late-mate/host_and_shared/apple_terminal_dell.csv")
  (io/file "/ws/late-mate/host_and_shared/test.csv"))

(defn read-file [file]
  (->> (slurp file)
    (str/split-lines)
    (keep #(->> (str/split % #",") (keep parse-double) vec not-empty))
    vec
    not-empty))

(defn analyze [data]
  (let [min-t      (transduce (map first) min Double/MAX_VALUE data)
        ; data       (map (fn [[t v]] [(- t min-t) v]) data)
        levels     (remove #(neg? (second %)) data)
        events     (filter #(neg? (second %)) data)
        max-v      (transduce (map second) max 0 levels)
        min-v      (transduce (map second) min 1 levels)
        high-level (- max-v (* 0.25 (- max-v min-v)))
        low-level  (+ min-v (* 0.25 (- max-v min-v)))
        level-fn   #(cond
                      (> % high-level) :high
                      (< % low-level)  :low
                      :else            nil)
        delays     (loop [res        (transient [])
                          data       data
                          level      nil
                          event-time nil]
                     (core/cond+
                       (empty? data)
                       (persistent! res)
                       
                       :let [[t v] (first data)]
                       
                       (neg? v)
                       (recur res (next data) nil t)
                       
                       (nil? event-time)
                       (recur res (next data) level event-time)
                       
                       (nil? level)
                       (recur res (next data) (level-fn v) event-time)
                       
                       :let [level' (level-fn v)]
                       
                       (and (not= level level') (= level' :high))
                       (recur (conj! res [event-time t (- t event-time)]) (next data) nil nil)
                       
                       (not= level level')
                       (recur res (next data) nil nil)
                       
                       :else
                       (recur res (next data) level event-time)))]
    delays))

(comment
  (def data (read-file file))
  (analyze data))

(defn update-file [data]
  (let [last-modified (:last-modified data 0)]
    (cond
      (not (.exists file))
      {}
      
      (<= (.lastModified file) last-modified)
      data
    
      :else
      (let [data (read-file file)]
        {:data          data
         :delays        (analyze data)
         :last-modified (.lastModified file)}))))

(defn use-timer []
  (let [*state (signal/signal {})
        cancel (core/schedule
                 #(signal/swap! *state update-file) 0 1000)]
    {:value *state
     :after-unmount cancel}))

(def paint-graph
  (paint/stroke 0xFF0080FF 2))

(def paint-scatter
  (paint/fill 0x400080FF))

(def paint-line
  (paint/stroke 0x40000000 2))

(def paint-gray
  (paint/fill 0x80000000))

(defn on-paint [data ctx ^Canvas canvas size]
  (let [{:keys [font-ui fill-text leading scale]} ctx
        {:keys [width height]} size
        {:keys [data delays]} data
        levels (remove #(neg? (second %)) data)
        min-t  (transduce (map first) min Double/MAX_VALUE levels)
        max-t  (transduce (map first) max 0 levels)
        max-v  (transduce (map second) max 0 levels)
        min-v  (transduce (map second) min 1 levels)
        dist   (- max-v min-v)
        max-v  (min 1 (+ max-v (* 0.1 dist)))
        min-v  (max 0 (- min-v (* 0.1 dist)))
        t->x   #(-> % (- min-t) (/ (- max-t min-t)) (* width))
        v->y   #(-> % #_(- min-v) #_(/ (- max-v min-v)) (* -1) (+ 1) (* height))
        pts    (mapv (fn [[t v]] (core/point (t->x t) (v->y v))) levels)
        _      (canvas/draw-polygon canvas pts paint-graph)
        keys   (filter #(neg? (second %)) data)
        _      (doseq [[t _] keys]
                 (canvas/draw-line canvas (t->x t) (v->y min-v) (t->x t) (v->y max-v) paint-line))
        
        _      (doseq [[[event-t switch-t delay] i] (core/zip delays (range))
                       :let [x (-> i (/ (count delays)) (* 40) (+ (* 0.5 width)))
                             y (-> (- 100 delay) (/ 100) (* 0.5 height))]]
                 ; (canvas/draw-line canvas (t->x event-t) y (t->x switch-t) y paint-graph)
                 (canvas/draw-circle canvas x y 5 paint-scatter))
        _      (doseq [delay (range 0 100 10)
                       :let [x0 (-> (+ -20 (* 0.5 width)))
                             x1 (-> (+ 60 (* 0.5 width)))
                             y (-> (- 100 delay) (/ 100) (* 0.5 height))]]
                 (canvas/draw-line canvas x0 y x1 y paint-line)
                 (canvas/draw-string canvas (str delay " ms") (+ x1 20) (+ y 10) font-ui paint-gray))
        
        _      (doseq [t (range min-t max-t 10)
                       :let [x (t->x t)]]
                 (canvas/draw-line canvas x (- height 20) x height paint-gray))
        
        _      (canvas/draw-rect canvas (core/rect-xywh 0 0 width height) paint-line)]
    
    
    
    ; (canvas/draw-string
    ;   canvas
    ;   (:last-modified data)
    ;   (/ width 2)
    ;   (/ height 2)
    ;   font-ui fill-text)
    ))

(ui/defcomp ui []
  (ui/with [*data (use-timer)]
    (fn []
      [ui/padding {:padding 10}
       [ui/canvas
        {:on-paint (partial on-paint @*data)}]])))
