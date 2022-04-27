(ns io.github.humbleui.ui.text-field
  (:require
    [clojure.java.io :as io]
    [clojure.math :as math]
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

(core/deftype+ TextField [*state
                          ^Font font
                          ^FontMetrics metrics
                          ^ShapingOptions features
                          ^Paint paint
                          ^:mut sel-from
                          ^:mut sel-to
                          ^String ^:mut text
                          ^TextLine ^:mut line]
  protocols/IComponent
  (-measure [_ ctx cs]
    (IPoint. (:width cs) (Math/ceil (.getCapHeight metrics))))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [state @*state]
      (when (not= text state)
        (some-> line .close)
        (set! text state)
        (set! line (.shapeLine core/shaper text font features))))
    (let [baseline (Math/ceil (.getCapHeight metrics))
          ascent   (Math/ceil (.getAscent metrics))
          descent  (Math/ceil (.getDescent metrics))]
      (.drawTextLine canvas line (:x rect) (+ (:y rect) baseline) paint)
      (canvas/draw-rect canvas
        (Rect/makeLTRB
          (+ (:x rect) (.getCoordAtOffset line sel-from))
          (+ (:y rect) baseline ascent)
          (+ (:x rect) (.getCoordAtOffset line sel-to) (:scale ctx))
          (+ (:y rect) baseline descent))
        paint)))
  
  (-event [_ event])
  
  AutoCloseable
  (close [_]
    #_(.close line))) ; TODO

(defn text-field
  ([*state]
   (text-field *state nil))
  ([*state opts]
   (dynamic/dynamic ctx [font ^Font (or (:font opts) (:font-ui ctx))
                         paint ^Paint (or (:paint opts) (:fill-text ctx))]
     (let [features (reduce #(.withFeatures ^ShapingOptions %1 ^String %2) ShapingOptions/DEFAULT (:features opts))]
       (->TextField *state font (.getMetrics ^Font font) features paint 0 0 nil nil)))))
