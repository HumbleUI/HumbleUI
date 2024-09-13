(ns io.github.humbleui.docs.devtools
  (:require
    [io.github.humbleui.docs.shared :as shared]
    [io.github.humbleui.debug :as debug]
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.window :as window]))

(defn setting [name opts]
  [ui/row
   [ui/align {:y :center}
    [ui/label name]]
   ^{:stretch 1} [ui/gap {:width 20}]
   [ui/switch opts]])

(defn redraw-all [_]
  (doseq [[window _] @window/*windows]
    (window/request-frame window)))

(ui/defcomp ui []
  [ui/align {:y :center}
   [ui/vscroll
    [ui/align {:x :center}
     [ui/padding {:padding 20}
      [ui/column {:gap 20}
       [ui/rect {:radius 6, :paint {:fill 0xFFF2F2F2}}
        [ui/rect {:radius 6, :paint {:stroke 0xFFE0E0E0, :width 0.5}}
         [ui/padding {:padding 12}
          [ui/column {:gap [ui/padding {:vertical 12}
                            [ui/rect {:paint {:fill 0xFFE7E7E7}}
                             [ui/gap {:height 1}]]]}
           [ui/label "Keep on top"]
           (for [[window {:keys [title]}] @window/*windows]
             [setting title {:*value    (ui/signal
                                          (= :floating (window/z-order window)))
                             :on-change #(if %
                                           (window/set-z-order window :floating)
                                           (window/set-z-order window :normal))}])]]]]
      
       [ui/rect {:radius 6, :paint {:fill 0xFFF2F2F2}}
        [ui/rect {:radius 6, :paint {:stroke 0xFFE0E0E0, :width 0.5}}
         [ui/padding {:padding 12}
          [ui/column {:gap [ui/padding {:vertical 12}
                            [ui/rect {:paint {:fill 0xFFE7E7E7}}
                             [ui/gap {:height 1}]]]}
          
           [setting "Frame paint time" {:*value debug/*paint? :on-change redraw-all}]
           [setting "Frame pacing" {:*value debug/*pacing? :on-change redraw-all}]
           [setting "Event pacing" {:*value debug/*events? :on-change redraw-all}]
           [setting "Outlines" {:*value debug/*outlines? :on-change redraw-all}]
           [setting "Continuous render" {:*value debug/*continuous-render? :on-change redraw-all}]]]]]]]]]])
