(in-ns 'io.github.humbleui.ui)

(def button-bg-pressed
  (paint/fill 0xFFA2C7EE))

(def button-bg-hovered
  (paint/fill 0xFFCFE8FC))

(def button-bg
  (paint/fill 0xFFB2D7FE))

(ui/defcomp button-look [state child]
  [clip-rrect {:radii [4]}
   [rect {:paint (case state
                   :selected button-bg-pressed
                   :pressed  button-bg-pressed
                   :hovered  button-bg-hovered
                   #_else    button-bg)}
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

(ui/defcomp toggleable [opts child-ctor-or-el]
  (let [value-pressed   (:value-pressed opts true)
        value-unpressed (:value-unpressed opts)
        *value          (or (:*value opts) (signal/signal value-unpressed))
        on-click        (fn [_]
                          (signal/reset-changed! *value
                            (if (= value-pressed @*value)
                              value-unpressed
                              value-pressed)))]
    {:should-setup?
     (fn [opts' child-ctor-or-el]
       (not (keys-match? [:value-pressed :value-unpressed :*value] opts opts')))
     :render
     (fn [opts child-ctor-or-el]
       (let [value @*value]
         [clickable {:on-click on-click}
          (if (fn? child-ctor-or-el)
            (fn [state]
              (child-ctor-or-el (if (= value value-pressed) :selected state)))
            child-ctor-or-el)]))}))

(ui/defcomp toggle-button-ctor [opts child]
  [toggleable opts
   (fn [state]
     [(or (:hui.button/look *ctx*) button-look) state child])])
