(in-ns 'io.github.humbleui.ui)

(def button-styles
  {:default
   {:text         {:fill 0xFFFFFFFF}
    :body         {:fill [0.54 0.2 256] :model :oklch}
    :body-hovered {:fill [0.62 0.2 256] :model :oklch}
    :body-pressed {:fill [0.50 0.17 256] :model :oklch}}
   
   :basic
   {:body         [{:fill [0.98 0.003 256] :model :oklch}
                   {:stroke [0.89 0.014 241] :model :oklch}]
    :body-hovered [{:fill [0.95 0.003 256] :model :oklch}
                   {:stroke [0.89 0.014 241] :model :oklch}]
    :body-pressed [{:fill [0.92 0.003 256] :model :oklch}
                   {:stroke [0.89 0.014 241] :model :oklch}]}

   :flat
   {:body-hovered {:fill [0.95 0.003 256] :model :oklch}
    :body-pressed {:fill [0.92 0.003 256] :model :oklch}}
   
   :outlined
   {:text         {:fill   [0.54 0.2 256] :model :oklch}
    :body         [{:fill   0xFFFFFFFF}
                   {:stroke [0.54 0.2 256] :model :oklch}]
    :body-hovered [{:fill   0xFFEEEEEE}
                   {:stroke [0.54 0.2 256] :model :oklch}]
    :body-pressed [{:fill   0xFFDDDDDD}
                   {:stroke [0.54 0.2 256] :model :oklch}]}})

(defn button-look-ctor [style]
  (let [{:keys [text
                text-hovered
                text-pressed
                body
                body-hovered
                body-pressed]} (or
                                 (button-styles style)
                                 (button-styles :basic))]
    (fn [state child]
      (let [cap-height (cap-height)]
        [rect {:radius 4
               :paint  (or
                         (cond
                           (and 
                             (:selected state)
                             (:pressed state)) body-hovered
                           (:selected state)   body-pressed
                           (:pressed state)    body-pressed
                           (:hovered state)    body-hovered)
                         body)}
         [padding {:horizontal (* 2 cap-height)
                   :vertical   cap-height}
          [center
           [with-context {:paint (or
                                   (cond
                                     (and 
                                       (:selected state)
                                       (:pressed state)) text-hovered
                                     (:selected state)   text-pressed
                                     (:pressed state)    text-pressed
                                     (:hovered state)    text-hovered)
                                   text
                                   (:paint *ctx*))}
            child]]]]))))

(defcomp button-ctor
  "A button. Options are
   
     :on-click         :: (fn [event]), what to do on click
     :on-click-capture :: (fn [event]), what to do on click before children
                                        have a chance to handle it
     :*state           :: signal, controls/represent state
     :style            :: :default | :basic | :flat | :outlined"
  ([child]
   (button-ctor {} child))
  ([opts child]
   [clickable opts
    (fn [state]
      [(or
         (:hui.button/look *ctx*)
         (button-look-ctor (:style opts)))
       state child])]))

(defcomp toggle-button-ctor
  "A button that can be toggled on and off. Options are
   
     :value-on  :: any          - value to set when button is pressed
     :value-off :: any          - value to set when button is unpressed
     :*value    :: signal       - controls/represent state
     :on-click  :: (fn [event]) - what to do on click
     :on-change :: (fn [value]) - what to do when state changes
     :style     :: :default | :basic | :flat | :outlined"
  ([child]
   (toggle-button-ctor {} child))
  ([opts child]
   [toggleable opts
    (fn [state]
      [(or
         (:hui.button/look *ctx*)
         (button-look-ctor (:style opts)))
       state (if (fn? child) (child state) child)])]))
