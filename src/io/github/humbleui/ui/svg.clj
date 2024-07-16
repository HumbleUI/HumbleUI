(in-ns 'io.github.humbleui.ui)
(import '[io.github.humbleui.skija.svg SVGDOM SVGLength SVGPreserveAspectRatio SVGPreserveAspectRatioAlign SVGPreserveAspectRatioScale])

(defn- ^SVGPreserveAspectRatio svg-opts->scaling [opts]
  (let [{:keys [preserve-aspect-ratio xpos ypos scale]
         :or {preserve-aspect-ratio true
              xpos 0.5
              ypos 0.5
              scale :fit}} opts]
    (if preserve-aspect-ratio
      (case [xpos ypos scale]
        [0 0 :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMIN  SVGPreserveAspectRatioScale/MEET)
        [0 0.5 :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMID  SVGPreserveAspectRatioScale/MEET)
        [0 1 :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMAX  SVGPreserveAspectRatioScale/MEET)
        [0.5 0 :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMIN  SVGPreserveAspectRatioScale/MEET)
        [0.5 0.5 :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMID  SVGPreserveAspectRatioScale/MEET)
        [0.5 1 :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMAX  SVGPreserveAspectRatioScale/MEET)
        [1 0 :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMIN  SVGPreserveAspectRatioScale/MEET)
        [1 0.5 :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMID  SVGPreserveAspectRatioScale/MEET)
        [1 1 :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMAX  SVGPreserveAspectRatioScale/MEET)
        [0 0 :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMIN  SVGPreserveAspectRatioScale/SLICE)
        [0 0.5 :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMID  SVGPreserveAspectRatioScale/SLICE)
        [0 1 :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMAX  SVGPreserveAspectRatioScale/SLICE)
        [0.5 0 :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMIN  SVGPreserveAspectRatioScale/SLICE)
        [0.5 0.5 :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMID  SVGPreserveAspectRatioScale/SLICE)
        [0.5 1 :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMAX  SVGPreserveAspectRatioScale/SLICE)
        [1 0 :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMIN  SVGPreserveAspectRatioScale/SLICE)
        [1 0.5 :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMID  SVGPreserveAspectRatioScale/SLICE)
        [1 1 :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMAX  SVGPreserveAspectRatioScale/SLICE))
      (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/NONE SVGPreserveAspectRatioScale/MEET))))

(util/deftype+ SVG [^SVGDOM dom]
  :extends ATerminalNode
  protocols/IComponent
  (-measure-impl [_ _ctx cs]
    cs)
  
  (-draw-impl [_ _ctx bounds viewport ^Canvas canvas]
    (let [root (.getRoot dom)
          {:keys [x y width height]} bounds
          [_ opts _] (parse-element element)
          scaling    (svg-opts->scaling opts)]
      (.setWidth root (SVGLength. width))
      (.setHeight root (SVGLength. height))
      (.setPreserveAspectRatio root scaling)
      (canvas/with-canvas canvas
        (canvas/translate canvas x y)
        (.render dom canvas))))
  
  (-should-reconcile? [_this ctx new-element]
    (opts-match? [:src] element new-element))
  
  (-unmount-impl [this]
    (.close dom)))

(defn- svg-ctor [opts]
  (let [src (util/checked-get opts :src util/slurpable?)
        dom (with-open [data (Data/makeFromBytes (util/slurp-bytes src))]
              (SVGDOM. data))]
    (map->SVG {:dom dom})))
