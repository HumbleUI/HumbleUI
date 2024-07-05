(in-ns 'io.github.humbleui.ui)

(core/deftype+ Label [size ^TextLine line ^Paint paint ^Font font features]
  :extends ATerminalNode
  protocols/IComponent
  (-measure-impl [this ctx cs]
    size)
  
  (-draw-impl [this ctx rect ^Canvas canvas]
    (.drawTextLine canvas line (:x rect) (+ (:y rect) (:height size)) paint))
  
  (-should-reconcile? [_this ctx new-element]
    (and
      (= element new-element)
      (let [[_ opts' _] (parse-element new-element)]
        (and
          (identical? font (get-font opts'))
          (identical? paint (or (:paint opts') (:fill-text ctx)))
          (= features (set (concat (:font-features ctx) (:font-features opts'))))))))
  
  (-unmount-impl [this]
    (.close line)))

(defn- label-impl [& texts]
  (let [[_ opts texts] (parse-element (cons nil texts))
        paint          (or (:paint opts) (:fill-text *ctx*))
        font           (get-font opts)
        features       (cond-> ShapingOptions/DEFAULT
                         (not (empty? (:font-features *ctx*)))
                         (.withFeatures (str/join " " (:font-features *ctx*)))
                         (not (empty? (:font-features opts)))
                         (.withFeatures (str/join " " (:font-features opts))))
        line           (.shapeLine shaper (str/join texts) font features)
        metrics        (.getMetrics font)
        size           (core/ipoint
                         (math/ceil (.getWidth line))
                         (math/ceil (.getCapHeight metrics)))]
    (map->Label 
      {:size     size
       :line     line
       :paint    paint
       :font     font
       :features features})))

(defn- label-ctor [& texts]
  (vec
    (cons label-impl
      (map signal/maybe-read texts))))
