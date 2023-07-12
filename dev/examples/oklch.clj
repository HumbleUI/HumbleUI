(ns examples.oklch
  (:require
    [clojure.math :as math]
    [clojure.string :as str]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [java.nio ByteBuffer ByteOrder]
    [java.text DecimalFormat DecimalFormatSymbols]
    [io.github.humbleui.skija Color Color4f Data Matrix33 Paint RuntimeEffect]))

(defn ->nonlinear ^double [^double x]
  (if (>= x 0.0031308)
    (-> (math/pow x (/ 1.0 2.4))
      (* 1.055)
      (- 0.055))
    (* 12.92 x)))

(defn oklch->srgb [^double l ^double c ^double h]
  (let [;; oklch -> oklab
        hr (math/to-radians h)
        a  (* c (math/cos hr))
        b  (* c (math/sin hr))
        
        ;; oklab -> linear-srgb
        l'  (+ l (* +0.3963377774 a) (* +0.2158037573 b))
        m'  (+ l (* -0.1055613458 a) (* -0.0638541728 b))
        s'  (+ l (* -0.0894841775 a) (* -1.2914855480 b))
        l'' (* l' l' l')
        m'' (* m' m' m')
        s'' (* s' s' s')
        lr  (+ (* +4.0767416621 l'') (* -3.3077115913 m'') (* +0.2309699292 s''))
        lg  (+ (* -1.2684380046 l'') (* +2.6097574011 m'') (* -0.3413193965 s''))
        lb  (+ (* -0.0041960863 l'') (* -0.7034186147 m'') (* +1.7076147010 s''))]
    (when (and (<= 0.0 lr 1.0) (<= 0.0 lg 1.0) (<= 0.0 lb 1.0))
      ;; linear-srgb -> srgb
      (let [r (-> lr ->nonlinear (* 255.0) math/floor)
            g (-> lg ->nonlinear (* 255.0) math/floor)
            b (-> lb ->nonlinear (* 255.0) math/floor)]
        (Color/makeRGB r g b)))))

(def *l
  (atom {:value 0.65
         :min   0.0
         :max   1.0
         :step  0.01}))

(def *c
  (atom {:value 0.1
         :min   0.0
         :max   0.35
         :step  0.01}))

(def *h
  (atom {:value 180
         :min   0
         :max   360
         :step  1}))

(defn on-paint [ctx canvas size]
  (let [{:keys [width height]} size
        steps 100
        dx    (max 1 (/ width steps))
        dy    (max 1 (/ height steps))
        l     (:value @*l)]
    (with-open [fill (paint/fill 0xFFFFFFFF)]
      (doseq [y (range 0.0 height dy)
              :let [c (-> y (/ height) (->> (- 1.0)) (* 0.35))]
              x (range 0.0 width dx)
              :let [h (-> x (/ width) (* 360.0))
                    color (oklch->srgb l c h)]
              :when color]
        (.setColor fill color)
        (canvas/draw-rect canvas (core/rect-xywh (math/ceil x) (math/ceil y) (math/ceil dx) (math/ceil dy)) fill)))))

(def oklab-shared "
    // https://bottosson.github.io/posts/oklab/#converting-from-linear-srgb-to-oklab
    const mat3 fromOkStep1 = mat3(
       1.0, 1.0, 1.0,
       0.3963377774, -0.1055613458, -0.0894841775,
       0.2158037573, -0.0638541728, -1.2914855480);
                       
    const mat3 fromOkStep2 = mat3(
       4.0767416621, -1.2684380046, -0.0041960863,
       -3.3077115913, 2.6097574011, -0.7034186147,
       0.2309699292, -0.3413193965,  1.7076147010);
    
    vec3 oklabToLinearSRGB(vec3 x) {
        vec3 lms = fromOkStep1 * x;
        return fromOkStep2 * (lms * lms * lms);
    }
                   
    vec4 fromLinear(vec3 lsrgb) {
       if (lsrgb.x < 0.0 || lsrgb.x > 1.0 || lsrgb.y < 0.0 || lsrgb.y > 1.0 || lsrgb.z < 0.0 || lsrgb.z > 1.0) {
           return vec4(0.0, 0.0, 0.0, 0.0);
       } else {
           return vec4(fromLinearSrgb(lsrgb), 1.0);
       }
    }
                   
    vec4 fromLCH(float l, float c, float h) {
       float hr = radians(h);
       float a = c * cos(hr);
       float b = c * sin(hr);
       return fromLinear(oklabToLinearSRGB(vec3(l, a, b)));
    }")

(def oklab-for-l "
    uniform float l;

    half4 main(vec2 fragcoord) {
       float c = (1.0 - fragcoord.y) * 0.35;
       float h = fragcoord.x * 360;
       return fromLCH(l, c, h);
    }")

(def oklab-for-c "
    uniform float c;

    half4 main(vec2 fragcoord) {
       float l = 1.0 - fragcoord.y;
       float h = fragcoord.x * 360;
       return fromLCH(l, c, h);
    }")

(def oklab-for-h "
    uniform float h;

    half4 main(vec2 fragcoord) {
       float l = fragcoord.x;
       float c = (1.0 - fragcoord.y) * 0.35;
       return fromLCH(l, c, h);
    }")

(def effect-for-l
  (RuntimeEffect/makeForShader (str oklab-shared oklab-for-l)))

(def effect-for-c
  (RuntimeEffect/makeForShader (str oklab-shared oklab-for-c)))

(def effect-for-h
  (RuntimeEffect/makeForShader (str oklab-shared oklab-for-h)))

(defn on-paint-shader [^RuntimeEffect effect value ctx canvas size]
  (let [{:keys [width height]} size
        l (:value @*l)
        bb (doto (ByteBuffer/allocate 4)
             (.order (ByteOrder/nativeOrder))
             (.putFloat (float value)))]
    (canvas/scale canvas width height)
    (with-open [data   (Data/makeFromBytes (.array bb))
                shader (.makeShader effect data nil nil)
                fill   (doto (Paint.)
                         (.setShader shader))]
      (canvas/draw-rect canvas (core/rect-xywh 0 0 1 1) fill))))

(def ^:private ^DecimalFormat decimal-format
  (DecimalFormat. "0.##" (doto (DecimalFormatSymbols.)
                           (.setDecimalSeparator \.))))

(defn row [name *atom]
  [(ui/width 300
     (ui/slider *atom))
   (ui/gap 10 0)
   (ui/valign 0.5
     (ui/max-width
       [(ui/label (str name ": 0.999"))]
       (ui/dynamic _ [value (:value @*atom)]
         (ui/label (format "%s: %s" name (.format decimal-format (double value)))))))])

(def ui
  (ui/padding 20
    (ui/column
      [:stretch 1
       (ui/row
         [:stretch 1
          (ui/dynamic _ [h (:value @*h)]
            (ui/canvas {:on-paint (partial on-paint-shader effect-for-h h)}))]
         (ui/gap 10 0)
         [:stretch 1
          (ui/dynamic _ [l (:value @*l)]
            (ui/canvas {:on-paint (partial on-paint-shader effect-for-l l)}))])]
      (ui/gap 0 10)
      [:stretch 1
       (ui/row
         [:stretch 1
          (ui/center
            (ui/grid
              [(row "Lightness" *l)
               [(ui/gap 0 20)]
               (row "Chroma" *c)
               [(ui/gap 0 20)]
               (row "Hue" *h)]))]
         (ui/gap 10 0)
         [:stretch 1
          (ui/dynamic _ [c (:value @*c)]
            (ui/canvas {:on-paint (partial on-paint-shader effect-for-c c)}))])])))
