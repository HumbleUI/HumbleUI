(in-ns 'io.github.humbleui.ui)

(ui/defcomp button-laf [state child]
  (let [{:hui.button/keys [bg
                           bg-pressed
                           bg-hovered
                           border-radius
                           padding-left
                           padding-top
                           padding-right
                           padding-bottom]} *ctx*]
    [clip-rrect {:radii [border-radius]}
     [rect {:paint (case state
                     :pressed bg-pressed
                     :hovered bg-hovered
                     bg)}
      [padding {:left   padding-left
                :top    padding-top
                :right  padding-right
                :bottom padding-bottom}
       [center
        (if (vector? child)
          child
          [label child])]]]]))

(ui/defcomp button-ctor [opts child]
  (let [*state (or (:*state opts) (signal/signal :default))
        laf    (or (:laf opts) (:hui.button/laf *ctx*) button-laf)]
    {:should-setup?
     (fn [opts' child]
       (not (keys-match? [:*state :laf] opts opts')))
     :render
     (fn [opts child]
       [clickable (assoc opts :*state *state)
        [laf @*state child]])}))

(ui/defcomp toggle-button-ctor [opts child]
  (let [value-pressed   (:value-pressed opts true)
        value-unpressed (:value-unpressed opts)
        *value          (or (:*value opts) (signal/signal value-unpressed))
        *state          (or (:*state opts) (signal/signal :default))
        on-click        (fn [_]
                          (signal/reset-changed! *value
                            (if (= value-pressed @*value)
                              value-unpressed
                              value-pressed)))
        laf             (or (:laf opts) button-laf)]
    {:should-setup?
     (fn [opts' child]
       (not (keys-match? [:value-pressed :value-unpressed :*value :*state :laf] opts opts')))
     :render
     (fn [opts child]
       (let [state @*state
             value @*value]
         [clickable {:*state *state
                     :on-click on-click}
          [laf
           (if (= value-pressed @*value)
             :pressed
             @*state)
           child]]))}))
