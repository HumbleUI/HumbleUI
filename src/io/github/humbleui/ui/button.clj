(in-ns 'io.github.humbleui.ui)

(defn button [opts child]
  (let [*hovered? (or (:*hovered? opts) (signal/signal false))
        *active?  (or (:*active? opts) (signal/signal false))]
    (fn [opts child]
      (let [{:hui.button/keys [bg
                               bg-active
                               bg-hovered
                               border-radius
                               padding-left
                               padding-top
                               padding-right
                               padding-bottom]} *ctx*]
        [clickable (assoc opts
                     :*hovered? *hovered?
                     :*active?  *active?)
         [clip-rrect {:radii [border-radius]}
          [rect {:paint (cond
                          @*active?  bg-active
                          @*hovered? bg-hovered
                          :else      bg)}
           [padding {:left   padding-left
                     :top    padding-top
                     :right  padding-right
                     :bottom padding-bottom}
            [center
             child]]]]]))))
