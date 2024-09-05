(in-ns 'io.github.humbleui.ui)

(defn- slider-value-at [slider x]
  (let [{:keys [*value
                thumb-size
                bounds
                delta-x
                element
                min-value
                max-value
                step]} slider
        {thumb-w :width}  thumb-size
        half-thumb-w      (/ thumb-w 2)
        left              (+ (:x bounds) half-thumb-w)
        width             (- (:width bounds) thumb-w)
        ratio             (util/clamp (/ (- x delta-x left) width) 0 1)
        range             (- max-value min-value)]
    (-> ratio
      (* (quot range step))
      (math/round)
      (* step)
      (+ min-value))))

(util/deftype+ SliderThumb []
  :extends ATerminalNode

  (-measure-impl [_ ctx _cs]
    (let [{:hui.slider/keys [thumb-size]} ctx]
      (util/isize thumb-size thumb-size)))

  (-draw-impl [_ ctx bounds container-size viewport canvas]
    (let [{:hui.slider/keys [fill-thumb
                             stroke-thumb
                             fill-thumb-active
                             stroke-thumb-active]
           :hui/keys        [active?]} ctx
          x (+ (:x bounds) (/ (:width bounds) 2))
          y (+ (:y bounds) (/ (:height bounds) 2))
          r (/ (:height bounds) 2)]
      (with-paint ctx [paint (if active? fill-thumb-active fill-thumb)]
        (canvas/draw-circle canvas x y r paint))
      (with-paint ctx [paint (if active? stroke-thumb-active stroke-thumb)]
        (canvas/draw-circle canvas x y r paint)))))

(util/deftype+ SliderTrack [fill-key]
  :extends ATerminalNode
  protocols/IComponent
  (-measure-impl [_ _ctx cs]
    cs)

  (-draw-impl [_ ctx bounds container-size viewport canvas]
    (let [{:hui.slider/keys [track-height]} ctx
          half-track-height (/ track-height 2)
          x      (- (:x bounds) half-track-height)
          y      (+ (:y bounds) (/ (:height bounds) 2) (- half-track-height))
          w      (+ (:width bounds) track-height)
          r      half-track-height
          rect   (util/rrect-xywh x y w track-height r)]
      (with-paint ctx [paint (ctx fill-key)]
        (canvas/draw-rect canvas rect paint)))))

(util/deftype+ Slider [*value
                       track-active
                       track-inactive
                       thumb
                       thumb-size
                       dragging?
                       delta-x
                       min-value
                       max-value
                       step]
  :extends ATerminalNode
  protocols/IComponent
  (-measure-impl [_ ctx cs]
    (measure thumb ctx cs))
  
  (-draw-impl [_ ctx bounds container-size viewport canvas]
    (set! thumb-size (measure thumb ctx container-size))
    (let [value             @*value
          {left :x
           top  :y
           w    :width}     bounds
          {thumb-w :width
           thumb-h :height} thumb-size
          half-thumb-w      (/ thumb-w 2)
          range             (- max-value min-value)
          ratio             (/ (- value min-value) range)
          thumb-x           (+ left half-thumb-w (* ratio (- w thumb-w)))
          ctx'              (cond-> ctx
                              dragging? (assoc :hui/active? true))]
      (draw track-active   ctx' (util/irect-ltrb (+ left half-thumb-w)    top thumb-x                     (+ top thumb-h)) container-size viewport canvas)
      (draw track-inactive ctx' (util/irect-ltrb thumb-x                  top (+ left w (- half-thumb-w)) (+ top thumb-h)) container-size viewport canvas)
      (draw thumb          ctx' (util/irect-xywh (- thumb-x half-thumb-w) top thumb-w                     thumb-h)         container-size viewport canvas)))
  
  (-event-impl [this _ctx event]
    (util/eager-or
      (when (and
              (= :mouse-button (:event event))
              (= :primary (:button event))
              (:pressed? event))
        (let [value             @*value
              {left :x
               top :y
               width :width}    bounds
              {thumb-w :width
               thumb-h :height} thumb-size
              half-thumb-w      (/ thumb-w 2)
              range             (- max-value min-value)
              ratio             (/ (- value min-value) range)
              thumb-x           (+ left half-thumb-w (* ratio (- width thumb-w)))
              thumb-rect        (util/irect-xywh (- thumb-x half-thumb-w) top thumb-w thumb-h)
              point             (util/ipoint (:x event) (:y event))]
          (cond
            (util/rect-contains? thumb-rect point)
            (do
              (set! dragging? true)
              (set! delta-x (- (:x event) thumb-x))
              true)
            
            (util/rect-contains? bounds point)
            (do
              (set! dragging? true)
              (reset! *value (slider-value-at this (:x event)))
              true))))
    
      (when (and
              dragging?
              (= :mouse-button (:event event))
              (= :primary (:button event))
              (not (:pressed? event)))
        (set! dragging? false)
        (set! delta-x 0)
        true)
      
      (when (and
              dragging?
              (= :mouse-move (:event event)))
        (reset! *value (slider-value-at this (:x event)))
        true)))
  
  (-should-reconcile? [_this _ctx new-element]
    (opts-match? [:*value :track-active :track-inactive :thumb] element new-element))
  
  (-reconcile-opts [_this _ctx new-element]
    (let [opts (parse-opts new-element)]
      (set! min-value (or (util/checked-get-optional opts :min number?) 0))
      (set! max-value (or (util/checked-get-optional opts :max number?) 100))
      (set! step      (or (util/checked-get-optional opts :step number?) 1)))))

(defn- slider-ctor [opts]
  (let [*value         (or (:*value opts) (signal (or (:min opts) 0)))
        track-active   (or (some-> (:track-active opts) make) (map->SliderTrack {:fill-key :hui.slider/fill-track-active}))
        track-inactive (or (some-> (:track-inactive opts) make) (map->SliderTrack {:fill-key :hui.slider/fill-track-inactive}))
        thumb          (or (some-> (:thumb opts) make) (map->SliderThumb {}))]
    (map->Slider
      {:*value         *value
       :track-active   track-active
       :track-inactive track-inactive
       :thumb          thumb
       :dragging?      false
       :delta-x        0})))
