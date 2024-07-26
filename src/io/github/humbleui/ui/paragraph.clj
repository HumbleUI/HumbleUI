(in-ns 'io.github.humbleui.ui)

(import '[io.github.humbleui.skija BreakIterator])

(defn- paragraph-split-whitespace [s]
  (let [trimmed (str/trimr s)
        space   (subs s (count trimmed))]
    (if (pos? (count space))
      [trimmed space]
      [s])))

(defn- paragraph-words [text]
  (with-open [iter (BreakIterator/makeLineInstance)]
    (.setText iter text)
    (loop [words (transient [])
           start 0]
      (let [end (.next iter)]
        (if (= BreakIterator/DONE end)
          (persistent! words)
          (recur (reduce conj! words (paragraph-split-whitespace (subs text start end))) end))))))

(defn- paragraph-layout [tokens max-width cap-height line-height]
  (util/loopr [positions (transient [])
               x         0
               y         0
               width     0
               height    0]
    [token tokens]
    (let [{:keys [^TextLine shaped blank?]} token
          token-width (.getWidth shaped)]
      (cond
        ;; first token ever
        (and (= x 0) (= y 0))
        (recur (conj! positions (util/point 0 cap-height)) token-width cap-height (max width token-width) cap-height)
        
        ;; blank — always advance, but don’t render
        blank?
        (recur (conj! positions nil) (+ x token-width) y width height)
        
        ;; next token fits on the same line
        (<= (+ x token-width) max-width)
        (recur (conj! positions (util/point x y)) (+ x token-width) y (max width (+ x token-width)) height)
        
        ;; have to start new line
        :else
        (recur (conj! positions (util/point 0 (+ y line-height))) token-width (+ y line-height) (max width token-width) (+ height line-height))))
    {:positions (persistent! positions)
     :width     width
     :height    height}))

(defn paragraph-maybe-update [this ctx element max-width']
  (let [[_ opts texts'] (parse-element element)
        font'           (get-font opts)
        metrics'        (if (= (:font this) font')
                          (:metrics this)
                          (font/metrics font'))
        features'       (concat (:font-features opts) (:font-features ctx))
        line-height'    (math/ceil
                          (or
                            (some-> opts :line-height (* (:scale ctx)))
                            (* 2 (:cap-height metrics'))))]
    (when (or
            (nil? (:tokens this))
            (not= (:font this) font')
            (not= (:metrics this) metrics')
            (not= (:features this) features')
            (not= (:texts this) texts'))
      (let [text            (str/join texts')
            shaping-options (cond-> ShapingOptions/DEFAULT
                              (not (empty? features'))
                              (.withFeatures (str/join " " features')))
            
            _               (doseq [token (:tokens this)]
                              (util/close (:shaped token)))
            tokens          (mapv
                              (fn [token]
                                {:text   token
                                 :shaped (.shapeLine shaper token font' shaping-options)
                                 :blank? (str/blank? token)})
                              (paragraph-words text))]
        (util/set!! this :font font')
        (util/set!! this :metrics metrics')
        (util/set!! this :features features')
        (util/set!! this :texts texts')
        (util/set!! this :tokens tokens)))
    
    (when (not= (:line-height this) line-height')
      (util/set!! this :line-height line-height'))
    
    (when (or
            (nil? (:layout this))
            (not= (:max-width this) max-width'))
      (util/set!! this :max-width max-width')
      (util/set!! this :layout (paragraph-layout (:tokens this) max-width' (:cap-height metrics') line-height')))))

(util/deftype+ Paragraph [^:mut paint
                          ^:mut ^Font font
                          ^:mut metrics
                          ^:mut features
                          ^:mut texts
                          ^:mut tokens
                          ^:mut line-height
                          ^:mut max-width
                          ^:mut layout]
  :extends ATerminalNode

  (-measure-impl [this ctx cs]
    (paragraph-maybe-update this ctx element (:width cs))
    (util/ipoint (math/ceil (:width layout)) (:height layout)))
  
  (-draw-impl [this ctx bounds viewport ^Canvas canvas]
    (paragraph-maybe-update this ctx element (:width bounds))
    (let [paint' (or paint (:fill-text ctx))]
      (doseq [[pos token] (util/zip (:positions layout) tokens)
              :when pos]
        (.drawTextLine canvas (:shaped token) (+ (:x bounds) (:x pos)) (+ (:y bounds) (:y pos)) paint'))))
  
  (-unmount-impl [this]
    (doseq [token tokens]
      (util/close (:shaped token)))))

(defn- paragraph-impl [opts & texts]
  (map->Paragraph {}))

(defn- paragraph-ctor [& texts]
  (let [[_ opts texts] (parse-element (util/consv nil texts))]
    (util/vector* paragraph-impl opts
      (map signal/maybe-read texts))))
