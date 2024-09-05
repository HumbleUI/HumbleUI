(ns examples.image
  (:require
    [io.github.humbleui.ui :as ui]))

(defn img [opts]
  [ui/size {:width (:w opts) :height (:h opts)}
   [ui/rect {:paint {:stroke 0x20000000, :width 2}}
    [ui/image
     {:scale (:scale opts)
      :x     (:x opts)
      :y     (:y opts)
      :src   "dev/images/face_100w.png"}]]])

(defn ui []
  [ui/align {:y :center}
   [ui/vscroll
    [ui/align {:x :center}
     [ui/padding {:padding 20}
      [ui/column {:gap 10}
     
       ;; scaled down
       [ui/row {:gap 10}
        (for [mode [:nearest :linear :mitchell :catmull-rom]]
          [ui/column {:gap 10}
           [ui/size {:width 100 :height 100}
            [ui/image {:sampling mode, :src "dev/images/face_185w.png"}]]
           [ui/label mode]])]
       [ui/label "Scaled down (185 → 100)"]
       [ui/gap]
    
       ;; scaled up
       [ui/row {:gap 10}
        (for [mode [:nearest :linear :mitchell :catmull-rom]]
          [ui/column {:gap 10}
           [ui/size {:width 100 :height 100}
            [ui/image {:sampling mode, :src "dev/images/face_35w.png"}]]
           [ui/label mode]])]
       [ui/label "Scaled up (35 → 100)"]
       [ui/gap]

       ;; not found
       [ui/align {:x :left}
        [ui/size {:width 50 :height 50}
         [ui/image {:src "dev/images/not_found.png"}]]]
       [ui/label "Not found"]
       [ui/gap]
     
       ;; 170 = x * 4 + y * 3
       
       ;; scaling
       [ui/row {:gap 10}
        [ui/column {:gap 10}
         [img {:w 170 :h 35 :scale :fit :x :left}]
         [img {:w 170 :h 35 :scale :fit :x 0.25}]
         [img {:w 170 :h 35 :scale :fit :x :center}]
         [img {:w 170 :h 35 :scale :fit :x :right}]
         [ui/label ":fit"]]
        [ui/gap]
      
        [ui/column {:gap 10}
         [img {:w 170 :h 35 :scale :fill :y :top}]
         [img {:w 170 :h 35 :scale :fill :y 0.25}]
         [img {:w 170 :h 35 :scale :fill :y :center}]
         [img {:w 170 :h 35 :scale :fill :y :bottom}]
         [ui/label ":fill"]]
        [ui/gap]
      
        [ui/column {:gap 10}
         [ui/row {:gap 10}
          [img {:w 50 :h 50 :scale 0.333 :x :left   :y :top}]
          [img {:w 50 :h 50 :scale 0.333 :x :center :y :top}]
          [img {:w 50 :h 50 :scale 0.333 :x :right   :y :top}]]
         [ui/row {:gap 10}
          [img {:w 50 :h 50 :scale 0.333 :x :left   :y :center}]
          [img {:w 50 :h 50 :scale 0.333 :x :center :y :center}]
          [img {:w 50 :h 50 :scale 0.333 :x :right   :y :center}]]
         [ui/row {:gap 10}
          [img {:w 50 :h 50 :scale 0.333 :x :left   :y :bottom}]
          [img {:w 50 :h 50 :scale 0.333 :x :center :y :bottom}]
          [img {:w 50 :h 50 :scale 0.333 :x :right   :y :bottom}]]
         [ui/label ":scale 0.333"]]
        [ui/gap]
      
        [ui/column {:gap 10}
         [ui/image {:scale :content, :src "dev/images/face_100w.png"}]
         [ui/label ":content"]]]
       [ui/gap]
     
       [ui/row {:gap 10}
        [ui/column {:gap 10}
         [ui/row {:gap 10}
          [img {:w 35 :h 170 :scale :fit :y :top}]
          [img {:w 35 :h 170 :scale :fit :y 0.25}]
          [img {:w 35 :h 170 :scale :fit :y :center}]
          [img {:w 35 :h 170 :scale :fit :y :bottom}]]
         [ui/label ":fit"]]
        [ui/gap]
      
        [ui/column {:gap 10}
         [ui/row {:gap 10}
          [img {:w 35 :h 170 :scale :fill :x :left}]
          [img {:w 35 :h 170 :scale :fill :x 0.25}]
          [img {:w 35 :h 170 :scale :fill :x :center}]
          [img {:w 35 :h 170 :scale :fill :x :right}]]
         [ui/label ":fill"]]
        [ui/gap]
      
        [ui/column {:gap 10}
      
         [ui/row {:gap 10}
          [img {:w 50 :h 50 :scale 1.5 :x :left   :y :top}]
          [img {:w 50 :h 50 :scale 1.5 :x :center :y :top}]
          [img {:w 50 :h 50 :scale 1.5 :x :right   :y :top}]]
         [ui/row {:gap 10}
          [img {:w 50 :h 50 :scale 1.5 :x :left   :y :center}]
          [img {:w 50 :h 50 :scale 1.5 :x :center :y :center}]
          [img {:w 50 :h 50 :scale 1.5 :x :right   :y :center}]]
         [ui/row {:gap 10}
          [img {:w 50 :h 50 :scale 1.5 :x :left   :y :bottom}]
          [img {:w 50 :h 50 :scale 1.5 :x :center :y :bottom}]
          [img {:w 50 :h 50 :scale 1.5 :x :right   :y :bottom}]]
         [ui/label ":scale 1.5"]]]]]]]])
