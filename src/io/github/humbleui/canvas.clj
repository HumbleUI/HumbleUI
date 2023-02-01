(ns io.github.humbleui.canvas
  (:import
    [io.github.humbleui.types IRect Rect RRect]
    [io.github.humbleui.skija BlendMode Canvas Font Paint]))

(defn draw-line
  ([^Canvas canvas p1 p2 ^Paint paint]
   (.drawLine canvas (:x p1) (:y p1) (:x p2) (:y p2) paint))
  ([^Canvas canvas x1 y1 x2 y2 ^Paint paint]
   (.drawLine canvas x1 y1 x2 y2 paint)))

(defn draw-rect [^Canvas canvas r ^Paint paint]
  (condp instance? r
    IRect (.drawRect  canvas (.toRect ^IRect r) paint)
    RRect (.drawRRect canvas r paint)
    Rect  (.drawRect  canvas r paint)))

(defn draw-circle [^Canvas canvas x y r ^Paint paint]
  (.drawCircle canvas x y r paint))

(defn draw-arc [^Canvas canvas left top right bottom start-angle sweep-angle use-center ^Paint paint]
  (.drawArc canvas left top right bottom start-angle sweep-angle use-center paint))

(def blend-modes
  {:clear BlendMode/CLEAR
   :src BlendMode/SRC
   :dst BlendMode/DST
   :src-over BlendMode/SRC_OVER
   :dst-over BlendMode/DST_OVER
   :src-in BlendMode/SRC_IN
   :dst-in BlendMode/DST_IN
   :src-out BlendMode/SRC_OUT
   :dst-out BlendMode/DST_OUT
   :src-atop BlendMode/SRC_ATOP
   :dst-atop BlendMode/DST_ATOP
   :xor BlendMode/XOR
   :plus BlendMode/PLUS
   :modulate BlendMode/MODULATE
   :screen BlendMode/SCREEN
   :overlay BlendMode/OVERLAY
   :darken BlendMode/DARKEN
   :lighten BlendMode/LIGHTEN
   :color-dodge BlendMode/COLOR_DODGE
   :color-burn BlendMode/COLOR_BURN
   :hard-light BlendMode/HARD_LIGHT
   :soft-light BlendMode/SOFT_LIGHT
   :difference BlendMode/DIFFERENCE
   :exclusion BlendMode/EXCLUSION
   :multiply BlendMode/MULTIPLY
   :hue BlendMode/HUE
   :saturation BlendMode/SATURATION
   :color BlendMode/COLOR
   :luminosity BlendMode/LUMINOSITY})


(defn draw-triangles
  ([^Canvas canvas points colors ^Paint paint]
   (.drawTriangles canvas points colors paint))
  ([^Canvas canvas points colors tex-coords indices ^Paint paint]
   (.drawTriangles canvas points colors tex-coords indices paint))
  ([^Canvas canvas points colors tex-coords indices blend-mode ^Paint paint]
   (let [blend-mode (blend-mode blend-modes)]
    (.drawTriangles canvas points colors tex-coords indices blend-mode paint))))

(defn draw-tri-strip
  ([^Canvas canvas points colors ^Paint paint]
   (.drawTriangleStrip canvas points colors paint))
  ([^Canvas canvas points colors tex-coords indices ^Paint paint]
   (.drawTriangleStrip canvas points colors tex-coords indices paint))
  ([^Canvas canvas points colors tex-coords indices blend-mode ^Paint paint]
   (let [blend-mode (blend-mode blend-modes)]
     (.drawTriangleStrip canvas points colors tex-coords indices blend-mode paint))))

(defn draw-tri-fan
  ([^Canvas canvas points colors ^Paint paint]
   (.drawTriangleFan canvas points colors paint))
  ([^Canvas canvas points colors tex-coords indices ^Paint paint]
   (.drawTriangleFan canvas points colors tex-coords indices paint))
  ([^Canvas canvas points colors tex-coords indices blend-mode ^Paint paint]
   (let [blend-mode (blend-mode blend-modes)]
     (.drawTriangleFan canvas points colors tex-coords indices blend-mode paint))))

(defn clear [^Canvas canvas color]
  (.clear canvas (unchecked-int color)))

(defn draw-string [^Canvas canvas s x y ^Font font ^Paint paint]
  (.drawString canvas (str s) x y font paint))

(defn clip-rect [^Canvas canvas r]
  (condp instance? r
    IRect (.clipRect canvas (.toRect ^IRect r))
    RRect (.clipRRect canvas ^RRect r true)
    Rect  (.clipRect canvas r)))

(defn translate [^Canvas canvas dx dy]
  (.translate canvas dx dy))

(defn scale
  ([^Canvas canvas s]
   (.scale canvas s s))
  ([^Canvas canvas sx sy]
   (.scale canvas sx sy)))

(defn rotate [^Canvas canvas deg]
  (.rotate canvas deg))

(defn skew [^Canvas canvas sx sy]
  (.skew canvas sx sy))

(defn save [^Canvas canvas]
  (.save canvas))

(defn save-count [^Canvas canvas]
  (.getSaveCount canvas))

(defn restore
  ([^Canvas canvas]
   (.restore canvas))
  ([^Canvas canvas count]
   (.restoreToCount canvas count)))

(defmacro with-canvas [canvas & body]
  `(let [canvas# ~canvas
         count# (save canvas#)]
     (try
       ~@body
       (finally
         (restore canvas# count#)))))