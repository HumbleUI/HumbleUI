(ns examples.unicode
  (:require
    [clojure.string :as str]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.typeface :as typeface]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.types IPoint]
    [io.github.humbleui.skija Canvas Color Paint]))

(defn assign [ranges]
  (->> ranges
    (reduce
      (fn [m [from to prop]]
        (reduce
          (fn [m code]
            (let [block (quot code 0x100)]
              (update m block update prop (fnil inc 0))))
          m (range from (inc to))))
      {})
    (reduce-kv
      (fn [m block v]
        (assoc m block
          (->> v (sort-by second #(compare %2 %1)) first first)))
      (sorted-map))))

(defn normalize-block-name [name]
  (cond
    (str/index-of name "CJK") "CJK"
    (#{"Hiragana" "Katakana" "Kana"} name) "CJK"
    (str/index-of name "Surrogates")       "Surrogates"
    (str/index-of name "Pictographs")      "Emoji"
    (str/index-of name "Emoticons")        "Emoji"
    (= name "Tags")                        "Emoji"
    
    (str/index-of name "Hangul")           "Hangul"
    (str/index-of name "Arabic")           "Arabic"
    (str/index-of name "Symbols")          "Symbols"
    (str/index-of name "Arrows")           "Symbols"
    (str/index-of name "Mathematical")     "Symbols"
    (str/index-of name "Shapes")           "Symbols"
    (str/index-of name "Miscellaneous")    "Symbols"
    (str/index-of name "Tiles")            "Symbols"
    (str/index-of name "Cards")            "Symbols"
    (str/index-of name "Enclosed")         "Symbols"
    (#{"Number Forms" "Control Pictures" "Optical Character Recognition" "Enclosed Alphanumerics" "Box Drawing" "Block Elements" "Dingbats"} name) "Symbols"
    
    :else
    (loop [name name]
      (let [name' (str/trim (str/replace name #"Basic|Compatibility|General|Area|[- ][A-Z0-9]$|Supplement(al|ary)?|Extended|Extensions|Symbols|Characters|Components|Additional|Syllables|Radicals|and|for| (?= )" ""))]
        (if (= name name')
          name
          (recur name'))))))

(def blocks
  (->> (slurp "dev/examples/unicode/Blocks.txt")
    (str/split-lines)
    (keep #(when-some [[_ from to name] (re-matches #"([0-9A-F]+)..([0-9A-F]+); (.*)" %)]
             [(Long/parseLong from 16) (Long/parseLong to 16) (normalize-block-name name)]))
    (assign)))

(def scripts
  (->> (slurp "dev/examples/unicode/Scripts.txt")
    (str/split-lines)
    (keep #(when-some [[_ from to script _] (re-matches #"([0-9A-F]+)(?:..([0-9A-F]+))? *; ([a-zA-Z]+) # (.*)" %)]
             [(Long/parseLong from 16)
              (Long/parseLong (or to from) 16)
              script]))
    (assign)))

(def age
  (->> (slurp "dev/examples/unicode/DerivedAge.txt")
    (str/split-lines)
    (remove #(str/index-of % "noncharacter"))
    (keep #(when-some [[_ from to version _] (re-matches #"([0-9A-F]+)(?:..([0-9A-F]+))? *; ([0-9.]+) # (.*)" %)]
             [(Long/parseLong from 16)
              (Long/parseLong (or to from) 16)
              version]))
    (assign)))

(def fill-bg
  (paint/fill 0xFFFFFFFF))

(def fill-unused
  (paint/fill 0xFFF8F8F8))

(def ^Paint fill-used
  (paint/fill 0xFF33CC33))

(def fill-text
  (paint/fill 0xFF000000))

(def font-text
  (font/make-with-cap-height
    (typeface/make-from-path "dev/fonts/IBMPlexMono-Text.otf")
    16))

(def inferno
  [(unchecked-int 0xFFF6F7BC)
   (unchecked-int 0xFFFD9E6C)
   (unchecked-int 0xFFDA4767)
   (unchecked-int 0xFF8B2781)
   (unchecked-int 0xFF380E6D)
   (unchecked-int 0xFF050508)])

(defn interpolate [x palette]
  (let [x    (* x 0.999)
        bins (dec (count palette))
        x'   (* x bins)
        from (nth palette (int x'))
        to   (nth palette (inc (int x')))
        x''  (- x (Math/floor x))]
    (Color/makeLerp from to x'')))

(defn color-age [version]
  (let [[major minor] (str/split version #"\.")
        ver   (-> (parse-long major) (* 4) (+ (parse-long minor)))]
    (interpolate (- 1 (/ ver 61)) inferno)))

(defn color-block [name]
  (cond
    (= "Latin" name)       0xFFdb3c18
    (= "CJK" name)         0xFF229EBC
    (= "Emoji" name)       0xFFee80fe
    (= "Symbols" name)     0xFFf85582
    (= "Private Use" name) 0xFFfb5607
    (= "Surrogates" name)  0xFF808080
    (= "Hangul" name)      0xFF8ecae6
    (= "Yi" name)          0xFF2a9d8f
    (= "Tangut" name)      0xFF588157
    (= "Arabic" name)      0xFF5f0f40
    (= "Variation Selectors" name) 0xFFDDDDDD
    
    :else
    ; 0xFFCCCCCC))
    (-> name hash (Color/withA 0xFF))))

(def cols
  4)

(def rows
  (+ (quot 16 cols) 1))

(def gapx
  4)

(def gapy
  40)

(def padding
  10)

(defn on-paint [ctx ^Canvas canvas ^IPoint size]
  (canvas/clear canvas 0xFFFFFFFF)
  (canvas/with-canvas canvas
    (canvas/translate canvas padding padding)
    (doseq [i (range 0 17)
            :let [x (rem i cols)
                  y (quot i cols)]]
      (canvas/draw-rect canvas (core/rect-xywh (* x (+ 256 gapx)) (* y (+ 256 gapy)) 256 256) fill-unused)
      (canvas/draw-string canvas (format "%d0000..%dFFFF" i i)
        (+ (* x (+ 256 gapx)) 0)
        (+ (* y (+ 256 gapy)) 280)
        font-text fill-text))
    
    (doseq [[start name] blocks]
      (let [start   (* start 0x100)
            block   (quot start 0x10000)
            blockx  (-> (rem block cols) (* (+ 256 gapx)))
            blocky  (-> (quot block cols) (* (+ 256 gapy)))
            start'  (rem start 0x10000)
            start'' (quot start' 0x100)
            x       (* 0x10 (rem start'' 0x10))
            y       (* 0x10 (quot start'' 0x10))]
        (.setColor fill-used (unchecked-int (color-block name)))
        (canvas/draw-rect canvas (core/rect-xywh (+ blockx x) (+ blocky y) 0x10 0x10) fill-used)))))
            
(def ui
  (ui/center
    (ui/width (-> (* 256 cols) (+ (* gapx (dec cols))) (+ (* 2 padding)) (quot 2))
      (ui/height (-> (* 256 rows) (+ (* gapy rows)) (+ (* 2 padding)) (quot 2))
        (ui/canvas {:on-paint on-paint})))))

