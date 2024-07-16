(in-ns 'io.github.humbleui.ui)

(util/deftype+ VScroll []
  :extends AWrapperNode
  
  protocols/IComponent
  (-draw-impl [_ ctx bounds viewport ^Canvas canvas]
    (draw child ctx bounds viewport canvas)
    (when (> (:height (:child-size child)) (:height bounds))
      (let [{:keys [scale]} ctx
            [_ opts _]      (parse-element element)
            fill-track      (or (:fill-track opts) (:hui.scroll/fill-track ctx))
            fill-thumb      (or (:fill-thumb opts) (:hui.scroll/fill-thumb ctx))
            content-y       (:offset-px child)
            content-h       (:height (:child-size child))
            scroll-y        (:y bounds)
            scroll-h        (:height bounds)
            
            padding         (scaled 4)
            track-w         (scaled 4)
            track-x         (+ (:x bounds) (:width bounds) (- track-w) (- padding))
            track-y         (+ scroll-y padding)
            track-h         (- scroll-h (* 2 padding))
            track           (util/rrect-xywh track-x track-y track-w track-h (* 2 scale))
            
            thumb-w         (scaled 4)
            min-thumb-h     (scaled 16)
            
            thumb-h         (-> track-h (* track-h) (/ content-h) (max min-thumb-h))
            thumb-range     (- track-h thumb-h)
            content-range   (- content-h scroll-h)
            thumb-y         (-> content-y (/ content-range) (* thumb-range))
            
            ; thumb-y-ratio   (/ content-y content-h)
            ; thumb-y         (-> (* track-h thumb-y-ratio) (util/clamp 0 (- track-h min-thumb-h)) (+ track-y))
            ; thumb-b-ratio   (/ (+ content-y scroll-h) content-h)
            ; thumb-b         (-> (* track-h thumb-b-ratio) (util/clamp min-thumb-h track-h) (+ track-y))
            
            thumb           (util/rrect-xywh track-x (+ track-y thumb-y) thumb-w thumb-h (scaled 2))]
        (canvas/draw-rect canvas track fill-track)
        (canvas/draw-rect canvas thumb fill-thumb)))))

(defn- vscroll-impl
  ([child]
   (vscroll-impl {} child))
  ([opts child]
   (map->VScroll {})))

(defn- vscroll-ctor
  ([child]
   (vscroll-ctor {} child))
  ([opts child]
   [vscroll-impl opts [vscrollable opts child]]))
