(in-ns 'io.github.humbleui.ui)

(util/deftype+ VScroll [^:mut fill-track
                        ^:mut fill-thumb]
  :extends AWrapperNode
  
  (-draw-impl [_ ctx bounds container-size viewport ^Canvas canvas]
    (draw child ctx bounds container-size viewport canvas)
    (when (> (:height (:child-size child)) (:height bounds))
      (let [{:keys [scale]} ctx
            fill-track      (or fill-track (:hui.scroll/fill-track ctx))
            fill-thumb      (or fill-thumb (:hui.scroll/fill-thumb ctx))
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
        (with-paint ctx [paint fill-track]
          (canvas/draw-rect canvas track paint))
        (with-paint ctx [paint fill-thumb]
          (canvas/draw-rect canvas thumb paint)))))
  
  (-reconcile-opts [_this _ctx new-element]
    (let [opts (parse-opts new-element)]
      (set! fill-track (:fill-track opts))
      (set! fill-thumb (:fill-thumb opts)))))

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
