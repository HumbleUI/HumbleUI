(ns io.github.humbleui.canvas
  (:import
    [io.github.humbleui.types IPoint Point IRect Rect RRect]
    [io.github.humbleui.skija BlendMode Canvas Font Paint Path]))


(def float-array-class (Class/forName "[F"))
(def ipoint-array-class (Class/forName "[Lio.github.humbleui.types.IPoint;"))
(def point-array-class (Class/forName "[Lio.github.humbleui.types.Point;"))

(defn draw-point
  ([^Canvas canvas p ^Paint paint]
   (.drawPoint canvas (:x p) (:y p) paint))
  ([^Canvas canvas x y ^Paint paint]
   (.drawPoint canvas x y paint)))

(defn draw-points [^Canvas canvas floats-or-points ^Paint paint]
  (cond
    ;; skip empty colls
    (empty? floats-or-points) nil
    (and (coll? floats-or-points) (instance? Point (first floats-or-points)))
    (.drawPoints canvas ^"[Lio.github.humbleui.types.Point;" (into-array Point floats-or-points) paint)
    (and (coll? floats-or-points) (instance? IPoint (first floats-or-points)))
    (.drawPoints canvas ^"[Lio.github.humbleui.types.Point;" (into-array Point (map (fn [^IPoint x] (.toPoint x)) floats-or-points)) paint)
    (coll? floats-or-points)
    (.drawPoints canvas #^floats (float-array floats-or-points) paint)
    (= (type floats-or-points) float-array-class)
    (.drawPoints canvas #^floats floats-or-points paint)
    (= (type floats-or-points) ipoint-array-class)
    (.drawPoints canvas ^"[Lio.github.humbleui.types.Point;" (into-array Point (map (fn [^IPoint x] (.toPoint x)) floats-or-points)) paint)
    :else
    (.drawPoints canvas ^"[Lio.github.humbleui.types.Point;" floats-or-points paint)))

(defn draw-lines [^Canvas canvas floats-or-points ^Paint paint]
  (cond
    ;; skip empty colls
    (empty? floats-or-points) nil
    (and (coll? floats-or-points) (instance? Point (first floats-or-points)))
    (.drawLines canvas ^"[Lio.github.humbleui.types.Point;" (into-array Point floats-or-points) paint)
    (and (coll? floats-or-points) (instance? IPoint (first floats-or-points)))
    (.drawLines canvas ^"[Lio.github.humbleui.types.Point;" (into-array Point (map (fn [^IPoint x] (.toPoint x)) floats-or-points)) paint)
    (coll? floats-or-points)
    (.drawLines canvas #^floats (float-array floats-or-points) paint)
    (= (type floats-or-points) float-array-class)
    (.drawLines canvas #^floats floats-or-points paint)
    (= (type floats-or-points) ipoint-array-class)
    (.drawLines canvas ^"[Lio.github.humbleui.types.Point;" (into-array Point (map (fn [^IPoint x] (.toPoint x)) floats-or-points)) paint)
    :else
    (.drawLines canvas ^"[Lio.github.humbleui.types.Point;" floats-or-points paint)))

(defn draw-polygon
  ([^Canvas canvas floats-or-points ^Paint paint]
   (cond
     ;; skip empty colls
     (empty? floats-or-points) nil
     (and (coll? floats-or-points) (instance? Point (first floats-or-points)))
     (.drawPolygon canvas ^"[Lio.github.humbleui.types.Point;" (into-array Point floats-or-points) paint)
     (and (coll? floats-or-points) (instance? IPoint (first floats-or-points)))
     (.drawPolygon canvas ^"[Lio.github.humbleui.types.Point;" (into-array Point (map (fn [^IPoint x] (.toPoint x)) floats-or-points)) paint)
     (coll? floats-or-points)
     (.drawPolygon canvas #^floats (float-array floats-or-points) paint)
     (= (type floats-or-points) float-array-class)
     (.drawPolygon canvas #^floats floats-or-points paint)
     (= (type floats-or-points) ipoint-array-class)
     (.drawPolygon canvas ^"[Lio.github.humbleui.types.Point;" (into-array Point (map (fn [^IPoint x] (.toPoint x)) floats-or-points)) paint)
     :else
     (.drawPolygon canvas ^"[Lio.github.humbleui.types.Point;" floats-or-points paint))))

(defn draw-line
  ([^Canvas canvas p1 p2 ^Paint paint]
   (.drawLine canvas (:x p1) (:y p1) (:x p2) (:y p2) paint))
  ([^Canvas canvas x1 y1 x2 y2 ^Paint paint]
   (.drawLine canvas x1 y1 x2 y2 paint)))

(defn draw-arc
  ([^Canvas canvas r start-angle sweep-angle use-center ^Paint paint]
   (condp instance? r
     IRect (let [r (.toRect ^IRect r)] (.drawArc canvas (:x r) (:y r) (:right r) (:bottom r) start-angle sweep-angle use-center paint))
     RRect (.drawArc canvas (:x r) (:y r) (:right r) (:bottom r) start-angle sweep-angle use-center paint)
     Rect  (.drawArc canvas (:x r) (:y r) (:right r) (:bottom r) start-angle sweep-angle use-center paint)))
  ([^Canvas canvas left top right bottom start-angle sweep-angle use-center ^Paint paint]
   (.drawArc canvas left top right bottom start-angle sweep-angle use-center paint)))

(defn draw-rect [^Canvas canvas r ^Paint paint]
  (condp instance? r
    IRect (.drawRect  canvas (.toRect ^IRect r) paint)
    RRect (.drawRRect canvas r paint)
    Rect  (.drawRect  canvas r paint)))

(defn draw-oval [^Canvas canvas r ^Paint paint]
  (condp instance? r
    IRect (.drawOval canvas (.toRect ^IRect r) paint)
    RRect (.drawOval canvas r paint)
    Rect  (.drawOval canvas r paint)))

(defn draw-circle [^Canvas canvas x y r ^Paint paint]
  (.drawCircle canvas x y r paint))

(def blend-modes
  ":clear replaces destination with zero: fully transparent.
   :src replaces destination.
   :dst preserves destination.
   :src-over source over destination.
   :dst-over destination over source.
   :src-in source trimmed inside destination.
   :dst-in destination trimmed inside source.
   :src-out source trimmed outside destination.
   :dst-out destination trimmed outside source.
   :src-atop source inside destination blended with destination.
   :dst-atop destination inside source blended with source.
   :xor source and destination trimmed outside each other.
   :plus sum colors.
   :modulate product of premultiplied colors, darkens destination.
   :screen multiply inverse of pixels followed by inverting result, brightens destination.
   :overlay multiply or screen depending on destination.
   :darken darker of source and destination.
   :lighten lighter of source and destination.
   :color-dodge brighten destination to reflect source.
   :color-burn darken destination to reflect source.
   :hard-light multiply or screen depending on source.
   :soft-light darken or lighten depending on source.
   :difference subtract darker from lighter with higher contrast.
   :exclusion subtract darker from lighter with lower contrast.
   :multiply multiply source with destination darkening image.
   :hue hue of source with saturation and luminosity of destination.
   :saturation saturation of source with hue and luminosity of destination.
   :color hue and saturation of source with luminosity of destination.
   :luminosity luminosity of source with hue and saturation of destination."
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