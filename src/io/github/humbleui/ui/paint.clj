(in-ns 'io.github.humbleui.ui)

(import '[io.github.humbleui.skija Color4f ColorSpace Paint PaintMode PaintStrokeCap PaintStrokeJoin])

(defn ^:private paint-hex-chars ^long [ch]
  (case ch
    \0 0
    \1 1
    \2 2
    \3 3
    \4 4
    \5 5
    \6 6
    \7 7
    \8 8
    \9 9
    \a 10
    \A 10
    \b 11
    \B 11
    \c 12
    \C 12
    \d 13
    \D 13
    \e 14
    \E 14
    \f 15
    \F 15))

(defn- color->nonlinear ^double [^double x]
  (if (>= x 0.0031308)
    (-> (math/pow x (/ 1.0 2.4))
      (* 1.055)
      (- 0.055))
    (* 12.92 x)))

(defn- color->linear ^double [^double x]
  (let [sign (if (neg? x) -1 1)
        abs  (* x sign)]
    (if (<= abs 0.04045)
      (/ x 12.92)
      (-> abs (+ 0.055) (/ 1.055) (math/pow 2.4) (* sign)))))

(defn color-oklch->srgb ^Color4f [^Color4f color]
  (let [l  (.getR color)
        c  (.getG color)
        h  (.getB color)
        
        ;; oklch -> oklab
        hr (math/to-radians h)
        a  (* c (math/cos hr))
        b  (* c (math/sin hr))
        
        ;; oklab -> srgb
        l'  (+ l (+ (* +0.3963377774 a) (* +0.2158037573 b)))
        m'  (+ l (+ (* -0.1055613458 a) (* -0.0638541728 b)))
        s'  (+ l (+ (* -0.0894841775 a) (* -1.2914855480 b)))
        l'' (* l' (* l' l'))
        m'' (* m' (* m' m'))
        s'' (* s' (* s' s'))
        lr  (+ (* +4.0767416621 l'') (+ (* -3.3077115913 m'') (* +0.2309699292 s'')))
        lg  (+ (* -1.2684380046 l'') (+ (* +2.6097574011 m'') (* -0.3413193965 s'')))
        lb  (+ (* -0.0041960863 l'') (+ (* -0.7034186147 m'') (* +1.7076147010 s'')))]
    (Color4f. (color->nonlinear (max 0 lr)) (color->nonlinear (max 0 lg)) (color->nonlinear (max 0 lb)) (.getA color))))

(defn color-p3->srgb ^Color4f [^Color4f color]
  (let [r  (.getR color)
        g  (.getG color)
        b  (.getB color)
        ;; p3 -> p3-linear
        r' (color->linear r)
        g' (color->linear g)
        b' (color->linear b)
        ;; p3 -> CIE XYZ (D65)
        x  (+ (* r' 0.4865709486482162) (+ (* g' 0.26566769316909306) (* b' 0.1982172852343625)))
        y  (+ (* r' 0.2289745640697488) (+ (* g' 0.6917385218365064)  (* b' 0.079286914093745)))
        z  (+ (* r' 0.0000000000000000) (+ (* g' 0.04511338185890264) (* b' 1.043944368900976)))
        ;; CIE XYZ (D65) -> linear sRGB
        lr (+ (* x  3.2404542) (+ (* y -1.5371385) (* z -0.4985314)))
        lg (+ (* x -0.9692660) (+ (* y  1.8760108) (* z  0.0415560)))
        lb (+ (* x  0.0556434) (+ (* y -0.2040259) (* z  1.0572252)))]
    (Color4f. (color->nonlinear (max 0 lr)) (color->nonlinear (max 0 lg)) (color->nonlinear (max 0 lb)) (.getA color))))

(defn- color->srgb ^Color4f [^Color4f color model]
  (case model
    nil         color
    :srgb       color
    :display-p3 (color-p3->srgb color) #_(.convert (ColorSpace/getDisplayP3) (ColorSpace/getSRGB) color) #_color
    :oklch      (color-oklch->srgb color)))

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
    
    :else
    (let [; _ (when (not (every? #(and (<= 0.0 %) (<= % 1.0)) spec))
          ;     (throw (ex-info (str "Expected floats between 0..1, got: " (pr-str spec)) {:spec spec})))
          r (float (nth spec 0))
          g (float (nth spec 1))
          b (float (nth spec 2))
          a (float (nth spec 3 1.0))]
      (Color4f. r g b a))))

(defn color-space [model]
  nil
  #_(if (= :display-p3 model)
      (ColorSpace/getDisplayP3)
      (ColorSpace/getSRGB)))

(defn paint
  "Colors:
   
     0xFFCC33FF          - AARRGGBB, 100% opaque
     0x80CC33FF          - AARRGGBB, 50% opaque
     \"CC33FF\"          - RRGGBB
     \"CC33FF80\"        - RRGGBBAA
     \"C3F\"             - RGB
     float[3]            - RGB,  float range depending on model
     float[4]            - RGBA, float range depending on model
   
   Color models:
   
     :srgb               - [0..1, 0..1, 0..1]
     :display-p3         - [0..1, 0..1, 0..1]
     :oklch              - [0..1, 0..0.4, 0..360]
   
   Specs:

     {:fill \"0088FF\"}  - Fill with just color (sRGB)
     {:fill  <float[4]>  
      :model <model>}    - Different color model
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
      (or (string? spec) (int? spec))
      (recur {:fill spec} ctx)
      
      :let [model (:model spec)
            fill  (:fill spec)]
    
      fill
      (let [color (-> fill paint-color (color->srgb model))]
        (doto (Paint.)
          (.setColor4f color (color-space model))))
        
      :let [stroke (:stroke spec)]
    
      stroke
      (let [color (-> stroke paint-color (color->srgb model))
            width (-> (:width spec) (or 1.0) (* (:scale ctx)))
            p     (doto (Paint.)
                    (.setMode PaintMode/STROKE)            
                    (.setColor4f color (color-space model))
                    (.setStrokeWidth width))]
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
                             
        p))))

(defmacro with-paint [ctx [binding specs] & body]
  `(let [specs#  ~specs
         ctx#    ~ctx
         specs#  (if (sequential? specs#) specs# [specs#])
         paints# (for [spec# specs#]
                   (cond
                     (nil? spec#)            nil
                     (instance? Paint spec#) spec#
                     :else                   (paint spec# ctx#)))]
     (try
       (doseq [paint#  paints#
               :when paint#
               :let [~binding paint#]]
         ~@body)
       (finally
         (doseq [[spec# paint#] (map vector specs# paints#)
                 :when (not (instance? Paint spec#))]
           (util/close paint#))))))
