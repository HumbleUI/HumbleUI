(ns io.github.humbleui.docs.paint
  (:require
    [clojure.string :as str]
    [io.github.humbleui.docs.shared :as shared]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  (fn []
    (shared/table
      "Fill"
      [ui/rect {:paint {:fill "FFDD22"}}
       [ui/size {:width 100 :height 50}]]
      
      "Stroke"
      [ui/rect {:paint {:stroke "0088FF80"}}
       [ui/size {:width 100 :height 50}]]
      
      "Stroke width"
      [ui/rect {:paint {:stroke "0088FF"
                        :width  6}}
       [ui/size {:width 100 :height 50}]]
      
      "Multiple paints"
      [ui/rect {:paint [{:fill   "FFDD22"}
                        {:stroke "0088FF", :width 6}]}
       [ui/size {:width 100 :height 50}]]
      
      "Multiple paints, reverse order"
      [ui/rect {:paint [{:stroke "0088FF", :width 6}
                        {:fill "FFDD22"}]}
       [ui/size {:width 100 :height 50}]]
      
      "Multiple paints on text"
      [ui/label {:font-cap-height 30
                 :paint [{:stroke "0088FF", :width 3}
                         {:fill   "FFDD22"}]}
       "Andy"]
      
      "Instanced paint"
      (let [paint (ui/paint {:fill "FFDD22"})]
        [ui/rect {:paint paint}
         [ui/size {:width 100 :height 50}]])
      
      "Stroke join"
      [ui/size {:width 100}
       [ui/row {:gap 20}
        (for [join [:miter :round :bevel]]
          ^{:stretch 1}
          [ui/rect {:paint {:stroke "0088FF"
                            :width  10
                            :join   join}}
           [ui/size {:height 50}]])]]
      
      "No paint"
      [ui/rect {:paint nil}
       [ui/size {:width 100 :height 50}]]
      
      "Nil paint"
      [ui/rect {:paint {:fill nil}}
       [ui/size {:width 100 :height 50}]])))
