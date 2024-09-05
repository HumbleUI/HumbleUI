(in-ns 'io.github.humbleui.ui)

(util/deftype+ EventListener [event-type
                              callback
                              capture?]
  :extends AWrapperNode
  
  (-event-impl [_ ctx event]
    (or
      (when (and capture?
              (= event-type (:event event)))
        (callback event ctx)) ;; FIXME need context?
      (ui/event child ctx event)
      (when (and (not capture?)
              (= event-type (:event event)))
        (callback event ctx))))
    
  (-reconcile-opts [_ _ new-element]
    (let [opts (parse-opts new-element)]
      (set! event-type (:event opts))
      (set! callback   (:on-event opts))
      (set! capture?   (:capture? opts)))))

(defn event-listener-ctor [opts child]
  (map->EventListener {}))

(defn on-key-focused-ctor [opts child]
  [event-listener-ctor
   {:event :key
    :on-event
    (fn [e ctx]
      (when (and (:hui/focused? ctx) (:pressed? e)) ;; FIXME hui/focused?
        (when-some [callback (-> opts :keymap (get (:key e)))]
          (callback)
          true)))
    :capture? true}
   child])

(util/deftype+ KeyListener [on-key-down
                            on-key-up]
  :extends AWrapperNode
  
  (-event-impl [_ ctx event]
    (or
      (ui/event child ctx event)
      (when (= :key (:event event))
        (if (:pressed? event)
          (when on-key-down
            (on-key-down event))
          (when on-key-up
            (on-key-up event))))))
  
  (-reconcile-opts [_ _ new-element]
    (let [opts (parse-opts new-element)]
      (set! on-key-down (:on-key-down opts))
      (set! on-key-up   (:on-key-up opts)))))

(defn key-listener-ctor [{:keys [on-key-up on-key-down] :as opts} child]
  (map->KeyListener {}))

(util/deftype+ MouseListener [on-move
                              on-scroll
                              on-button
                              on-over
                              on-out
                              over?]
  :extends AWrapperNode

  (-draw-impl [_ ctx bounds container-size viewport canvas]
    (set! over? (util/rect-contains? bounds (:mouse-pos ctx)))
    (draw child ctx bounds container-size viewport canvas))
  
  (-event-impl [_ ctx event]
    (util/when-some+ [{:keys [x y]} event]
      (let [over?' (util/rect-contains? bounds (util/ipoint x y))]
        (when (and (not over?) over?' on-over)
          (on-over event))
        (when (and over? (not over?') on-out)
          (on-out event))
        (set! over? over?')))

    (util/eager-or
      (when (and on-move
              over?
              (= :mouse-move (:event event)))
        (on-move event))
      (when (and on-scroll
              over?
              (= :mouse-scroll (:event event)))
        (on-scroll event))
      (when (and on-button
              over?
              (= :mouse-button (:event event)))
        (on-button event))
      (ui/event child ctx event)))
    
  (-reconcile-opts [_ _ new-element]
    (let [opts (parse-opts new-element)]
      (set! on-move   (:on-move opts))
      (set! on-scroll (:on-scroll opts))
      (set! on-button (:on-button opts))
      (set! on-over   (:on-over opts))
      (set! on-out    (:on-out opts)))))

(defn mouse-listener-ctor [{:keys [on-move on-scroll on-button on-over on-out] :as opts} child]
  (map->MouseListener {}))

(util/deftype+ TextListener [on-input]
  :extends AWrapperNode
  
  (-event-impl [_ ctx event]
    (util/eager-or
      (when (= :text-input (:event event))
        (on-input (:text event)))
      (ui/event child ctx event)))
  
  (-reconcile-opts [_ _ new-element]
    (let [opts (parse-opts new-element)]
      (set! on-input (:on-input opts)))))

(defn text-listener-ctor [{:keys [on-input]} child]
  (map->TextListener {}))
