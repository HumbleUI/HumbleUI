(in-ns 'io.github.humbleui.ui)

(def button-bg-pressed
  (paint/fill 0xFFA2C7EE))

(def button-bg-hovered
  (paint/fill 0xFFCFE8FC))

(def button-bg
  (paint/fill 0xFFB2D7FE))

(ui/defcomp button-look [state child]
  [clip-rrect {:radii [4]}
   [rect {:paint (cond
                   (and 
                     (:selected state)
                     (:pressed state)) button-bg-hovered
                   (:selected state)   button-bg-pressed
                   (:pressed state)    button-bg-pressed
                   (:hovered state)    button-bg-hovered
                   :else               button-bg)}
    [padding {:horizontal (* 2 (:leading *ctx*))
              :vertical   (:leading *ctx*)}
     [center
      (if (vector? child)
        child
        [label child])]]]])

(ui/defcomp button-ctor [opts child]
  [clickable opts
   (fn [state]
     [(or (:hui.button/look *ctx*) button-look) state child])])

(ui/defcomp toggle-button-ctor [opts child]
  [toggleable opts
   (fn [state]
     [(or (:hui.button/look *ctx*) button-look) state child])])
