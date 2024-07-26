(in-ns 'io.github.humbleui.ui)

(defn label-maybe-update-text-line [this ctx element]
  (let [[_ opts texts'] (parse-element element)
        font'           (get-font opts)
        features'       (concat (:font-features opts) (:font-features ctx))]
    (when (or
            (nil? (:text-line this))
            (not= (:font this) font')
            (not= (:features this) features')
            (not= (:texts this) texts'))
      (let [shaping-options (cond-> ShapingOptions/DEFAULT
                              (not (empty? features'))
                              (.withFeatures (str/join " " features')))
            metrics         (.getMetrics font')
            _               (util/close (:text-line this))
            text-line       (.shapeLine shaper (str/join texts') font' shaping-options)]
        (util/set!! this :font font')
        (util/set!! this :features features')
        (util/set!! this :texts texts')
        (util/set!! this :text-line text-line)
        (util/set!! this :size (util/ipoint
                                 (math/ceil (.getWidth text-line))
                                 (math/ceil (.getCapHeight metrics))))))))

(util/deftype+ Label [^:mut paint
                      ^:mut ^Font font
                      ^:mut features
                      ^:mut texts
                      ^:mut ^TextLine text-line
                      ^:mut size]
  :extends ATerminalNode

  (-measure-impl [this ctx cs]
    (label-maybe-update-text-line this ctx element)
    size)
  
  (-draw-impl [this ctx bounds viewport ^Canvas canvas]
    (label-maybe-update-text-line this ctx element)
    (.drawTextLine canvas text-line (:x bounds) (+ (:y bounds) (:height size)) (or paint (:fill-text ctx))))
  
  (-update-element [_this ctx new-element]
    (let [opts (parse-opts new-element)]
      (set! paint (:paint opts))))
  
  (-unmount-impl [this]
    (.close text-line)))

(defn- label-impl [opts & texts]
  (map->Label {}))

(defn- label-ctor [& texts]
  (let [[_ opts texts] (parse-element (util/consv nil texts))]
    (util/vector* label-impl opts
      (map signal/maybe-read texts))))
