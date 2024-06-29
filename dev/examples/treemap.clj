(ns examples.treemap
  (:require
    [clojure.string :as str]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.window :as window]
    [io.github.humbleui.ui :as ui])
  (:import
    [java.io IOException]
    [java.nio.file Files FileVisitResult SimpleFileVisitor LinkOption Path]
    [java.nio.file.attribute BasicFileAttributes]
    [java.util.concurrent Future]
    [io.github.humbleui.types IPoint]
    [io.github.humbleui.skija Canvas Paint]))

(defonce *state
  (signal/signal nil))

(def *path
  (signal/signal
    {:text (System/getProperty "user.home")
     :placeholder "Path to scan"}))

(def *progress
  (signal/signal 0.0))

(def *future
  (signal/signal nil))

(def link-options
  (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))

(defn scan-impl [path *progress]
  (try
    (let [jpath (Path/of "/" (into-array String path))]
      (with-open [stream (Files/newDirectoryStream jpath)]
        (let [children (filterv #(or (Files/isDirectory % link-options) (Files/isRegularFile % link-options)) stream)
              children (->>
                         (for [^Path child children
                               :let [name (str (.getFileName child))]]
                           (cond
                             (Files/isDirectory child link-options)
                             (let [res (scan-impl (conj path name) nil)]
                               (when *progress
                                 (swap! *progress + (/ 1.0 (count children))))
                               res)
                     
                             (Files/isRegularFile child link-options)
                             {:name name
                              :size (Files/size child)}))
                         (remove nil?)
                         (sort-by #(- (:size %))))]
          {:name     (peek path)
           :size     (transduce (map :size) + 0 children)
           :children children})))
    (catch IOException e
      (println (.getMessage e))
      {:name (peek path)
       :size 0
       :children []})))

(defn scan [path *progress]
  (reset! *progress 0.0)
  (let [path (vec (str/split (:text @*path) #"/"))
        res (scan-impl path *progress)]
    (reset! *progress 1.0)
    res))

(comment
  (scan ["Users" "tonsky" "Documents"]))

(def gap
  4)

(def ^Paint fill
  (paint/fill 0xFF33CC33))

(defn set-color [^Canvas canvas full-size]
  (let [m    (.getLocalToDeviceAsMatrix33 canvas)
        dx   (-> (nth (.-_mat m) 2) (/ (:width full-size)))
        dy   (-> (nth (.-_mat m) 5) (/ (:height full-size)))
        color (-> 0xFF000000
                (bit-or (-> (* dx 255) int (max 0) (min 255)))
                (bit-or (-> (* dy 255) int (max 0) (min 255) (bit-shift-left 16)))
                (unchecked-int))]
    (.setColor fill color)))

(def stroke
  (paint/stroke 0xFFF6F6F6 2))

(defn aspect [w h]
  (cond
    (= h 0) Float/POSITIVE_INFINITY
    (= w 0) Float/POSITIVE_INFINITY
    :else   (float (max (/ w h) (/ h w)))))

(defn paint [paths ^Canvas canvas size full-size]
  (core/cond+
    :let [{:keys [width height]} size
          total-size (transduce (map :size) + 0 paths)]
    
    (<= total-size 0)
    :pass
    
    (<= width 0)
    :pass
    
    (<= height 0)
    :pass
    
    (< width height)
    (canvas/with-canvas canvas
      (canvas/translate canvas 0 height)
      (canvas/rotate canvas -90)
      (paint paths canvas (core/isize (:height size) (:width size)) full-size))
    
    (> (count paths) 1)
    (let [[left left-size]
          (loop [left 0
                 left-size 0
                 last-aspect Float/POSITIVE_INFINITY]
            (let [last         (nth paths left)
                  left-size'   (long (+ left-size (:size last)))
                  left-width'  (-> left-size' (/ total-size) (* width))
                  last-height  (-> (:size last) (/ left-size') (* height))
                  last-aspect' (double (aspect left-width' last-height))]
              (if (< last-aspect' last-aspect)
                (recur (inc left) left-size' last-aspect')
                [left left-size])))
          left-width (-> left-size (/ total-size) (* width) long)
          [left-paths right-paths] (split-at left paths)]
      (canvas/with-canvas canvas
        (let [*total-h (volatile! 0)]
          (doseq [path (butlast left-paths)
                  :let [h (-> (:size path) (/ left-size) (* height) long)]]
            (paint [path] canvas (core/isize left-width h) full-size)
            (canvas/translate canvas 0 h)
            (vswap! *total-h + h))
          (paint [(last left-paths)] canvas (core/isize left-width (- height @*total-h)) full-size)))
      (canvas/with-canvas canvas
        (canvas/translate canvas left-width 0)
        (paint right-paths canvas (core/isize (- width left-width) height) full-size)))

    :let [children (:children (first paths))]
    
    (nil? children)
    (let [rect (core/rect-xywh 0 0 width height)]
      (set-color canvas full-size)
      (canvas/draw-rect canvas rect fill)
      (when (and (> width 4) (> height 4))
        (canvas/draw-rect canvas rect stroke)))
    
    :else
    (let [rect (core/rect-xywh 0 0 width height)]
      (paint children canvas size full-size))))

(defn rescan []
  (when-some [f ^Future @*future]
    (.cancel f true))
  (reset! *future
    (future
      (reset! *state (scan (:text @*path) *progress))
      (reset! *future nil))))

(defn ui []
  (let [state    @*state
        progress @*progress]
    [ui/padding {:padding 10}
     [ui/column {:gap 10}
      [ui/row {:gap 10}
       ^{:stretch 1}
       [ui/text-field {:*state *path}]
       [ui/button {:on-click (fn [_] (rescan))} "Scan"]]
      
      (cond
        (= progress 0.0)
        ^{:stretch 1} [ui/gap]
         
        (< progress 1.0)
        ^{:stretch 1} 
        [ui/center
         [ui/width {:width #(max 200 (* 0.25 (:width %)))}
          [ui/column {:gap 10}
           [ui/row
            ^{:stretch progress}
            [ui/rect {:paint (paint/fill 0xFF33CC33)}
             [ui/gap {:height 10}]]
            ^{:stretch (- 1 progress)}
            [ui/rect {:paint (paint/fill 0xFFCCCCCC)}
             [ui/gap {:height 10}]]]
           [ui/label (str "Scanning " (-> progress (* 100) int (str "%")))]]]]
         
        :else
        ^{:stretch 1}
        [ui/image-snapshot
         [ui/canvas
          {:on-paint
           (fn [_ canvas size]
             (paint [state] canvas size size))}]])]]))
