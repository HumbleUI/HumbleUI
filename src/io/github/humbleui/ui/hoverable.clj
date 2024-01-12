(in-ns 'io.github.humbleui.ui)

(core/deftype+ Hoverable [*hovered?]
  :extends AWrapperNode
  
  protocols/IComponent
  (-event-impl [this ctx event]
    (core/when-some+ [{:keys [x y]} event]
      (let [[_ opts _]                (parse-element element)
            {:keys [on-hover on-out]} opts
            *hovered?                 (or (:*hovered? opts) *hovered?)
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
          false)))))

(defn hoverable
  "Enable the child element to respond to mouse hover events.
  
   Opts are:
   
   :on-hover    :: (fn [event] ...)
   :on-out      :: (fn [event] ...)
   :*hoverable? :: signal"
  ([child]
   (hoverable {} child))
  ([opts child]
   (map->Hoverable {:*hovered? (signal/signal false)})))
