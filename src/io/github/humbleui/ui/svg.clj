(in-ns 'io.github.humbleui.ui)

(import '[io.github.humbleui.skija.svg SVGDOM SVGLength SVGPreserveAspectRatio SVGPreserveAspectRatioAlign SVGPreserveAspectRatioScale])

(defn- ^SVGPreserveAspectRatio svg-opts->scaling [opts]
  (let [{:keys [preserve-aspect-ratio x y scale]
         :or {preserve-aspect-ratio true
              scale :fit}} opts
        x (case x
            0   :left
            0.0 :left
            nil :center
            0.5 :center
            1   :right
            1.0 :right
            x)
        y (case y
            0   :top
            0.0 :top
            nil :center
            0.5 :center
            1   :bottom
            1.0 :bottom
            y)]
    (if preserve-aspect-ratio
      (condp = [x y scale]
        [:left   :top    :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMIN  SVGPreserveAspectRatioScale/MEET)
        [:left   :center :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMID  SVGPreserveAspectRatioScale/MEET)
        [:left   :bottom :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMAX  SVGPreserveAspectRatioScale/MEET)
        [:center :top    :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMIN  SVGPreserveAspectRatioScale/MEET)
        [:center :center :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMID  SVGPreserveAspectRatioScale/MEET)
        [:center :bottom :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMAX  SVGPreserveAspectRatioScale/MEET)
        [:right  :top    :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMIN  SVGPreserveAspectRatioScale/MEET)
        [:right  :center :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMID  SVGPreserveAspectRatioScale/MEET)
        [:right  :bottom :fit]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMAX  SVGPreserveAspectRatioScale/MEET)
        [:left   :top    :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMIN  SVGPreserveAspectRatioScale/SLICE)
        [:left   :center :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMID  SVGPreserveAspectRatioScale/SLICE)
        [:left   :bottom :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMAX  SVGPreserveAspectRatioScale/SLICE)
        [:center :top    :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMIN  SVGPreserveAspectRatioScale/SLICE)
        [:center :center :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMID  SVGPreserveAspectRatioScale/SLICE)
        [:center :bottom :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMAX  SVGPreserveAspectRatioScale/SLICE)
        [:right  :top    :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMIN  SVGPreserveAspectRatioScale/SLICE)
        [:right  :center :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMID  SVGPreserveAspectRatioScale/SLICE)
        [:right  :bottom :fill] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMAX  SVGPreserveAspectRatioScale/SLICE))
      (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/NONE SVGPreserveAspectRatioScale/MEET))))

(util/deftype+ SVG [^SVGDOM dom
                    scaling]
  :extends ATerminalNode
  protocols/IComponent
  (-measure-impl [_ _ctx cs]
    cs)
  
  (-draw-impl [_ _ctx bounds container-size viewport ^Canvas canvas]
    (let [root (.getRoot dom)
          {:keys [x y width height]} bounds]
      (.setWidth root (SVGLength. width))
      (.setHeight root (SVGLength. height))
      (.setPreserveAspectRatio root scaling)
      (canvas/with-canvas canvas
        (canvas/translate canvas x y)
        (.render dom canvas))))
  
  (-should-reconcile? [_this ctx new-element]
    (opts-match? [:src] element new-element))
  
  (-reconcile-opts [_thix _ctx new-element]
    (let [opts (parse-opts new-element)]
      (set! scaling (svg-opts->scaling opts))))
  
  (-unmount-impl [this]
    (.close dom)))

(defn- svg-ctor [opts]
  (let [src (util/checked-get opts :src util/slurpable?)
        dom (with-open [data (Data/makeFromBytes (util/slurp-bytes src))]
              (SVGDOM. data))]
    (map->SVG {:dom dom})))
