(in-ns 'io.github.humbleui.ui)

(core/deftype+ Clickable [*state
                          ^:mut pressed?
                          ^:mut clicks
                          ^:mut last-click]
  :extends AWrapperNode
  (-event-impl [this ctx event]
    (when (= :mouse-move (:event event))
      (set! clicks 0)
      (set! last-click 0))
    
    (let [{:keys [on-click on-click-capture]} (parse-opts element)
          state     @*state
          hovered?  (= :hovered state)
          hovered?' (core/if-some+ [{:keys [x y]} event]
                      (core/rect-contains? rect (core/ipoint x y))
                      hovered?)
          pressed?' (if (= :mouse-button (:event event))
                      (if (:pressed? event)
                        hovered?'
                        false)
                      pressed?)
          clicked?  (and hovered?' pressed? (not pressed?'))
          now       (core/now)
          _         (when clicked?
                      (when (> (- now last-click) core/double-click-threshold-ms)
                        (set! clicks 0))
                      (set! clicks (inc clicks))
                      (set! last-click now))
          event'    (cond-> event
                      clicked? (assoc :clicks clicks))]
      (core/eager-or
        (when (and clicked? on-click-capture)
          (core/invoke on-click-capture event')
          true) ;; was false?
        (if (event-child child ctx event')
          ;; child have handled this event
          (do
            (signal/reset-changed! *state
              (cond
                hovered?' :hoverer
                :else     :default)))
          ;; we have to handle this event
          (do
            (set! pressed? pressed?')
            (signal/reset-changed! *state
              (cond
                (and hovered?' pressed?') :pressed
                hovered?'                 :hovered
                :else                     :default))
            (when (and clicked? on-click)
              (core/invoke on-click event')
              true))))))
  
  (-should-reconcile? [_this _ctx new-element]
    (opts-match? [:*state] element new-element)))

(defn- clickable-ctor
  "Element that can be clicked. Supports nesting (innermost will be clicked).
   
   Options are:
   
   :on-click         :: (fn [event]), what to do on click
   :on-click-capture :: (fn [event]), what to do on click before children
                        have a chance to handle it
   :*state           :: signal, controls/represent state
   
   Event is map with keys:
   
   :clicks  :: long, number of consequitive clicks
   
   Possible *state values:
   
   :default :: default state
   :hovered :: mouse hovers over object
   :pressed :: mouse is held over object"
  [opts child]
  (map->Clickable
    {:*state     (or
                   (:*state opts)
                   (signal/signal :enabled))
     :pressed?   nil
     :clicks     0
     :last-click 0}))
