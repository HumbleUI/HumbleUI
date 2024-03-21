(ns examples.image
  (:require
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui]))

(def border
  (paint/stroke 0x20000000 2))

(defn img [opts]
  [ui/size {:width (:w opts) :height (:h opts)}
   [ui/rect {:paint border}
    [ui/image {:scale (:scale opts)
               :xpos    (:xpos opts)
               :ypos    (:ypos opts)}
     "dev/images/face_100w.png"]]])

(defn ui []
  [ui/vscrollbar
   [ui/padding {:padding 10}
    [ui/column {:gap 10}
     
     ;; scaled down
     [ui/row {:gap 10}
      (for [mode [:nearest :linear :mitchell :catmull-rom]]
        [ui/column {:gap 10}
         [ui/size {:width 100 :height 100}
          [ui/image {:sampling mode} "dev/images/face_185w.png"]]
         [ui/label mode]])]
     [ui/label "Scaled down (185 → 100)"]
     [ui/gap]
    
     ;; scaled up
     [ui/row {:gap 10}
      (for [mode [:nearest :linear :mitchell :catmull-rom]]
        [ui/column {:gap 10}
         [ui/size {:width 100 :height 100}
          [ui/image {:sampling mode} "dev/images/face_35w.png"]]
         [ui/label mode]])]
     [ui/label "Scaled up (35 → 100)"]
     [ui/gap]

     ;; not found
     [ui/halign {:position 0}
      [ui/size {:width 50 :height 50}
       [ui/image "dev/images/not_found.png"]]]
     [ui/label "Not found"]
     [ui/gap]
     
     ;; scaling
     [ui/row {:gap 10}
      [ui/column {:gap 10}
       [img {:w 170 :h 50 :scale :fit :xpos 0}]
       [img {:w 170 :h 50 :scale :fit :xpos 0.5}]
       [img {:w 170 :h 50 :scale :fit :xpos 1}]
       [ui/label ":fit"]]
      [ui/gap]
      
      [ui/column {:gap 10}
       [img {:w 170 :h 50 :scale :fill :ypos 0}]
       [img {:w 170 :h 50 :scale :fill :ypos 0.5}]
       [img {:w 170 :h 50 :scale :fill :ypos 1}]
       [ui/label ":fill"]]
      [ui/gap]
      
      [ui/column {:gap 10}
       [ui/row {:gap 10}
        [img {:w 50 :h 50 :scale 0.333 :xpos 0   :ypos 0}]
        [img {:w 50 :h 50 :scale 0.333 :xpos 0.5 :ypos 0}]
        [img {:w 50 :h 50 :scale 0.333 :xpos 1   :ypos 0}]]
       [ui/row {:gap 10}
        [img {:w 50 :h 50 :scale 0.333 :xpos 0   :ypos 0.5}]
        [img {:w 50 :h 50 :scale 0.333 :xpos 0.5 :ypos 0.5}]
        [img {:w 50 :h 50 :scale 0.333 :xpos 1   :ypos 0.5}]]
       [ui/row {:gap 10}
        [img {:w 50 :h 50 :scale 0.333 :xpos 0   :ypos 1}]
        [img {:w 50 :h 50 :scale 0.333 :xpos 0.5 :ypos 1}]
        [img {:w 50 :h 50 :scale 0.333 :xpos 1   :ypos 1}]]
       [ui/label ":scale 0.333"]]
      [ui/gap]
      
      [ui/column {:gap 10}
       [ui/image {:scale :content} "dev/images/face_100w.png"]
       [ui/label ":content"]]]
     [ui/gap]
     
     [ui/row {:gap 10}
      [ui/column {:gap 10}
       [ui/row {:gap 10}
        [img {:w 50 :h 170 :scale :fit :ypos 0}]
        [img {:w 50 :h 170 :scale :fit :ypos 0.5}]
        [img {:w 50 :h 170 :scale :fit :ypos 1}]]
       [ui/label ":fit"]]
      [ui/gap]
      
      [ui/column {:gap 10}
       [ui/row {:gap 10}
        [img {:w 50 :h 170 :scale :fill :xpos 0}]
        [img {:w 50 :h 170 :scale :fill :xpos 0.5}]
        [img {:w 50 :h 170 :scale :fill :xpos 1}]]
       [ui/label ":fill"]]
      [ui/gap]
      
      [ui/column {:gap 10}
      
       [ui/row {:gap 10}
        [img {:w 50 :h 50 :scale 1.5 :xpos 0   :ypos 0}]
        [img {:w 50 :h 50 :scale 1.5 :xpos 0.5 :ypos 0}]
        [img {:w 50 :h 50 :scale 1.5 :xpos 1   :ypos 0}]]
       [ui/row {:gap 10}
        [img {:w 50 :h 50 :scale 1.5 :xpos 0   :ypos 0.5}]
        [img {:w 50 :h 50 :scale 1.5 :xpos 0.5 :ypos 0.5}]
        [img {:w 50 :h 50 :scale 1.5 :xpos 1   :ypos 0.5}]]
       [ui/row {:gap 10}
        [img {:w 50 :h 50 :scale 1.5 :xpos 0   :ypos 1}]
        [img {:w 50 :h 50 :scale 1.5 :xpos 0.5 :ypos 1}]
        [img {:w 50 :h 50 :scale 1.5 :xpos 1   :ypos 1}]]
       [ui/label ":scale 1.5"]]]
      
     ]]])
