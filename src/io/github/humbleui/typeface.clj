(ns io.github.humbleui.typeface
  (:require
    [clojure.java.io :as io])
  (:import
    [java.io Writer]
    [io.github.humbleui.skija Data Typeface]))

(defn ^Typeface make-from-data
  ([^Data data]
   (Typeface/makeFromFile data 0))
  ([^Data data index]
   (Typeface/makeFromFile data index)))

(defn ^Typeface make-from-path
  ([^String path]
   (Typeface/makeFromFile path 0))
  ([^String path index]
   (Typeface/makeFromFile path index)))

(defn ^Typeface make-from-resource
  ([res]
   (make-from-resource res 0))
  ([res index]
    (with-open [is (io/input-stream (io/resource res))]
      (let [bytes (.readAllBytes is)]
        (with-open [data (Data/makeFromBytes bytes)]
          (Typeface/makeFromData data index))))))

(defn ^String family-name [^Typeface typeface]
  (.getFamilyName typeface))

(defmethod print-method Typeface [o ^Writer w]
  (.write w "#Typeface{familyName=")
  (.write w (family-name o))
  (.write w "}"))
