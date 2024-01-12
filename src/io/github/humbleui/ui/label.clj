(in-ns 'io.github.humbleui.ui)

(core/deftype+ Label [size ^TextLine line ^Paint paint]
  :extends ATerminalNode
  protocols/IComponent
  (-measure-impl [this ctx cs]
    size)
  
  (-draw-impl [this ctx rect ^Canvas canvas]
    (.drawTextLine canvas line (:x rect) (+ (:y rect) (:height size)) paint))
  
  (-should-reconcile? [_this ctx new-element]
    (= element new-element))
  
  (-unmount-impl [this]
    (.close line)))

(defn label [& texts]
  (let [[_ opts texts] (parse-element (cons nil texts))
        paint          (or (:paint opts) (:fill-text *ctx*))
        font           (or (:font opts) (:font-ui *ctx*))
        features       (cond-> ShapingOptions/DEFAULT
                         (not (empty? (:font-features *ctx*)))
                         (.withFeatures (str/join " " (:font-features *ctx*)))
                         (not (empty? (:features opts)))
                         (.withFeatures (str/join " " (:features opts))))
        line           (.shapeLine shaper (str/join texts) font features)
        metrics        (.getMetrics font)
        size           (core/ipoint
                         (math/ceil (.getWidth line))
                         (math/ceil (.getCapHeight metrics)))]
    (map->Label 
      {:size  size
       :line  line
       :paint paint})))
