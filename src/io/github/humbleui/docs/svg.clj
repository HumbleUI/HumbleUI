(ns io.github.humbleui.docs.svg
  (:require
    [clojure.java.io :as io]
    [io.github.humbleui.docs.shared :as shared]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp ui []
  (let [cl  (-> (Thread/currentThread) .getContextClassLoader)
        src (.getResource cl "io/github/humbleui/docs/svg/ratio.svg")]
    (fn []
      (shared/table
        "From file path"
        [ui/size {:size 64}
         [ui/svg
          {:src "src/io/github/humbleui/docs/svg/ratio.svg"}]]
        
        "From File object"
        [ui/size {:size 64}
         [ui/svg
          {:src (io/file "src/io/github/humbleui/docs/svg/ratio.svg")}]]
        
        "From resource"
        [ui/size {:size 64}
         [ui/svg
          {:src (.getResource cl "io/github/humbleui/docs/svg/ratio.svg")}]]
        
        "From input stream"
        [ui/size {:size 64}
         [ui/svg
          {:src (-> (.getResource cl "io/github/humbleui/docs/svg/ratio.svg")
                  (io/input-stream))}]]
        
        "From bytes"
        [ui/size {:size 64}
         [ui/svg
          {:src (-> (.getResource cl "io/github/humbleui/docs/svg/ratio.svg")
                  (io/input-stream)
                  (.readAllBytes))}]]
           
        "From URL string"
        [ui/size {:size 64}
         [ui/svg
          {:src "https://upload.wikimedia.org/wikipedia/commons/4/4f/SVG_Logo.svg"}]]
        
        "From URL object"
        [ui/size {:size 64}
         [ui/svg
          {:src (java.net.URL. "https://upload.wikimedia.org/wikipedia/commons/4/4f/SVG_Logo.svg")}]]
        
        "From URI object"
        [ui/size {:size 64}
         [ui/svg
          {:src (java.net.URI. "https://upload.wikimedia.org/wikipedia/commons/4/4f/SVG_Logo.svg")}]]
        
        "Fallback"
        [ui/size {:size 64}
         [ui/svg
          {:src "does-not-exist.svg"}]]
        
        "SVG fills container"
        [ui/row {:gap 10}
         (for [size [32 64 128]]
           [ui/rect {:paint {:stroke "CCC"}}
            [ui/size {:width size :height size}
             [ui/svg {:src src}]]])]
        
        ":scale :fit"
        [ui/rect {:paint {:stroke "CCC"}}
         [ui/size {:width 96 :height 32}
          [ui/svg {:src src :scale :fit}]]]
        
        ":scale :fill"
        [ui/rect {:paint {:stroke "CCC"}}
         [ui/clip
          [ui/size {:width 96 :height 32}
           [ui/svg {:src src :scale :fill}]]]]
        
        ":preserve-aspect-ratio false"
        [ui/row {:gap 5}
         [ui/rect {:paint {:stroke "CCC"}}
          [ui/size {:width 96 :height 32}
           [ui/svg {:src src
                    :preserve-aspect-ratio false}]]]]
        
        "Align inside container: horizontal fit"
        [ui/column {:gap 5}
         (for [x [:left :center :right]]
           [ui/rect {:paint {:stroke "CCC"}}
            [ui/size {:width 96 :height 32}
             [ui/svg {:src src :scale :fit :x x}]]])]
        
        "Align inside container: vertical fit"
        [ui/row {:gap 5}
         (for [y [:top :center :bottom]]
           [ui/rect {:paint {:stroke "CCC"}}
            [ui/size {:width 32 :height 96}
             [ui/svg {:src src :scale :fit :y y}]]])]
        
        "Align inside container: horizontal fill"
        [ui/column {:gap 5}
         (for [y [:top :center :bottom]]
           [ui/rect {:paint {:stroke "CCC"}}
            [ui/clip
             [ui/size {:width 96 :height 32}
              [ui/svg {:src src :scale :fill :y y}]]]])]
        
        "Align inside container: vertical fill"
        [ui/row {:gap 5}
         (for [x [:left :center :right]]
           [ui/rect {:paint {:stroke "CCC"}}
            [ui/clip
             [ui/size {:width 32 :height 96}
              [ui/svg {:src src :scale :fill :x x}]]]])]

        ))))
