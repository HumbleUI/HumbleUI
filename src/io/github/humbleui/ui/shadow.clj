(ns io.github.humbleui.ui.shadow
  (:require
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.ui.canvas :as ui.canvas]
    [io.github.humbleui.ui.dynamic :as dynamic]
    [io.github.humbleui.ui.gap :as gap]
    [io.github.humbleui.ui.rect :as rect]
    [io.github.humbleui.ui.stack :as stack])
  (:import
    [io.github.humbleui.skija Canvas FilterBlurMode ImageFilter MaskFilter Path PathDirection]))

(defn shadow 
  ([opts]
   (shadow opts (gap/gap 0 0)))
  ([{:keys [dx dy blur color fill]
     :or {dx 0, dy 0, blur 0, color 0x80000000}}
    child]
   (dynamic/dynamic ctx [{:keys [scale]} ctx]
     (let [r      (core/radius->sigma (* blur scale))
           shadow (if fill
                    (ImageFilter/makeDropShadow (* dx scale) (* dy scale) r r color)
                    (ImageFilter/makeDropShadowOnly (* dx scale) (* dy scale) r r color))
           paint  (-> (paint/fill (or fill 0xFFFFFFFF))
                    (paint/set-image-filter shadow))]
       (rect/rect paint
         child)))))

(defn shadow-inset [{:keys [dx dy blur color]
                     :or {dx 0, dy 0, blur 0, color 0x80000000}} child]
  (stack/stack
    (ui.canvas/canvas
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
             (.drawPath canvas path paint))))})
    child))
