(in-ns 'io.github.humbleui.ui)
(import '[io.github.humbleui.skija.svg SVGDOM SVGLength SVGPreserveAspectRatio SVGPreserveAspectRatioAlign SVGPreserveAspectRatioScale])

(defn- ^SVGPreserveAspectRatio svg-opts->scaling [opts]
  (let [{:keys [preserve-aspect-ratio xpos ypos scale]
         :or {preserve-aspect-ratio true
              xpos :mid
              ypos :mid
              scale :meet}} opts]
    (if preserve-aspect-ratio
      (case [xpos ypos scale]
        [:min :min :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMIN  SVGPreserveAspectRatioScale/MEET)
        [:min :mid :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMID  SVGPreserveAspectRatioScale/MEET)
        [:min :max :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMAX  SVGPreserveAspectRatioScale/MEET)
        [:mid :min :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMIN  SVGPreserveAspectRatioScale/MEET)
        [:mid :mid :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMID  SVGPreserveAspectRatioScale/MEET)
        [:mid :max :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMAX  SVGPreserveAspectRatioScale/MEET)
        [:max :min :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMIN  SVGPreserveAspectRatioScale/MEET)
        [:max :mid :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMID  SVGPreserveAspectRatioScale/MEET)
        [:max :max :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMAX  SVGPreserveAspectRatioScale/MEET)
        [:min :min :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMIN  SVGPreserveAspectRatioScale/SLICE)
        [:min :mid :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMID  SVGPreserveAspectRatioScale/SLICE)
        [:min :max :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMAX  SVGPreserveAspectRatioScale/SLICE)
        [:mid :min :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMIN  SVGPreserveAspectRatioScale/SLICE)
        [:mid :mid :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMID  SVGPreserveAspectRatioScale/SLICE)
        [:mid :max :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMAX  SVGPreserveAspectRatioScale/SLICE)
        [:max :min :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMIN  SVGPreserveAspectRatioScale/SLICE)
        [:max :mid :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMID  SVGPreserveAspectRatioScale/SLICE)
        [:max :max :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMAX  SVGPreserveAspectRatioScale/SLICE))
      (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/NONE SVGPreserveAspectRatioScale/MEET))))

(core/deftype+ SVG [^SVGDOM dom]
  :extends ATerminalNode
  protocols/IComponent
  (-measure-impl [_ _ctx cs]
    cs)
  
  (-draw-impl [_ _ctx rect ^Canvas canvas]
    (let [root (.getRoot dom)
          {:keys [x y width height]} rect
          [_ opts _] (parse-element element)
          scaling    (svg-opts->scaling opts)]
      (.setWidth root (SVGLength. width))
      (.setHeight root (SVGLength. height))
      (.setPreserveAspectRatio root scaling)
      (canvas/with-canvas canvas
        (canvas/translate canvas x y)
        (.render dom canvas))))
  
  (-should-reconcile? [_this ctx new-element]
    (let [[_ _ [src]] (parse-element element)
          [_ _ [new-src]] (parse-element new-element)]
      (= src new-src)))
  
  (-unmount-impl [this]
    (.close dom)))

(defn svg
  ([src]
   (svg {} src))
  ([opts src]
   (let [dom (with-open [data (Data/makeFromBytes (core/slurp-bytes src))]
               (SVGDOM. data))]
     (map->SVG {:dom dom}))))
