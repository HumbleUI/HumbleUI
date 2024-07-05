(ns io.github.humbleui.typeface
  (:require
    [clojure.java.io :as io]
    [clojure.set :as set]
    [clojure.string :as str]
    [io.github.humbleui.core :as core])
  (:import
    [java.io File Writer]
    [io.github.humbleui.skija Data FontMgr FontSlant FontStyle FontWidth Typeface]))

(def *typeface-cache
  "{{:font-family <string>
     :font-weight <int>
     :font-width  :ultra-condensed | :extra-condensed | :condensed | :semi-condensed | :normal | :semi-expanded | :expanded | :extra-expanded | :ultra-expanded
     :font-slant  :upright | :italic | :oblique}
    -> Typeface}"
  (atom {}))

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

(defmethod print-method Typeface [^Typeface o ^Writer w]
  (.write w "#Typeface{familyName=")
  (.write w (family-name o))
  (.write w " weight=")
  (.write w (-> o .getFontStyle .getWeight str))
  (.write w " width=")
  (.write w (-> o .getFontStyle .getWidth str))
  (.write w " slant=")
  (.write w (-> o .getFontStyle .getSlant str))
  (.write w "}"))

(def weights
  {:invisible   0
   :thin        100
   :extra-light 200
   :light       300
   :normal      400
   :medium      500
   :semi-bold   600
   :bold        700
   :extra-bold  800
   :black       900
   :extra-black 1000})

(def widths
  {:ultra-condensed FontWidth/ULTRA_CONDENSED 
   :extra-condensed FontWidth/EXTRA_CONDENSED 
   :condensed       FontWidth/CONDENSED       
   :semi-condensed  FontWidth/SEMI_CONDENSED  
   :normal          FontWidth/NORMAL          
   :semi-expanded   FontWidth/SEMI_EXPANDED   
   :expanded        FontWidth/EXPANDED        
   :extra-expanded  FontWidth/EXTRA_EXPANDED  
   :ultra-expanded  FontWidth/ULTRA_EXPANDED})

(def widths-inverted
  (set/map-invert widths))

(def slants
  {:upright FontSlant/UPRIGHT
   :italic  FontSlant/ITALIC
   :oblique FontSlant/OBLIQUE})

(def slants-inverted
  (set/map-invert slants))

(defn font-style->clj [^FontStyle style]
  {:font-weight (.getWeight style)
   :font-width  (widths-inverted (.getWidth style))
   :font-slant  (slants-inverted (.getSlant style))})

(defn clj->font-style
  ^FontStyle
  [{:keys [font-weight font-width font-slant]
    :or {font-weight 400
         font-width  :normal
         font-slant  :upright}}]
  (let [font-weight (or (weights font-weight) font-weight)]
    (cond-> FontStyle/NORMAL
      (not= 400 font-weight)
      (.withWeight font-weight)
    
      (not= :normal font-width)
      (.withWidth (widths font-width))
    
      (not= :upright font-slant)
      (.withSlant (slants font-slant)))))

(defn load-typeface
  "Load typeface from a file and cache it.
   
   Supports string, File, URL, URI, byte array and Skija Data."
  (^Typeface [what]
    (load-typeface what {}))
  (^Typeface [what opts]
    (cond
      (or
        (string? what)
        (instance? File what)
        (instance? java.net.URL what)
        (instance? java.net.URI what))
      (with-open [is (io/input-stream what)]
        (load-typeface is opts))
      
      (instance? java.io.InputStream what)
      (recur (.readAllBytes ^java.io.InputStream what) opts)
      
      (bytes? what)
      (with-open [data (Data/makeFromBytes ^bytes what)]
        (load-typeface data opts))
      
      (instance? Data what)
      (let [typeface (Typeface/makeFromData what)
            family   (.getFamilyName typeface)
            style    (font-style->clj (.getFontStyle typeface))
            key      (core/merge-some {:font-family family} style)]
        (swap! *typeface-cache assoc key typeface)
        typeface))))

(defn load-typefaces
  "Loads all typefaces from a directory and cache them. Loaded typefaces
   will be available for (ui/get-font) by their family name and style."
  ([dir]
   (load-typefaces dir {}))
  ([dir {:keys [recursive?]}]
   (let [dir (io/file dir)]
     (for [file  (if recursive?
                   (file-seq dir)
                   (.listFiles dir))
           :when (or 
                   (str/ends-with? (.getName ^File file) ".ttf")
                   (str/ends-with? (.getName ^File file) ".otf"))]
       (load-typeface file)))))

(defn find-typeface ^Typeface [family style]
  (let [typeface (.matchFamilyStyle (FontMgr/getDefault) family (clj->font-style style))]
    (swap! *typeface-cache assoc (core/merge-some {:font-family family} style) typeface) ;; FIXME clean up typeface cache
    typeface))

(defn typeface
  "Returns a cached version of a typeface by family name and style"
  ([families]
   (typeface families {}))
  ([families style]
   (assert (and (coll? families) (every? string? families))
     (str "families, expected: [string ...], got: " (pr-str families)))
   (assert
     (or 
       (nil? (:font-weight style))
       (weights (:font-weight style))
       (and (number? (:font-weight style)) (<= 0 (:font-weight style) 1000)))
     (str ":font-weight, expected one of: " (str/join "," (sort (keys weights))) " or number 0..1000, got: " (pr-str (:font-weight style))))
   (assert (or (nil? (:font-width style)) (widths (:font-width style))) (str ":font-width, expected one of: " (str/join "," (sort (keys widths))) ", got: " (pr-str (:font-width style))))
   (assert (or (nil? (:font-slant style)) (slants (:font-slant style))) (str ":font-slant, expected one of: " (str/join "," (keys slants)) ", got: " (pr-str (:font-slant style))))
   (let [style (core/merge-some
                 {:font-weight 400
                  :font-width  :normal
                  :font-slant  :upright}
                 (-> style
                   (select-keys [:font-weight :font-width :font-slant])
                   (update :font-weight weights (:font-weight style))))]
     (loop [families families]
       (core/cond+
         (empty? families)
         @*default
         
         :let [family (first families)
               cached (@*typeface-cache (core/merge-some {:font-family family} style) ::not-found)]
         
         (nil? cached)
         (recur (next families))
         
         (= ::not-found cached)
         (or
           (find-typeface family style)
           (recur (next families)))
         
         :else
         cached)))))
