(in-ns 'io.github.humbleui.ui)

(util/deftype+ Label [^:mut paint
                      ^:mut ^Font font
                      ^:mut features
                      ^:mut texts
                      ^:mut ^TextLine text-line
                      ^:mut size]
  :extends ATerminalNode

  (-measure-impl [this ctx cs]
    size)
  
  (-draw-impl [this ctx bounds container-size viewport ^Canvas canvas]
    (with-paint ctx [paint (or paint (:fill-text ctx))]
      (.drawTextLine canvas text-line (:x bounds) (+ (:y bounds) (:height size)) paint)))

  (-reconcile-opts [this ctx new-element]
    (let [[_ opts texts'] (parse-element new-element)
          font'           (get-font opts ctx)
          features'       (concat (:font-features opts) (:font-features ctx))]
      (set! paint (:paint opts))
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
              text-line'      (.shapeLine shaper (str/join texts') font' shaping-options)
              size'           (util/ipoint
                                (math/ceil (.getWidth text-line'))
                                (math/ceil (.getCapHeight metrics)))]
          (set! font font')
          (set! features features')
          (set! texts texts')
          (set! text-line text-line')
          (set! size size')
          (invalidate-size this)))))
  
  (-unmount-impl [this]
    (util/close text-line)))

(defn- label-impl [opts & texts]
  (map->Label {}))

(defn- label-ctor [& texts]
  (let [[_ opts texts] (parse-element (util/consv nil texts))]
    (util/vector* label-impl opts
      (map signal/maybe-read texts))))
