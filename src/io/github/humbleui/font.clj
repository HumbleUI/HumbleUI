(ns io.github.humbleui.font
  (:require
    [io.github.humbleui.core :as core]
    [io.github.humbleui.typeface :as typeface])
  (:import
    [java.io Writer]
    [io.github.humbleui.skija Font Typeface]))

(def *font-cache
  "{{:typeface  Typeface
     :font-size size} -> Font}"
  (atom {}))

(defn make-with-size ^Font [^Typeface typeface size]
  (Font. typeface (float size)))

(defn make-with-cap-height ^Font [^Typeface typeface cap-height]
  (let [size    (float 100)
        font    (Font. typeface size)
        current (-> font .getMetrics .getCapHeight)
        size'   (-> size (/ current) (* cap-height))]
    (.setSize font size')))

(defn typeface ^Typeface [^Font font]
  (.getTypeface font))

(defn size [^Font font]
  (.getSize font))

(defn metrics [^Font font]
  (let [m (.getMetrics font)]
    {:top                 (.getTop m)
     :ascent              (.getAscent m)
     :descent             (.getDescent m)
     :bottom              (.getBottom m)
     :leading             (.getLeading m)
     :avg-char-width      (.getAvgCharWidth m)
     :max-char-width      (.getMaxCharWidth m)
     :x-min               (.getXMin m)
     :x-max               (.getXMax m)
     :x-height            (.getXHeight m)
     :cap-height          (.getCapHeight m)
     :underline-thickness (.getUnderlineThickness m)
     :underline-position  (.getUnderlinePosition m)
     :strikeout-thickness (.getStrikeoutThickness m)
     :strikeout-position  (.getStrikeoutPosition m)
     :height              (.getHeight m)}))

(defn set-size! [^Font font size]
  (.setSize font size))

(defmethod print-method Font [o ^Writer w]
  (.write w "#Font{typeface=")
  (.write w (pr-str (typeface o)))
  (.write w ", size=")
  (.write w (str (size o)))
  (.write w "}"))

(defn get-font [opts]
  (let [typeface (typeface/typeface
                   (core/checked-get opts :font-family string?)
                   opts)
        key      (core/merge-some {:typeface typeface}
                   (cond
                     (:font-size opts)
                     (do
                       (assert (and
                                 (number? (:font-size opts))
                                 (>= (:font-size opts) 0))
                         (str ":font-size, expected number? >= 0, got: " (pr-str (:font-size opts))))
                       {:font-size (:font-size opts)})
                     
                     (:font-cap-height opts)
                     (do
                       (assert (and
                                 (number? (:font-cap-height opts))
                                 (>= (:font-cap-height opts) 0))
                         (str ":font-cap-height, expected number? >= 0, got: " (pr-str (:font-cap-height opts))))
                       {:font-cap-height (:font-cap-height opts)})
                     
                     :else
                     (throw (ex-info (str "Expected one of: :cap-height, :size, got: " opts) {}))))]
    (or
      (@*font-cache key)
      (let [font (cond
                   (:font-size opts)
                   (make-with-size typeface (:font-size opts))
                   
                   (:font-cap-height opts)
                   (make-with-cap-height typeface (:font-cap-height opts)))]
        (swap! *font-cache assoc key font) ;; FIXME clean up font cache
        font))))
