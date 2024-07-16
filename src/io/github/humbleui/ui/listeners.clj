(in-ns 'io.github.humbleui.ui)

(util/deftype+ EventListener []
  :extends AWrapperNode
  protocols/IComponent
  (-event-impl [_ ctx event]
    (let [[_ opts _] (parse-element element)
          {event-type :event
           callback   :on-event
           capture?   :capture?} opts]
      (or
        (when (and capture?
                (= event-type (:event event)))
          (callback event ctx)) ;; FIXME need context?
        (ui/event child ctx event)
        (when (and (not capture?)
                (= event-type (:event event)))
          (callback event ctx))))))

(defn event-listener-ctor [opts child]
  (map->EventListener {}))

(defn on-key-focused-ctor [opts child]
  [event-listener-ctor
   {:event    :key
    :on-event
    (fn [e ctx]
      (when (and (:hui/focused? ctx) (:pressed? e)) ;; FIXME hui/focused?
        (when-some [callback (-> opts :keymap (get (:key e)))]
          (callback)
          true)))
    :capture? true}
   child])

(util/deftype+ KeyListener [on-key-down on-key-up]
  :extends AWrapperNode
  protocols/IComponent
  (-event-impl [_ ctx event]
    (let [[_ opts _] (parse-element element)
          {:keys [on-key-down
                  on-key-up]} opts]
      (or
        (ui/event child ctx event)
        (when (= :key (:event event))
          (if (:pressed? event)
            (when on-key-down
              (on-key-down event))
            (when on-key-up
              (on-key-up event))))))))

(defn key-listener-ctor [{:keys [on-key-up on-key-down] :as opts} child]
  (map->KeyListener {}))

(util/deftype+ MouseListener [^:mut over?]
  :extends AWrapperNode
  protocols/IComponent
  (-draw-impl [_ ctx bounds viewport canvas]
    (set! over? (util/rect-contains? bounds (:mouse-pos ctx)))
    (draw child ctx bounds viewport canvas))
  
  (-event-impl [_ ctx event]
    (let [[_ opts _] (parse-element element)
          {:keys [on-move
                  on-scroll
                  on-button
                  on-over
                  on-out]} opts]
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
        (ui/event child ctx event)))))

(defn mouse-listener-ctor [{:keys [on-move on-scroll on-button on-over on-out] :as opts} child]
  (map->MouseListener {}))

(util/deftype+ TextListener []
  :extends AWrapperNode
  protocols/IComponent
  (-event-impl [_ ctx event]
    (let [[_ opts _] (parse-element element)
          {:keys [on-input]} opts]
      (util/eager-or
        (when (= :text-input (:event event))
          (on-input (:text event)))
        (ui/event child ctx event)))))

(defn text-listener-ctor [{:keys [on-input]} child]
  (map->TextListener {}))
