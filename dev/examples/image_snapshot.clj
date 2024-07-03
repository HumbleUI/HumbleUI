(ns examples.image-snapshot
  (:require
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint Shader]))

(defn ui-impl [bounds]
  (let [scale (:scale ui/*ctx*)
        height (:height bounds)
        shader (Shader/makeLinearGradient
                 (float 0)
                 (float 0)
                 (float 0)
                 (float (* height 2 scale))
                 (int-array [(unchecked-int 0xFF277da1)
                             (unchecked-int 0xFFffba08)]))
        paint  (-> (Paint.) (.setShader shader))]
    (fn [_]
      [ui/vscrollbar
       [ui/size {:height (* height 2)}
        [ui/row
         [ui/gap {:width 10}]
         ^{:stretch 1}
         [ui/rect {:paint paint} [ui/gap]]
         [ui/gap {:width 10}]
         ^{:stretch 1}
         [ui/image-snapshot
          [ui/rect {:paint paint} [ui/gap]]]
         [ui/gap {:width 10}]]]])))

(ui/defcomp ui []
  [ui/with-bounds 
   (fn [bounds]
     [ui-impl bounds])])
