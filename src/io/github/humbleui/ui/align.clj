(in-ns 'io.github.humbleui.ui)

(util/deftype+ Align [x
                      y
                      child-x
                      child-y]
  :extends AWrapperNode  

  (-draw-impl [_ ctx bounds container-size viewport canvas]
    (let [child-size   (measure child ctx container-size)
          left         (when x
                         (-> (:x bounds)
                           (+ (* (:width bounds) x))
                           (- (* (:width child-size) child-x))))
          top          (when y
                         (-> (:y bounds)
                           (+ (* (:height bounds) y))
                           (- (* (:height child-size) child-y))))
          child-bounds (cond
                         (and x y)
                         (util/irect-xywh left top (:width child-size) (:height child-size))
                         
                         x
                         (util/irect-xywh left (:y bounds) (:width child-size) (:height bounds))
                         
                         y
                         (util/irect-xywh (:x bounds) top (:width bounds) (:height child-size)))]
      (draw child ctx child-bounds container-size viewport canvas)))
  
  (-reconcile-opts [_this ctx new-element]
    (let [opts (parse-opts new-element)
          {x' :x
           y' :y
           child-x' :child-x
           child-y' :child-y} opts]
      (assert (or (nil? x') (number? x') (#{:left :center :right} x')) (str ":x, expected number or :left/:center/:right, got: " (pr-str x')))
      (assert (or (nil? child-x') (number? child-x') (#{:left :center :right} child-x')) (str ":child-x, expected number or :left/:center/:right, got: " (pr-str child-x')))
      (assert (or (nil? y') (number? y') (#{:top :center :bottom} y')) (str ":y, expected number or :top/:center/:bottom, got: " (pr-str y')))
      (assert (or (nil? child-y') (number? child-y') (#{:top :center :bottom} child-y')) (str ":child-y, expected number or :top/:center/:bottom, got: " (pr-str child-y')))
      (assert (or x' y') (str "Expected one of: :x, :y, got:" (pr-str opts)))
      (set! x
        (condp identical? x'
          :left   0
          :center 0.5
          :right  1
          x'))
      (set! y 
        (condp identical? y'
          :top    0
          :center 0.5
          :bottom 1
          y'))
      (set! child-x
        (condp identical? child-x'
          nil     x
          :left   0
          :center 0.5
          :right  1
          child-x'))
      (set! child-y
        (condp identical? child-y'
          nil     y
          :top    0
          :center 0.5
          :bottom 1
          child-y')))))

(defn- align-ctor [opts child]
  (map->Align {}))

(defn- center-ctor [child]
  [align {:x 0.5 :y 0.5}
   child])
