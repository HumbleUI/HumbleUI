(in-ns 'io.github.humbleui.ui)

(import '[io.github.humbleui.skija FilterBlurMode ImageFilter MaskFilter Path PathDirection])

(defn shadow-ctor
  ([opts]
   (shadow-ctor opts [gap]))
  ([opts child]
   (let [opts   (merge {:dx 0
                        :dy 0
                        :blur 0
                        :color (unchecked-int 0x80000000)} opts)
         {:keys [dx dy blur color fill]} opts
         {:keys [scale]} *ctx*
         r      (core/radius->sigma (* blur scale))
         shadow (if fill
                  (ImageFilter/makeDropShadow (* dx scale) (* dy scale) r r color)
                  (ImageFilter/makeDropShadowOnly (* dx scale) (* dy scale) r r color))
         paint  (-> (paint/fill (or fill 0xFFFFFFFF))
                  (paint/set-image-filter shadow))]
     {:should-setup?
      (fn [opts' child-ctor-or-el]
        (not= opts opts'))
      :render
      (fn
        ([_]
         [rect {:paint paint}
          [gap]])
        ([_ child]
         [rect {:paint paint}
          child]))})))

(defn shadow-inset-ctor [opts child]
  (let [{:keys [dx dy blur color]
         :or {dx 0
              dy 0
              blur 0
              color (unchecked-int 0x80000000)}} opts]
    [stack
     child
     [canvas
      {:on-paint
       (fn [ctx ^Canvas canvas size]
         (let [{:keys [width height]} size
               {:keys [scale]} ctx
               blur'  (* blur scale)
               inner  (core/rect-ltrb 0 0 width height)
               extra  (+ blur'
                        (max
                          (abs (* dx scale))
                          (abs (* dy scale))))
               outer  (core/rect-ltrb (- extra) (- extra) (+ width extra) (+ height extra))]
           (with-open [paint  (paint/fill color)
                       filter (MaskFilter/makeBlur FilterBlurMode/NORMAL (core/radius->sigma blur'))
                       path   (Path.)]
             (.addRect path outer)
             (.addRect path inner PathDirection/COUNTER_CLOCKWISE)
             (paint/set-mask-filter paint filter)
             (canvas/translate canvas (* dx scale) (* dy scale))
             (.drawPath canvas path paint))))}]]))
