(in-ns 'io.github.humbleui.ui)

(def ^Paint switch-fill-enabled
  (paint/fill 0xFF0080FF))

(def ^Paint switch-fill-disabled
  (paint/fill 0xFFD9D9D9))

(def ^Paint switch-fill-handle
  (paint/fill 0xFFFFFFFF))

(def ^Paint switch-fill-enabled-active
  (paint/fill 0xFF0060E0))

(def ^Paint switch-fill-disabled-active
  (paint/fill 0xFFBBBBBB))

(def ^Paint switch-fill-handle-active
  (paint/fill 0xFFE0E0E0))

(defn- switch-height [ctx]
  (let [cap-height (* (scale) (cap-height))
        extra      (-> cap-height (/ 8) math/ceil (* 4))] ;; half cap-height but increased so that it’s divisible by 4
    (+ cap-height extra)))

(defn- switch-start-animation [switch]
  (let [; ratio = (now - start) / len
        ; start = now - ratio * len
        start  (:animation-start switch)
        len    (:animation-length switch)
        now    (util/now)
        ratio  (min 1 (/ (- now start) len))
        ratio' (- 1 ratio)
        start' (- now (* ratio' len))]
    (protocols/-set! switch :animation-start start')))

(util/deftype+ Switch [animation-length
                       ^:mut on?-cached
                       ^:mut animation-start]
  :extends ATerminalNode
  protocols/IComponent
  (-measure-impl [_ ctx _cs]
    (let [height (switch-height ctx)
          width  (math/round (* height 1.61803))]
      (util/ipoint width height)))
  
  (-draw-impl [this ctx bounds viewport canvas]
    (let [{x :x, y :y, w :width, h :height} bounds
          [_ on? pressed?] element
          _                (when (nil? on?-cached)
                             (set! on?-cached on?))
          _                (when (not= on? on?-cached)
                             (switch-start-animation this)
                             (set! on?-cached on?))
          now              (util/now)
          ratio            (min 1 (/ (- now animation-start) animation-length))
          animating?       (< ratio 1)
          fill             (let [switch-fill-enabled (if pressed?
                                                       switch-fill-enabled-active
                                                       switch-fill-enabled)
                                 switch-fill-disabled (if pressed?
                                                        switch-fill-disabled-active
                                                        switch-fill-disabled)]
                             (condp = [on? animating?]
                               [true  true]  (paint/fill
                                               (Color/makeLerp
                                                 (.getColor switch-fill-enabled)
                                                 (.getColor switch-fill-disabled) ratio))
                               [false true]  (paint/fill
                                               (Color/makeLerp
                                                 (.getColor switch-fill-disabled)
                                                 (.getColor switch-fill-enabled) ratio))
                               [true  false] switch-fill-enabled
                               [false false] switch-fill-disabled))
          padding          (/ h 16)
          handle-r         (-> h (- (* 2 padding)) (/ 2))
          handle-left      (-> x (+ padding) (+ handle-r))
          handle-right     (-> x (+ w) (- padding) (- handle-r))
          handle-x         (if on?
                             (+ (* handle-right ratio) (* handle-left (- 1 ratio)))
                             (+ (* handle-left ratio) (* handle-right (- 1 ratio))))
          handle-y         (-> y (+ padding) (+ handle-r))
          handle-fill      (if pressed?
                             switch-fill-handle-active
                             switch-fill-handle)]
      (canvas/draw-rect canvas (RRect/makeXYWH x y w h (/ h 2)) fill)
      (canvas/draw-circle canvas handle-x handle-y handle-r handle-fill)
      (when animating?
        (util/close fill)
        (window/request-frame (:window ctx))))))

(defn- switch-impl [on? pressed?]
  (map->Switch
    {:animation-length 50
     :animation-start  0}))

(defn- switch-ctor
  ([]
   (switch-ctor {}))
  ([opts]
   [toggleable opts
    (fn [state]
      [switch-impl (boolean (:selected state)) (boolean (:pressed state))])]))
