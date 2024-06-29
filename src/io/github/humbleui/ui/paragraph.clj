(in-ns 'io.github.humbleui.ui)

(import '[io.github.humbleui.skija BreakIterator])

(defn- paragraph-layout [tokens max-width cap-height line-height]
  (core/loopr [positions (transient [])
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
        (recur (conj! positions (core/point 0 cap-height)) token-width cap-height (max width token-width) cap-height)
        
        ;; blank — always advance, but don’t render
        blank?
        (recur (conj! positions nil) (+ x token-width) y width height)
        
        ;; next token fits on the same line
        (<= (+ x token-width) max-width)
        (recur (conj! positions (core/point x y)) (+ x token-width) y (max width (+ x token-width)) height)
        
        ;; have to start new line
        :else
        (recur (conj! positions (core/point 0 (+ y line-height))) token-width (+ y line-height) (max width token-width) (+ height line-height))))
    {:positions (persistent! positions)
     :width     width
     :height    height}))

(core/deftype+ Paragraph [^Paint paint tokens *layout line-height metrics]
  :extends ATerminalNode
  protocols/IComponent
  (-measure-impl [_ _ctx cs]
    (let [layout (core/cached *layout (:width cs)
                   #(paragraph-layout tokens (:width cs) (:cap-height metrics) line-height))]
      (core/ipoint
        (math/ceil (:width layout))
        (:height layout))))
  
  (-draw-impl [_ _ctx rect ^Canvas canvas]
    (let [layout (core/cached *layout (:width rect)
                   #(paragraph-layout tokens (:width rect) (:cap-height metrics) line-height))]
      (doseq [[pos token] (core/zip (:positions layout) tokens)
              :when pos]
        (.drawTextLine canvas (:shaped token) (+ (:x rect) (:x pos)) (+ (:y rect) (:y pos)) paint))))
  
  (-should-reconcile? [_this ctx new-element]
    (= element new-element))
  
  (-unmount-impl [this]
    (doseq [token tokens]
      (.close (:shaped ^TextLine token)))))

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

(defn- paragraph-ctor
  ([text]
   (paragraph-ctor nil text))
  ([opts text]
   (let [^Font font  (or (:font opts) (:font-ui *ctx*))
         paint       (or (:paint opts) (:fill-text *ctx*))
         line-height (Math/ceil
                       (or (some-> opts :line-height (* (:scale *ctx*)))
                         (* 2 (:cap-height (font/metrics font)))))
         text        (str text)
         features    (cond-> ShapingOptions/DEFAULT
                       (not (empty? (:features opts)))
                       (.withFeatures (str/join " " (:features opts))))
         tokens      (mapv
                       (fn [token]
                         {:text   token
                          :shaped (.shapeLine shaper token font ^ShapingOptions features)
                          :blank? (str/blank? token)})
                       (paragraph-words text))]
     (map->Paragraph
       {:paint       paint
        :tokens      tokens
        :*layout     (atom nil)
        :line-height line-height
        :metrics     (font/metrics font)}))))
