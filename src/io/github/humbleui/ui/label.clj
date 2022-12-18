(ns io.github.humbleui.ui.label
  (:require
    [clojure.math :as math]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.ui.dynamic :as dynamic])
  (:import
    [io.github.humbleui.skija Canvas Font FontMetrics Paint TextLine]
    [io.github.humbleui.skija.shaper ShapingOptions]))

(core/deftype+ Label [^Paint paint ^TextLine line ^FontMetrics metrics]
  :extends core/ATerminal
  protocols/IComponent
  (-measure [_ _ctx _cs]
    (core/ipoint
      (Math/ceil (.getWidth line))
      (Math/ceil (.getCapHeight metrics))))
  
  (-draw [_ _ctx rect ^Canvas canvas]
    (.drawTextLine canvas line (:x rect) (+ (:y rect) (math/ceil (.getCapHeight metrics))) paint)))

(defn label
  ([text]
   (label nil text))
  ([opts text]
   (dynamic/dynamic ctx [^Font font (or (:font opts) (:font-ui ctx))
                         paint (or (:paint opts) (:fill-text ctx))]
     (let [text     (str text)
           features (reduce #(.withFeatures ^ShapingOptions %1 ^String %2) ShapingOptions/DEFAULT (:features opts))
           line     (.shapeLine core/shaper text font ^ShapingOptions features)]
       (map->Label
         {:paint   paint
          :line    line
          :metrics (.getMetrics font)})))))
