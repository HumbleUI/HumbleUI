(in-ns 'io.github.humbleui.ui)

(core/deftype+ Hoverable [*state]
  :extends AWrapperNode
  (-event-impl [this ctx event]
    (core/when-some+ [{:keys [x y]} event]
      (let [{:keys [on-hover on-out]} (parse-opts element)
            state     *state
            hovered?  (= :hovered state)
            hovered?' (core/rect-contains? rect (core/ipoint x y))]
        (cond
          (and (not hovered?) hovered?')
          (do
            (reset! *state :hovered)
            (core/invoke on-hover event)
            true)
          
          (and hovered? (not hovered?'))
          (do
            (reset! *state :default)
            (core/invoke on-out event)
            true)
          
          :else
          false))))
  
  (-should-reconcile? [_this _ctx new-element]
    (opts-match? [:*state] element new-element)))

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
                (signal/signal :default))})))
