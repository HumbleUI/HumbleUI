(in-ns 'io.github.humbleui.ui)

(import '[io.github.humbleui.skija Color4f ColorSpace Paint PaintMode PaintStrokeCap PaintStrokeJoin])

(def ^:private paint-hex-chars
  {\0 0, \1 1, \2 2, \3 3, \4 4, \5 5, \6 6, \7 7, \8 8, \9 9,
   \a 10, \A 10, \b 11, \B 11, \c 12, \C 12, \d 13, \D 13, \e 14, \E 14, \f 15, \F 15})

(defn- paint-color ^Color4f [spec]
  (util/cond+
    (string? spec)
    (do
      (when-not (re-matches #"[0-9a-fA-F]+" spec)
        (throw (ex-info (str "Malformed color, expected [0-9a-fA-F]+, got: " spec) {:spec spec})))
      (case (count spec)
        3
        (let [r (paint-hex-chars (nth spec 0))
              g (paint-hex-chars (nth spec 1))
              b (paint-hex-chars (nth spec 2))]
          (Color4f.
            (/ (bit-or (bit-shift-left r 4) r) 255.0)
            (/ (bit-or (bit-shift-left g 4) g) 255.0)
            (/ (bit-or (bit-shift-left b 4) b) 255.0)
            1.0))
        
        6
        (Color4f. (unchecked-int (bit-or 0xFF000000 (Integer/parseUnsignedInt spec 16))))
        
        8
        (let [rgba (Integer/parseUnsignedInt spec 16)
              rgb  (clojure.lang.Numbers/unsignedShiftRightInt rgba 8)
              a    (bit-and 0xFF rgba)]
          (Color4f. (unchecked-int (bit-or (bit-shift-left a 24) rgb))))
        
        (throw (ex-info (str "Malformed color, expected 3, 6 or 8 digits, got: " spec) {:spec spec}))))
    
    (int? spec)
    (Color4f. (unchecked-int spec))
    
    (not (sequential? spec))
    (throw (ex-info (str "Malformed color, expected string or vector, got: " (pr-str spec)) {:spec spec}))
  
    (not (every? number? spec))
    (throw (ex-info (str "Malformed color, expected numbers, got: " (pr-str spec)) {:spec spec}))
    
    (not (#{3 4} (count spec)))
    (throw (ex-info (str "Expected 3 or 4 values, got: " (pr-str spec)) {:spec spec}))
    
    (some float? spec)
    (let [_ (when (not (every? #(and (<= 0.0 %) (<= % 1.0)) spec))
              (throw (ex-info (str "Expected floats between 0..1, got: " (pr-str spec)) {:spec spec})))
          r (float (nth spec 0))
          g (float (nth spec 1))
          b (float (nth spec 2))
          a (float (nth spec 3 1.0))]
      (Color4f. r g b a))
    
    :let [cnt (count spec)]
    
    (not (every? #(and (<= 0 %) (<= % 255)) spec))
    (throw (ex-info (str "Expected ints between 0..255, got: " (pr-str spec)) {:spec spec}))
    
    (= 3 cnt)
    (let [r (nth spec 0)
          g (nth spec 1)
          b (nth spec 2)]
      (Color4f. (/ r 255.0) (/ g 255.0) (/ b 255.0) 1.0))
    
    (= 4 cnt)
    (let [r (nth spec 0)
          g (nth spec 1)
          b (nth spec 2)
          a (nth spec 3)]
      (Color4f. (/ r 255.0) (/ g 255.0) (/ b 255.0) (/ a 255.0)))
    
    :else
    (throw (ex-info (str "Malfromed color: " (pr-str spec)) {:spec spec}))))

(defn paint-color-space ^ColorSpace [spec]
  (when-some [cs (:color-space spec)]
    (case cs
      :srgb       (ColorSpace/getSRGB)
      :display-p3 (ColorSpace/getDisplayP3)
      (throw (ex-info (str "Unexpected :color-space: " cs {:spec spec}))))))

(defn paint
  "Colors:
   
     0xFFCC33FF          - AARRGGBB, 100% opaque
     0x80CC33FF          - AARRGGBB, 50% opaque
     \"CC33FF\"          - RRGGBB
     \"CC33FF80\"        - RRGGBBAA
     \"C3F\"             - RGB
     int[3]              - RGB,  each int 0..255
     int[4]              - RGBA, each int 0..255
     float[3]            - RGB,  each float 0..1
     float[4]            - RGBA, each float 0..1
   
   Color spaces:
   
     :srgb
     :display-p3
   
   Specs:

     {:fill \"0088FF\"}  - Fill with just color (sRGB)
     {:fill <float[4]>  
      :color-space <cs>} - Different color space
     {:stroke 0xCC33FF}  - Stroke with default width (1)
     {:stroke 0xCC33FF
      :width 1}          - Stroke with custom width
     {:stroke 0xCC33FF
      :width 1
      :cap   <:butt | :round | :square>
      :join  <:miter | :round | :bevel>
      :miter <float>}    - Stroke other params"
  (^Paint [spec]
   (paint spec *ctx*))
  (^Paint [spec ctx]
   (util/cond+
     :let [fill (:fill spec)]
    
     fill
     (let [p (Paint.)]
       (when-some [color (paint-color fill)]
         (if-some [cs (paint-color-space spec)]
           (.setColor4f p color cs)
           (.setColor4f p color))
         p))
        
     :let [stroke (:stroke spec)]
    
     stroke
     (let [p (Paint.)]
       (.setMode p PaintMode/STROKE)
       (when-some [color (paint-color stroke)]
         (if-some [cs (paint-color-space spec)]
           (.setColor4f p color cs)
           (.setColor4f p color)))
       (let [width (or (:width spec) 1)]
         (.setStrokeWidth p (* width (:scale ctx))))
       (when-some [cap (:cap spec)]
         (.setStrokeCap p (case cap
                            :butt   PaintStrokeCap/BUTT
                            :round  PaintStrokeCap/ROUND
                            :square PaintStrokeCap/SQUARE)))
       (when-some [join (:join spec)]
         (.setStrokeJoin p (case join
                             :miter  PaintStrokeJoin/MITER
                             :round  PaintStrokeJoin/ROUND
                             :bevel  PaintStrokeJoin/BEVEL)))
                             
       p)
    
     :else
     (throw (ex-info "Unknown paint spec, expected {:fill ...} or {:stroke ...}" {:spec spec})))))

(defmacro with-paint [ctx [binding specs] & body]
  `(let [specs#  ~specs
         ctx#    ~ctx
         specs#  (if (sequential? specs#) specs# [specs#])
         paints# (for [spec# specs#]
                   (if (instance? Paint spec#)
                     spec#
                     (paint spec# ctx#)))]
     (try
       (doseq [paint#  paints#
               :let [~binding paint#]]
         ~@body)
       (finally
         (doseq [[spec# paint#] (map vector specs# paints#)
                 :when (not (instance? Paint spec#))]
           (util/close paint#))))))
