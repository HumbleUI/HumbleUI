(in-ns 'io.github.humbleui.ui)

(core/deftype+ EventListener []
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
          (callback event ctx))
        (event-child child ctx event)
        (when (and (not capture?)
                (= event-type (:event event)))
          (callback event ctx))))))

(defn event-listener-ctor [opts child]
  (map->EventListener {}))

(defn on-key-focused-ctor [opts child]
  (event-listener
    {:event    :key
     :on-event
     (fn [e ctx]
       (when (and (:hui/focused? ctx) (:pressed? e)) ;; FIXME hui/focused?
         (when-some [callback (-> opts :keymap (get (:key e)))]
           (callback)
           true)))
     :capture? true}
    child))

(core/deftype+ KeyListener [on-key-down on-key-up]
  :extends AWrapperNode
  protocols/IComponent
  (-event-impl [_ ctx event]
    (let [[_ opts _] (parse-element element)
          {:keys [on-key-down
                  on-key-up]} opts]
      (or
        (event-child child ctx event)
        (when (= :key (:event event))
          (if (:pressed? event)
            (when on-key-down
              (on-key-down event))
            (when on-key-up
              (on-key-up event))))))))

(defn key-listener-ctor [{:keys [on-key-up on-key-down] :as opts} child]
  (map->KeyListener {}))

(core/deftype+ MouseListener [^:mut over?]
  :extends AWrapperNode
  protocols/IComponent
  (-draw-impl [_ ctx rect canvas]
    (set! over? (core/rect-contains? rect (:mouse-pos ctx)))
    (draw-child child ctx rect canvas))
  
  (-event-impl [_ ctx event]
    (let [[_ opts _] (parse-element element)
          {:keys [on-move
                  on-scroll
                  on-button
                  on-over
                  on-out]} opts]
      (core/when-some+ [{:keys [x y]} event]
        (let [over?' (core/rect-contains? rect (core/ipoint x y))]
          (when (and (not over?) over?' on-over)
            (on-over event))
          (when (and over? (not over?') on-out)
            (on-out event))
          (set! over? over?')))
        
      (core/eager-or
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
        (event-child child ctx event)))))

(defn mouse-listener-ctor [{:keys [on-move on-scroll on-button on-over on-out] :as opts} child]
  (map->MouseListener {}))

(core/deftype+ TextListener []
  :extends AWrapperNode
  protocols/IComponent
  (-event-impl [_ ctx event]
    (let [[_ opts _] (parse-element element)
          {:keys [on-input]} opts]
      (core/eager-or
        (when (= :text-input (:event event))
          (on-input (:text event)))
        (event-child child ctx event)))))

(defn text-listener-ctor [{:keys [on-input]} child]
  (map->TextListener {}))
