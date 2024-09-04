(in-ns 'io.github.humbleui.ui)

(util/deftype+ Focusable [^:mut focused]
  :extends AWrapperNode  

  (-context [_ ctx]
    (cond-> ctx
      focused (assoc :hui/focused? true)))
  
  (-draw-impl [this ctx bounds container-size viewport canvas]
    (some-> (::*focused ctx)
      (cond->
        focused (vswap! conj this)))
    (draw child ctx bounds container-size viewport canvas))
  
  (-event-impl [this ctx event]
    (util/eager-or
      (when (and
              (= :mouse-button (:event event))
              (:pressed? event)
              (not focused)
              (util/rect-contains? bounds (util/ipoint (:x event) (:y event))))
        (set! focused (util/now))
        (invoke-callback this :on-focus)
        true)
      (let [event' (cond-> event
                     focused (assoc :focused? true))
            ctx'   (cond-> ctx
                     focused (assoc :hui/focused? true))]
        (ui/event child ctx' event'))))
  
  (-child-elements [this ctx new-element]
    (let [[_ _ [child-ctor-or-el]] (parse-element new-element)]
      (if (fn? child-ctor-or-el)
        [[child-ctor-or-el (if focused #{:focused} #{})]]
        [child-ctor-or-el]))))

(defn focusable
  ([child]
   (map->Focusable {}))
  ([{:keys [focused on-focus on-blur] :as opts} child]
   (map->Focusable {:focused focused})))

(defn focused [this ctx]
  (let [*acc (volatile! [])]
    (iterate this ctx
      (fn [comp]
        (when (and (instance? Focusable comp) (:focused comp))
          (vswap! *acc conj comp)
          false)))
    @*acc))

(declare focus-prev focus-next)

(util/deftype+ FocusController []
  :extends AWrapperNode

  (-draw-impl [_ ctx bounds container-size viewport canvas]
    (let [*focused (volatile! [])
          ctx'     (assoc ctx ::*focused *focused)
          res      (draw child ctx' bounds container-size viewport canvas)
          focused  (sort-by :focused @*focused)]
      (doseq [comp (butlast focused)]
        (util/set!! comp :focused nil)
        (invoke-callback comp :on-blur))
      res))
  
  (-event-impl [this ctx event]
    (util/cond+
      ;; tab key
      (and
        (= :key (:event event))
        (= :tab (:key event))
        (:pressed? event))
      (do
        (if (:shift (:modifiers event))
          (focus-prev (-> this :child :child) ctx)
          (focus-next (-> this :child :child) ctx))
        true)
    
      ;; click
      (and
        (= :mouse-button (:event event))
        (:pressed? event))
      (let [point          (util/ipoint (:x event) (:y event))
            focused-before (focused this ctx)
            res            (ui/event child ctx event)
            focused-after  (focused this ctx)]
        (util/cond+
          ;; unfocus previous selections
          (< 1 (count focused-after))
          (do
            (doseq [comp focused-before]
              (util/set!! comp :focused nil)
              (invoke-callback comp :on-blur))
            true)
            
          ;; unfocus if clicked outside
          (and
            (= focused-before focused-after)
            :let [comp (first focused-after)]
            comp
            (not (util/rect-contains? (:bounds comp) point)))
          (do
            (util/set!! comp :focused nil)
            (invoke-callback comp :on-blur)
            true)))

      :else
      (ui/event child ctx event))))

(defn focus-prev [this ctx]
  (let [*prev    (volatile! nil)
        *focused (volatile! nil)]
    (iterate this ctx
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
      (util/set!! focused :focused nil)
      (invoke-callback focused :on-blur))
    (when-some [prev @*prev]
      (util/set!! prev :focused (util/now))
      (invoke-callback prev :on-focus))))

(defn focus-next [this ctx]
  (let [*first   (volatile! nil)
        *focused (volatile! nil)
        *next    (volatile! nil)]
    (iterate this ctx
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
      (util/set!! focused :focused nil)
      (invoke-callback focused :on-blur))
    (when-some [next (or @*next @*first)]
      (util/set!! next :focused (util/now))
      (invoke-callback next :on-focus))))

(defn focus-controller-impl [child]
  (map->FocusController {}))

(defcomp focus-controller [child]
  (map->FocusController {}))
