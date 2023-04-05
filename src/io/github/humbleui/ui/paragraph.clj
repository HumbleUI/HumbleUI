(ns io.github.humbleui.ui.paragraph
  (:require
    [clojure.math :as math]
    [clojure.string :as str]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.ui.dynamic :as dynamic])
  (:import
    [io.github.humbleui.skija BreakIterator Canvas Font FontMetrics Paint TextLine]
    [io.github.humbleui.skija.shaper ShapingOptions]))

(defn- layout [tokens max-width cap-height line-height]
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

(core/deftype+ Paragraph [^Paint paint
                          tokens
                          line-height
                          metrics]
  :extends core/ATerminal
  protocols/IComponent
  (-measure [_ _ctx cs]
    (let [layout (layout tokens (:width cs) (:cap-height metrics) line-height)]
      (core/ipoint
        (math/ceil (:width layout))
        (:height layout))))
  
  (-draw [_ _ctx rect ^Canvas canvas]
    (let [layout (layout tokens (:width rect) (:cap-height metrics) line-height)]
      (doseq [[pos token] (core/zip (:positions layout) tokens)
              :when pos]
        (.drawTextLine canvas (:shaped token) (+ (:x rect) (:x pos)) (+ (:y rect) (:y pos)) paint)))))

(defn- split-whitespace [s]
  (let [trimmed (str/trimr s)
        space   (subs s (count trimmed))]
    (if (pos? (count space))
      [trimmed space]
      [s])))

(defn- words [text]
  (with-open [iter (BreakIterator/makeLineInstance)]
    (.setText iter text)
    (loop [words (transient [])
           start 0]
      (let [end (.next iter)]
        (if (= BreakIterator/DONE end)
          (persistent! words)
          (recur (reduce conj! words (split-whitespace (subs text start end))) end))))))

(comment
  (words "A word,   then: “another-word” one"))

(defn paragraph
  ([text]
   (paragraph nil text))
  ([opts text]
   (dynamic/dynamic ctx [^Font font  (or (:font opts) (:font-ui ctx))
                         paint       (or (:paint opts) (:fill-text ctx))
                         line-height (Math/ceil
                                       (or (some-> opts :line-height (* (:scale ctx)))
                                         (* 2 (:cap-height (font/metrics font)))))]
     (let [text     (str text)
           features (cond-> ShapingOptions/DEFAULT
                      (not (empty? (:features opts)))
                      (.withFeatures (str/join " " (:features opts))))
           tokens   (mapv
                      (fn [token]
                        {:text   token
                         :shaped (.shapeLine core/shaper token font ^ShapingOptions features)
                         :blank? (str/blank? token)})
                      (words text))]
       (map->Paragraph
         {:paint       paint
          :tokens      tokens
          :line-height line-height
          :metrics     (font/metrics font)})))))
