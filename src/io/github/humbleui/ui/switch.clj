(in-ns 'io.github.humbleui.ui)

(def switch-fill-enabled
  0xFF0080FF)

(def switch-fill-disabled
  0xFFD9D9D9)

(def switch-fill-handle
  0xFFFFFFFF)

(def switch-fill-enabled-active
  0xFF0060E0)

(def switch-fill-disabled-active
  0xFFBBBBBB)

(def switch-fill-handle-active
  0xFFE0E0E0)

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
                       on?
                       pressed?
                       animation-start]
  :extends ATerminalNode

  (-measure-impl [_ ctx _cs]
    (let [height (switch-height ctx)
          width  (math/round (* height 1.61803))]
      (util/ipoint width height)))
  
  (-draw-impl [this ctx bounds container-size viewport canvas]
    (let [{x :x, y :y, w :width, h :height} bounds
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
                               [true  true]  (Color/makeLerp
                                               (unchecked-int switch-fill-enabled)
                                               (unchecked-int switch-fill-disabled)
                                               ratio)
                               [false true]  (Color/makeLerp
                                               (unchecked-int switch-fill-disabled)
                                               (unchecked-int switch-fill-enabled)
                                               ratio)
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
      (with-paint ctx [paint {:fill fill}]
        (canvas/draw-rect canvas (RRect/makeXYWH x y w h (/ h 2)) paint))
      (with-paint ctx [paint {:fill handle-fill}]
        (canvas/draw-circle canvas handle-x handle-y handle-r paint))
      (when animating?
        (window/request-frame (:window ctx)))))
  
  (-reconcile-opts [this _ctx new-element]
    (let [[_ on?' pressed?'] new-element]
      (when (and (some? on?) (not= on?' on?))
        (switch-start-animation this))
      (set! on? on?')
      (set! pressed? pressed?'))))

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
