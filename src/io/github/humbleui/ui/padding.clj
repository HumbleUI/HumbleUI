(in-ns 'io.github.humbleui.ui)

(util/deftype+ Padding [left
                        top
                        right
                        bottom]
  :extends AWrapperNode  

  (-measure-impl [_ ctx cs]
    (let [left-px    (dimension left cs ctx)
          top-px     (dimension top cs ctx)
          right-px   (dimension right cs ctx)
          bottom-px  (dimension bottom cs ctx)
          child-cs   (util/ipoint
                       (-> (:width cs) (- left-px) (- right-px) (max 0))
                       (-> (:height cs) (- top-px) (- bottom-px) (max 0)))
          child-size (measure child ctx child-cs)]
      (util/ipoint
        (-> (:width child-size) (+ left-px) (+ right-px))
        (-> (:height child-size) (+ top-px) (+ bottom-px)))))
  
  (-draw-impl [_ ctx bounds container-size viewport canvas]
    (let [left-px      (dimension left container-size ctx)
          top-px       (dimension top container-size ctx)
          right-px     (dimension right container-size ctx)
          bottom-px    (dimension bottom container-size ctx)
          width        (-> (:width bounds) (- left-px) (- right-px) (max 0))
          height       (-> (:height bounds) (- top-px) (- bottom-px) (max 0))
          child-bounds (util/irect-xywh
                         (+ (:x bounds) left-px)
                         (+ (:y bounds) top-px)
                         width
                         height)
          child-cs     (util/ipoint
                         (-> (:width container-size) (- left-px) (- right-px) (max 0))
                         (-> (:height container-size) (- left-px) (- right-px) (max 0)))]
      (draw child ctx child-bounds child-cs viewport canvas)))
  
  (-reconcile-opts [this ctx new-element]
    (let [opts    (parse-opts new-element)
          left'   (or (:left opts)   (:horizontal opts) (:padding opts) 0)
          top'    (or (:top opts)    (:vertical opts)   (:padding opts) 0)
          right'  (or (:right opts)  (:horizontal opts) (:padding opts) 0)
          bottom' (or (:bottom opts) (:vertical opts)   (:padding opts) 0)]
      (when (or
              (not= left left')
              (not= top top')
              (not= right right')
              (not= bottom bottom'))
        (set! left left')
        (set! top top')
        (set! right right')
        (set! bottom bottom')
        (invalidate-size this)))))

(defn- padding-ctor
  "Adds padding to element. Options are:
   
     :padding    :: <number> | (fn [size]) - equal padding from all four sides
     :horizontal :: <number> | (fn [size]) - equal horizontal padding
     :vertical   :: <number> | (fn [size]) - equal vertical padding
     :left       :: <number> | (fn [size]) - padding from the left
     :right      :: <number> | (fn [size]) - padding from the right
     :top        :: <number> | (fn [size]) - padding from the top
     :bottom     :: <number> | (fn [size]) - padding from the bottom
   
   More specific values override more generice ones."
  [opts child]
  (map->Padding {}))
