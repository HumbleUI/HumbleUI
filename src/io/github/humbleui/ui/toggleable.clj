(in-ns 'io.github.humbleui.ui)

(ui/defcomp toggleable [opts child-ctor-or-el]
  (let [value-on  (:value-on opts true)
        value-off (:value-off opts)
        *value    (or (:*value opts) (signal/signal value-off))
        on-click  (:on-click opts)
        on-click' (fn [event]
                    (signal/reset-changed! *value
                      (if (= value-on @*value)
                        value-off
                        value-on))
                    (core/invoke on-click event))]
    (when-some [on-change (:on-change opts)]
      (add-watch *value ::on-change
        (fn [_ _ old new]
          (when (not= old new)
            (on-change new)))))
    {:should-setup?
     (fn [opts' child-ctor-or-el]
       (not (keys-match? [:value-on :value-off :*value] opts opts')))
     :after-unmount
     (fn []
       (remove-watch *value ::on-change))
     :render
     (fn [opts child-ctor-or-el]
       (let [value @*value]
         [clickable {:on-click on-click'}
          (if (fn? child-ctor-or-el)
            (fn [state]
              (child-ctor-or-el
                (cond-> state
                  (= value value-on) (conj :selected))))
            child-ctor-or-el)]))}))
