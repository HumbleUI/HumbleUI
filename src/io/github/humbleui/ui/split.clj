(in-ns 'io.github.humbleui.ui)

(defcomp hsplit-ctor
  ([left right]
   (hsplit-ctor {} left right))
  ([opts left right]
   [with-bounds
    (fn [bounds]
      (let [width  (:width opts)
            *width (signal (cond
                             (number? width)
                             width
                             
                             (fn? width)
                             (width bounds)
                             
                             (nil? width)
                             (quot (:width bounds) 2)
                             
                             :else
                             (throw (ex-info (str ":width expected number or fn, got: " width) {:width width}))))
            gap-width (cond
                        (vector? (:gap opts))
                        (-> (:gap opts) make measure :width)
                        
                        (number? (:gap opts))
                        (:gap opts)
                        
                        (nil? (:gap opts))
                        8)]
        (fn [_]
          [row
           [size {:width (util/clamp @*width 0 (- (:width bounds) gap-width))}
            left]
         
           [draggable
            {:on-drag (fn [e]
                        (swap! *width + (-> e :delta-last :x)))}
            (let [g (:gap opts)]
              (cond
                (number? g)
                [gap {:width g}]
                
                (nil? g)
                [gap {:width 8}]
                
                (fn? g)
                [gap {:width (g bounds)}]
                
                :else
                g))]
         
           (with-meta right {:stretch 1})])))]))
