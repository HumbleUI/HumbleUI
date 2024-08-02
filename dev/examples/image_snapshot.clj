(ns examples.image-snapshot
  (:require
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.util :as util])
  (:import
    [io.github.humbleui.skija Paint Shader]))

(defn ui-impl [bounds]
  (let [*shader (atom nil)
        *paint  (atom nil)]
    {:render
     (fn [bounds]
       (util/close @*shader)
       (util/close @*paint)
       (let [scale  (:scale ui/*ctx*)
             height (:height bounds)
             shader (Shader/makeLinearGradient
                      (float 0)
                      (float 0)
                      (float 0)
                      (float (* height 2 scale))
                      (int-array [(unchecked-int 0xFF277DA1)
                                  (unchecked-int 0xFFFFBA08)]))
             paint  (-> (Paint.) (.setShader shader))]
         (reset! *shader shader)
         (reset! *paint paint)
         [ui/vscroll
          [ui/padding {:padding 20}
           [ui/size {:height (* height 2)}
            [ui/row
             [ui/gap {:width 10}]
             ^{:stretch 1}
             [ui/rect {:paint paint} [ui/gap]]
             [ui/gap {:width 10}]
             ^{:stretch 1}
             [ui/image-snapshot
              [ui/rect {:paint paint} [ui/gap]]]
             [ui/gap {:width 10}]]]]]))
     :after-unmount
     (fn []
       (util/close @*shader)
       (util/close @*paint))}))

(ui/defcomp ui []
  [ui/with-bounds
   (fn [bounds]
     [ui-impl bounds])])
