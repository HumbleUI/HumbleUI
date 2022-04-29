(ns io.github.humbleui.ui.text-field
  (:require
    [clojure.java.io :as io]
    [clojure.math :as math]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.ui.dynamic :as dynamic])
  (:import
    [java.lang AutoCloseable]
    [java.io File]
    [io.github.humbleui.types IPoint IRect Point Rect RRect]
    [io.github.humbleui.skija Canvas Data Font FontMetrics Image Paint Surface TextLine]
    [io.github.humbleui.skija.shaper Shaper ShapingOptions]
    [io.github.humbleui.skija.svg SVGDOM SVGSVG SVGLength SVGPreserveAspectRatio SVGPreserveAspectRatioAlign SVGPreserveAspectRatioScale]))

(set! *warn-on-reflection* true)

(defmulti edit (fn [state command arg] command))

(defmethod edit :insert [{:keys [text to]} _ text']
  {:text (str (subs text 0 to) text' (subs text to))
   :from (+ to (count text'))
   :to   (+ to (count text'))})
  
(defmethod edit :replace [{:keys [text from to]} _ text']
  (let [left  (min from to)
        right (max from to)]
    {:text (str (subs text 0 left) text' (subs text right))
     :from (+ left (count text'))
     :to   (+ left (count text'))}))

(defmethod edit :move-left [{:keys [text from to]} _ _]
  (if (= from to)
    {:text text
     :from (max 0 (- to 1))
     :to   (max 0 (- to 1))}
    {:text text
     :from (min from to)
     :to   (min from to)}))

(defmethod edit :expand-left [{:keys [text from to]} _ _]
  {:text text
   :from from
   :to   (max 0 (- to 1))})

(defmethod edit :move-right [{:keys [text from to]} _ _]
  (if (= from to)
    {:text text
     :from (min (count text) (+ to 1))
     :to   (min (count text) (+ to 1))}
    {:text text
     :from (max from to)
     :to   (max from to)}))

(defmethod edit :expand-right [{:keys [text from to]} _ _]
  {:text text
   :from from
   :to   (min (count text) (+ to 1))})

(defmethod edit :move-beginning [{:keys [text from to]} _ _]
  {:text text
   :from 0
   :to   0})

(defmethod edit :expand-beginning [{:keys [text from to]} _ _]
  {:text text
   :from (if (= 0 from) 0 (max from to))
   :to   0})

(defmethod edit :move-end [{:keys [text from to]} _ _]
  {:text text
   :from (count text)
   :to   (count text)})

(defmethod edit :expand-end [{:keys [text from to]} _ _]
  {:text text
   :from (if (= (count text) from) (count text) (min from to))
   :to   (count text)})

(defmethod edit :delete-left [{:keys [text from to] :as state} _ _]
  (if (> to 0)
    {:text (str (subs text 0 (- to 1)) (subs text to))
     :from (- to 1)
     :to   (- to 1)}
    state))

(defmethod edit :delete-right [{:keys [text from to] :as state} _ _]
  (if (< to (count text))
    {:text (str (subs text 0 to) (subs text (+ to 1)))
     :from to
     :to   to}
    state))

(defmethod edit :kill [{:keys [text from to] :as state} _ _]
  {:text (str (subs text 0 (min from to)) (subs text (max from to)))
   :from (min from to)
   :to   (min from to)})

(defmethod edit :select-all [{:keys [text from to] :as state} _ _]
  {:text text
   :from 0
   :to   (count text)})

(core/deftype+ TextField [*state
                          ^Font font
                          ^FontMetrics metrics
                          ^ShapingOptions features
                          ^Paint fill-text
                          ^Paint fill-cursor
                          ^Paint fill-selection
                          ^String ^:mut line-text
                          ^TextLine ^:mut line]
  protocols/IComponent
  (-measure [_ ctx cs]
    (IPoint. (:width cs) (Math/ceil (.getCapHeight metrics))))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [{:keys [text from to]} @*state
          baseline (Math/ceil (.getCapHeight metrics))
          ascent   (- (+ baseline (Math/ceil (.getAscent metrics))))
          descent  (Math/ceil (.getDescent metrics))]
      (when (not= text line-text)
        (some-> line .close)
        (set! line-text text)
        (set! line (.shapeLine core/shaper text font features)))
      (canvas/with-canvas canvas
        (canvas/clip-rect canvas (Rect/makeXYWH (:x rect) (- (:y rect) ascent) (:width rect) (+ ascent baseline descent)))
        (canvas/draw-rect canvas
          (Rect/makeLTRB
            (+ (:x rect) (.getCoordAtOffset line (min from to)))
            (- (:y rect) ascent)
            (+ (:x rect) (.getCoordAtOffset line (max from to)) (:scale ctx))
            (+ (:y rect) baseline descent))
          (if (= from to)
            fill-cursor
            fill-selection))
        (.drawTextLine canvas line (:x rect) (+ (:y rect) baseline) fill-text))))
        
  (-event [_ event]
    (let [state @*state
          {:keys [text from to]} state]
      
      (cond 
        (= :text-input (:event event))
        (let [op (if (= from to) :insert :replace)
              state' (swap! *state edit op (:text event))]
          (not= op state))
      
        (and (= :key (:event event)) (:pressed? event))
        (let [key        (:key event)
              shift?     ((:modifiers event) :shift)
              super?     (if (= :macos app/platform)
                           ((:modifiers event) :mac-command)
                           ((:modifiers event) :control))
              selection? (not= from to)
              op         (cond
                           (and (= :left key)  (not shift?)) :move-left
                           (and (= :left key)  shift?)       :expand-left
                           (and (= :right key) (not shift?)) :move-right
                           (and (= :right key) shift?)       :expand-right
                           (and (= :up key)    (not shift?)) :move-beginning
                           (and (= :up key)    shift?)       :expand-beginning
                           (and (= :down key)  (not shift?)) :move-end
                           (and (= :down key)  shift?)       :expand-end
                           (and (= :a key) super?)           :select-all
                           (and (= :backspace key) (not selection?)) :delete-left
                           (and (= :delete key)    (not selection?)) :delete-right
                           (and (#{:backspace :delete} key) selection?) :kill)]
          (when op
            (not= state (swap! *state edit op nil)))))))
  
  AutoCloseable
  (close [_]
    #_(.close line))) ; TODO
  
(defn text-field
  ([*state]
   (text-field *state nil))
  ([*state opts]
   (dynamic/dynamic ctx [font           ^Font  (or (:font opts) (:font-ui ctx))
                         fill-text      ^Paint (or (:fill-text opts) (:fill-text ctx))
                         fill-cursor    ^Paint (or (:fill-cursor opts) (:fill-cursor ctx))
                         fill-selection ^Paint (or (:fill-selection opts) (:fill-selection ctx))]
     (let [features (reduce #(.withFeatures ^ShapingOptions %1 ^String %2) ShapingOptions/DEFAULT (:features opts))]
       (->TextField *state font (.getMetrics ^Font font) features fill-text fill-cursor fill-selection nil nil)))))

; (require 'examples.text-field :reload)
; (reset! user/*example "text-field")