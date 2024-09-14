(in-ns 'io.github.humbleui.ui)

(ui/defcomp toggleable-ctor
  "A wrapper that can be toggled on and off. Options are
   
     :value-on  :: any, value to set when button is pressed
     :value-off :: any, value to set when button is unpressed
     :*value    :: signal, controls/represent state
     :on-click  :: (fn [event]), what to do on click
     :on-change :: (fn [value]), what to do when state changes
   
   Child can be a (fn [state]) where state is a set of
     
     :hovered  :: mouse hovers over object
     :pressed  :: mouse is held over object
     :held     :: mouse started over object and is still held
     :selected :: button is currently pressed"
  [opts child-ctor-or-el]
  (let [value-on  (:value-on opts true)
        value-off (:value-off opts)
        *value    (or (:*value opts) (signal value-off))
        on-click  (:on-click opts)
        on-click' (fn [event]
                    (reset-changed! *value
                      (if (= value-on @*value)
                        value-off
                        value-on))
                    (util/invoke on-click event))]
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
       [clickable {:on-click on-click'}
        (if (fn? child-ctor-or-el)
          (fn [state]
            (child-ctor-or-el
              (cond-> state
                (= @*value value-on) (conj :selected))))
          child-ctor-or-el)])}))
