(ns examples.tree
  (:require
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(defn random-green []
  (let [r (+ 32  (rand-int 32))
        g (+ 192 (rand-int 32))
        b (+ 32  (rand-int 32))]
    (unchecked-int
      (bit-or
        (unchecked-int 0xFF000000)
        (bit-shift-left r 16)
        (bit-shift-left g 8)
        (bit-shift-left b 0)))))

(def ui
  (ui/dynamic ctx [{:keys [font-ui leading fill-text]} ctx]
    (let [labels (cycle (map #(ui/label (str %) font-ui fill-text) "HappyNew2022!"))]
      (ui/halign 0.5
        (ui/padding 0 10
          ;; TODO replace bounds with component
          (ui/dynamic ctx [rows (quot (- (:height (:bounds ctx)) 10) (+ 11 leading))
                           time (quot (System/currentTimeMillis) 1000)]
            (apply ui/column
              (interpose
                (ui/gap 0 1)
                (for [y (range rows)]
                  (ui/halign 0.5
                    (apply ui/row
                      (interpose
                        (ui/gap 1 0)
                        (for [x (range (inc y))]
                          (if (= x y 0)
                            (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFFCC3333)))
                              (ui/padding 5 5
                                (ui/label "★" font-ui fill-text)))
                            (ui/fill (doto (Paint.) (.setColor (random-green)))
                              (ui/padding 5 5
                                (let [idx (+ x (* y (+ y 1) 1/2) -1)]
                                  (nth labels idx))))))))))))))))))
