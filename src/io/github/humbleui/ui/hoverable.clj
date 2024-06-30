(in-ns 'io.github.humbleui.ui)

(core/deftype+ Hoverable [*state]
  :extends AWrapperNode
  (-event-impl [this ctx event]
    (core/eager-or
      (core/when-some+ [{:keys [x y]} event]
        (let [state     @*state
              hovered?  (:hovered state)
              hovered?' (core/rect-contains? rect (core/ipoint x y))]
          (cond
            (and (not hovered?) hovered?')
            (do
              (reset! *state #{:hovered})
              (force-render this (:window ctx))
              (invoke-callback this :on-hover event)
              true)
          
            (and hovered? (not hovered?'))
            (do
              (reset! *state #{})
              (force-render this (:window ctx))
              (invoke-callback this :on-out event)
              true)
          
            :else
            false)))
      (event-child child ctx event)))
  
  (-should-reconcile? [_this _ctx new-element]
    (opts-match? [:*state] element new-element))
  
  (-child-elements [this ctx new-element]
    (let [[_ _ [child-ctor-or-el]] (parse-element new-element)]
      (if (fn? child-ctor-or-el)
        [[child-ctor-or-el @*state]]
        [child-ctor-or-el]))))

(defn- hoverable-ctor
  "Enable the child element to respond to mouse hover events.
  
   Optsions are:
   
   :on-hover :: (fn [event])
   :on-out   :: (fn [event])
   :*state   :: signal
   
   Possible *state values:
   
   :default :: default state
   :hovered :: mouse hovers over object"
  ([child]
   (hoverable-ctor {} child))
  ([opts child]
   (map->Hoverable
     {:*state (or
                (:*state opts)
                (signal/signal #{}))})))
