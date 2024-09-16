(ns io.github.humbleui.docs.image
  (:require
    [clojure.java.io :as io]
    [io.github.humbleui.docs.shared :as shared]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  (let [cl        (-> (Thread/currentThread) .getContextClassLoader)
        face-35w  (.getResource cl "io/github/humbleui/docs/image/face_35w.png")
        face-100w (.getResource cl "io/github/humbleui/docs/image/face_100w.png")
        face-185w (.getResource cl "io/github/humbleui/docs/image/face_185w.png")]
    (fn []
      (shared/table
        "From file path"
        [ui/size {:size 64}
         [ui/image
          {:src "src/io/github/humbleui/docs/image/face_185w.png"}]]
        
        "From File object"
        [ui/size {:size 64}
         [ui/image
          {:src (io/file "src/io/github/humbleui/docs/image/face_185w.png")}]]
        
        "From resource"
        [ui/size {:size 64}
         [ui/image
          {:src (.getResource cl "io/github/humbleui/docs/image/face_185w.png")}]]
        
        "From input stream"
        [ui/size {:size 64}
         [ui/image
          {:src (-> (.getResource cl "io/github/humbleui/docs/image/face_185w.png")
                  (io/input-stream))}]]
        
        "From bytes"
        [ui/size {:size 64}
         [ui/image
          {:src (-> (.getResource cl "io/github/humbleui/docs/image/face_185w.png")
                  (io/input-stream)
                  (.readAllBytes))}]]
           
        "From URL string"
        [ui/size {:size 64}
         [ui/image
          {:src "https://tonsky.me/i/favicon.png"}]]
        
        "From URL object"
        [ui/size {:size 64}
         [ui/image
          {:src (java.net.URL. "https://tonsky.me/i/favicon.png")}]]
        
        "From URI object"
        [ui/size {:size 64}
         [ui/image
          {:src (java.net.URI. "https://tonsky.me/i/favicon.png")}]]
        
        "Fallback"
        [ui/size {:size 64}
         [ui/image
          {:src "does-not-exist.png"}]]
        
        "Image fills container"
        [ui/row {:gap 10}
         (for [size [32 64 128]]
           [ui/size {:width size :height size}
            [ui/image {:src face-185w}]])]
        
        ":scale :fit"
        [ui/rect {:paint {:stroke "CCC"}}
         [ui/size {:width 128 :height 32}
          [ui/image {:src face-185w :scale :fit}]]]
        
        ":scale :fill"
        [ui/rect {:paint {:stroke "CCC"}}
         [ui/size {:width 128 :height 32}
          [ui/image {:src face-185w :scale :fill}]]]
        
        ":scale :content (original image’s dimensions)"
        [ui/row {:gap 5}
         (for [src [face-35w face-100w face-185w]]
           [ui/rect {:paint {:stroke "CCC"}}
            [ui/image {:src src :scale :content}]])]
        
        ":scale 0.5 (multiple of image’s dimensions)"
        [ui/rect {:paint {:stroke "CCC"}}
         [ui/size {:width 128 :height 32}
          [ui/image {:src face-185w :scale 0.5}]]]
        
        "Align inside container: horizontal fit"
        [ui/column {:gap 5}
         (for [x [:left 0.25 :center 0.75 :right]]
           [ui/rect {:paint {:stroke "CCC"}}
            [ui/size {:width 128 :height 32}
             [ui/image {:src face-185w :scale :fit :x x}]]])]
        
        "Align inside container: vertical fit"
        [ui/row {:gap 5}
         (for [y [:top 0.25 :center 0.75 :bottom]]
           [ui/rect {:paint {:stroke "CCC"}}
            [ui/size {:width 32 :height 128}
             [ui/image {:src face-185w :scale :fit :y y}]]])]
        
        "Align inside container: horizontal fill"
        [ui/column {:gap 5}
         (for [y [:top 0.25 :center 0.75 :bottom]]
           [ui/rect {:paint {:stroke "CCC"}}
            [ui/size {:width 128 :height 32}
             [ui/image {:src face-185w :scale :fill :y y}]]])]
        
        "Align inside container: vertical fill"
        [ui/row {:gap 5}
         (for [x [:left 0.25 :center 0.75 :right]]
           [ui/rect {:paint {:stroke "CCC"}}
            [ui/size {:width 32 :height 128}
             [ui/image {:src face-185w :scale :fill :x x}]]])]
        
        "Scaling down (185 -> 100)"
        [ui/row {:gap 5}
         (for [mode [:nearest :linear :mitchell :catmull-rom]]
           [ui/size {:size 100}
            [ui/image {:src face-185w
                       :sampling mode}]])]
        
        "Scaling up (35 -> 100)"
        [ui/row {:gap 5}
         (for [mode [:nearest :linear :mitchell :catmull-rom]]
           [ui/size {:size 100}
            [ui/image {:src face-35w
                       :sampling mode}]])]
        
        ))))
