(in-ns 'io.github.humbleui.ui)

(core/deftype+ Clickable [*hovered?
                          *pressed?
                          *active?
                          ^:mut clicks
                          ^:mut last-click]
  :extends AWrapperNode
  protocols/IComponent
  (-event-impl [this ctx event]
    (when (= :mouse-move (:event event))
      (set! clicks 0)
      (set! last-click 0))
    
    (let [[_ opts _] (parse-element element)
          *hovered?  (or (:*hovered? opts) *hovered?)
          *pressed?  (or (:*pressed? opts) *pressed?)
          *active?   (or (:*active? opts) *active?)
          hovered?   @*hovered?
          hovered?'  (core/if-some+ [{:keys [x y]} event]
                       (core/rect-contains? rect (core/ipoint x y))
                       hovered?)
          pressed?   @*pressed?
          pressed?'  (if (= :mouse-button (:event event))
                       (if (:pressed? event)
                         hovered?'
                         false)
                       pressed?)
          clicked?   (and hovered?' pressed? (not pressed?'))
          now        (core/now)
          _          (when clicked?
                       (when (> (- now last-click) core/double-click-threshold-ms)
                         (set! clicks 0))
                       (set! clicks (inc clicks))
                       (set! last-click now))
          event'     (cond-> event
                       clicked? (assoc :clicks clicks))
          {:keys [on-click
                  on-click-capture]} opts]
      (signal/reset-changed! *hovered? hovered?')
      (core/eager-or
        (when (and clicked? on-click-capture)
          (on-click-capture event')
          true) ;; was false?
        (or
          (event-child child ctx event')
          (do
            (signal/reset-changed! *pressed? pressed?')
            (signal/reset-changed! *active? (and hovered?' pressed?'))
            (when (and clicked? on-click)
              (on-click event')
              true)))))))

(defn clickable [opts child]
  (map->Clickable
    {:*hovered?  (signal/signal false)
     :*pressed?  (signal/signal false)
     :*active?   (signal/signal false)
     :clicks     0
     :last-click 0}))
