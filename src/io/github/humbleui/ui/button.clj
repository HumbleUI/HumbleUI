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
               :paint  (cond
                         (and 
                           (:selected state)
                           (:pressed state)) (or body-hovered body)
                         (:selected state)   (or body-pressed body)
                         (:pressed state)    (or body-pressed body)
                         (:hovered state)    (or body-hovered body)
                         :else               body)}
         [padding {:horizontal (* 2 cap-height)
                   :vertical   cap-height}
          [center
           [with-context {:fill-text (or text
                                       (:fill-text *ctx*))}
            child]]]]))))

(defcomp button-ctor
  ([child]
   (button-ctor {} child))
  ([opts child]
   [clickable opts
    (fn [state]
      [(or
         (:hui.button/look *ctx*)
         (button-look-ctor (:style opts)))
       state child])]))

(defcomp toggle-button-ctor [opts child]
  [toggleable opts
   (fn [state]
     [(or
        (:hui.button/look *ctx*)
        (button-look-ctor (:style opts)))
      state child])])
