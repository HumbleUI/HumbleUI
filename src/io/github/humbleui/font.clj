(ns io.github.humbleui.font
  (:require
    [clojure.java.io :as io]
    [io.github.humbleui.typeface :as typeface])
  (:import
    [java.io Writer]
    [io.github.humbleui.skija Data Font Typeface]))

(set! *warn-on-reflection* true)

(defn ^Font make-with-size [^Typeface typeface size]
  (Font. typeface (float size)))

(defn ^Font make-with-cap-height [^Typeface typeface cap-height]
  (let [size    (float 100)
        font    (Font. typeface size)
        current (-> font .getMetrics .getCapHeight)
        size'   (-> size (/ current) (* cap-height))]
    (.setSize font size')))

(defn ^Typeface typeface [^Font font]
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
  (.write w "#Font{familyName=")
  (.write w (typeface/family-name (typeface o)))
  (.write w ", size=")
  (.write w (str (size o)))
  (.write w "}"))