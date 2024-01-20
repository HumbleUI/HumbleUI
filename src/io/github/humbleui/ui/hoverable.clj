(in-ns 'io.github.humbleui.ui)

(core/deftype+ Hoverable [*hovered?]
  :extends AWrapperNode
  
  protocols/IComponent
  (-event-impl [this ctx event]
    (core/when-some+ [{:keys [x y]} event]
      (let [{:keys [on-hover on-out]} (parse-opts element)
            hovered?                  (core/rect-contains? rect (core/ipoint x y))]
        (cond
          (and @*hovered? (not hovered?))
          (do
            (signal/reset! *hovered? false)
            (core/invoke on-hover event)
            true)
          
          (and (not @*hovered?) hovered?)
          (do
            (signal/reset! *hovered? true)
            (core/invoke on-out event)
            true)
          
          :else
          false))))
  
  (-should-reconcile? [_this _ctx new-element]
    (opts-match? [:*hovered?] element new-element)))

(defn hoverable
  "Enable the child element to respond to mouse hover events.
  
   Opts are:
   
   :on-hover    :: (fn [event] ...)
   :on-out      :: (fn [event] ...)
   :*hoverable? :: signal"
  ([child]
   (hoverable {} child))
  ([opts child]
   (map->Hoverable
     {:*hovered? (or (:*hovered? opts) (signal/signal false))})))
