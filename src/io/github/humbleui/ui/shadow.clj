(in-ns 'io.github.humbleui.ui)

(import '[io.github.humbleui.skija FilterBlurMode ImageFilter MaskFilter Path PathDirection])

(defn shadow-ctor
  ([opts]
   (shadow-ctor opts [gap]))
  ([opts child]
   (let [paint-fn (util/memo-fn [{:keys [dx dy blur color fill]
                                  :or {dx 0 dy 0 blur 0 color (unchecked-int 0x80000000)}}
                                 {:keys [scale]}]
                    (let [r      (util/radius->sigma (* blur scale))
                          shadow (if fill
                                   (ImageFilter/makeDropShadow (* dx scale) (* dy scale) r r color)
                                   (ImageFilter/makeDropShadowOnly (* dx scale) (* dy scale) r r color))
                          paint  (-> (paint {:fill (or fill 0xFFFFFFFF)})
                                   (.setImageFilter shadow))]
                      paint))]
     (fn render
       ([opts]
        (render opts [gap]))
       ([opts child]
        [rect {:paint (paint-fn opts *ctx*)}
         child])))))

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
               inner  (util/rect-ltrb 0 0 width height)
               extra  (+ blur'
                        (max
                          (abs (* dx scale))
                          (abs (* dy scale))))
               outer  (util/rect-ltrb (- extra) (- extra) (+ width extra) (+ height extra))]
           (with-open [paint  (ui/paint {:fill color})
                       filter (MaskFilter/makeBlur FilterBlurMode/NORMAL (util/radius->sigma blur'))
                       path   (Path.)]
             (.addRect path outer)
             (.addRect path inner PathDirection/COUNTER_CLOCKWISE)
             (.setMaskFilter paint filter)
             (canvas/translate canvas (* dx scale) (* dy scale))
             (.drawPath canvas path paint))))}]]))
