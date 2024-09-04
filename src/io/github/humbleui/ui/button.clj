(in-ns 'io.github.humbleui.ui)

(def button-bg-pressed
  {:fill 0xFFA2C7EE})

(def button-bg-hovered
  {:fill 0xFFCFE8FC})

(def button-bg
  {:fill 0xFFB2D7FE})

(ui/defcomp button-look-ctor [state child]
  (let [cap-height (cap-height)]
    [clip {:radius 4}
     [rect {:paint (cond
                     (and 
                       (:selected state)
                       (:pressed state)) button-bg-hovered
                     (:selected state)   button-bg-pressed
                     (:pressed state)    button-bg-pressed
                     (:hovered state)    button-bg-hovered
                     :else               button-bg)}
      [padding {:horizontal (* 2 cap-height)
                :vertical   cap-height}
       [center
        child]]]]))

(ui/defcomp button-ctor
  ([child]
   (button-ctor {} child))
  ([opts child]
   [clickable opts
    (fn [state]
      [(or (:hui.button/look *ctx*) button-look-ctor) state child])]))

(ui/defcomp toggle-button-ctor [opts child]
  [toggleable opts
   (fn [state]
     [(or (:hui.button/look *ctx*) button-look-ctor) state child])])
