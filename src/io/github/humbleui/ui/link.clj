(in-ns 'io.github.humbleui.ui)

(defcomp link-ctor [{:keys [color
                            color-visited
                            color-hovered
                            color-pressed
                            on-click
                            visited]
                     :or {color         0xFF0071FF
                          color-visited 0xFFB30EB3
                          color-hovered 0xFFEE0000
                          on-click      #()}}
                    child]
  (let [color-pressed (or
                        color-pressed
                        color-hovered)
        visited       (or visited (signal/signal false))
        font          (get-font)
        metrics       (font/metrics font)
        position      (descaled (:underline-position metrics))
        thickness     (descaled (:underline-thickness metrics))]
    (with-resources [paint         (paint/fill color)
                     paint-visited (paint/fill color-visited)
                     paint-hovered (paint/fill color-hovered)
                     paint-pressed (paint/fill color-pressed)]
      (fn [on-click child]
        [center
         [with-cursor {:cursor :pointing-hand}
          [clickable {:on-click (fn [_event] 
                                  (reset! visited true)
                                  (on-click))}
           (fn [state]
             (let [paint (cond
                           (:pressed state) paint-pressed
                           (:hovered state) paint-hovered
                           @visited        paint-visited
                           :else            paint)]
               [column
                [label {:paint paint} child]
                [size {:height position}]
                [rect {:paint paint}
                 [size {:height thickness}]]]))]]]))))
