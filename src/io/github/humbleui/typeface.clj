(ns io.github.humbleui.typeface
  (:require
    [clojure.java.io :as io])
  (:import
    [java.io Writer]
    [io.github.humbleui.skija Data Typeface]))

(def *default
  (delay
    (Typeface/makeDefault)))
  
(defn make-from-data
  (^Typeface [^Data data]
   (Typeface/makeFromFile data 0))
  (^Typeface [^Data data index]
   (Typeface/makeFromFile data index)))

(defn make-from-path
  (^Typeface [^String path]
   (Typeface/makeFromFile path 0))
  (^Typeface [^String path index]
   (Typeface/makeFromFile path index)))

(defn make-from-resource
  (^Typeface [res]
   (make-from-resource res 0))
  (^Typeface [res index]
   (with-open [is (io/input-stream (io/resource res))]
     (let [bytes (.readAllBytes is)]
       (with-open [data (Data/makeFromBytes bytes)]
         (Typeface/makeFromData data index))))))

(defn family-name ^String [^Typeface typeface]
  (.getFamilyName typeface))

(defmethod print-method Typeface [o ^Writer w]
  (.write w "#Typeface{familyName=")
  (.write w (family-name o))
  (.write w "}"))
