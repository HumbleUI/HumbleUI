(in-ns 'io.github.humbleui.ui)

(core/deftype+ Focusable [^:mut focused]
  :extends AWrapperNode  
  protocols/IComponent
  (-context [_ ctx]
    (cond-> ctx
      focused (assoc :hui/focused? true)))
  
  (-draw-impl [this ctx rect canvas]
    (some-> (::*focused ctx)
      (cond->
        focused (vswap! conj this)))
    (draw-child child (protocols/-context this ctx) rect canvas))
  
  (-event-impl [this ctx event]
    (core/eager-or
      (when (and
              (= :mouse-button (:event event))
              (:pressed? event)
              (not focused)
              (core/rect-contains? rect (core/ipoint (:x event) (:y event))))
        (set! focused (core/now))
        (invoke-callback this :on-focus)
        true)
      (let [event' (cond-> event
                     focused (assoc :focused? true))]
        (event-child child (protocols/-context this ctx) event'))))
  
  (-child-elements [this ctx new-element]
    (let [[_ _ [child-ctor-or-el]] (parse-element new-element)]
      (if (fn? child-ctor-or-el)
        [[child-ctor-or-el (if focused #{:focused} #{})]]
        [child-ctor-or-el]))))

(defn focusable-ctor
  ([child]
   (map->Focusable {}))
  ([{:keys [focused on-focus on-blur] :as opts} child]
   (map->Focusable {:focused focused})))

(defn focused [this ctx]
  (let [*acc (volatile! [])]
    (iterate-child this ctx
      (fn [comp]
        (when (and (instance? Focusable comp) (:focused comp))
          (vswap! *acc conj comp)
          false)))
    @*acc))

(core/deftype+ FocusController []
  :extends AWrapperNode
  protocols/IComponent
  (-draw-impl [_ ctx rect canvas]
    (let [*focused (volatile! [])
          ctx'     (assoc ctx ::*focused *focused)
          res      (draw-child child ctx' rect canvas)
          focused  (sort-by :focused @*focused)]
      (doseq [comp (butlast focused)]
        (core/set!! comp :focused nil)
        (invoke-callback comp :on-blur))
      res))
  
  (-event-impl [this ctx event]
    (if (and
          (= :mouse-button (:event event))
          (:pressed? event)
          (core/rect-contains? rect (core/ipoint (:x event) (:y event))))
      (let [focused-before (focused this ctx)
            res            (event-child child ctx event)
            focused-after  (focused this ctx)]
        (when (< 1 (count focused-after))
          (doseq [comp focused-before]
            (core/set!! comp :focused nil)
            (invoke-callback comp :on-blur)))
        (or
          res
          (< 1 (count focused-after))))
      (event-child child ctx event))))

(defn focus-prev [this ctx]
  (let [*prev    (volatile! nil)
        *focused (volatile! nil)]
    (iterate-child this ctx
      (fn [comp]
        (when (instance? Focusable comp)
          (if (:focused comp)
            (do
              (vreset! *focused comp)
              (some? @*prev))
            (do
              (vreset! *prev comp)
              false)))))
    (when-some [focused @*focused]
      (core/set!! focused :focused nil)
      (invoke-callback focused :on-blur))
    (when-some [prev @*prev]
      (core/set!! prev :focused (core/now))
      (invoke-callback prev :on-focus))))

(defn focus-next [this ctx]
  (let [*first   (volatile! nil)
        *focused (volatile! nil)
        *next    (volatile! nil)]
    (iterate-child this ctx
      (fn [comp]
        (when (instance? Focusable comp)
          (when (nil? @*first)
            (vreset! *first comp)
            false)
          (if (:focused comp)
            (do
              (vreset! *focused comp)
              false)
            (when @*focused
              (vreset! *next comp)
              true)))))
    (when-some [focused @*focused]
      (core/set!! focused :focused nil)
      (invoke-callback focused :on-blur))
    (when-some [next (or @*next @*first)]
      (core/set!! next :focused (core/now))
      (invoke-callback next :on-focus))))

(defn focus-controller-impl [child]
  (map->FocusController {}))

(defcomp focus-controller-ctor [child]
  [event-listener
   {:event    :key
    :on-event (fn [e ctx]
                (when (and
                        (:pressed? e)
                        (= :tab (:key e)))
                  (if (:shift (:modifiers e))
                    (focus-prev (-> &node :child :child :child) ctx)
                    (focus-next (-> &node :child :child :child) ctx))
                  true))
    :capture? true}
   [focus-controller-impl
    child]])
