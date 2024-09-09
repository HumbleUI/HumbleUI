(ns examples.grid
  (:require
    [clojure.string :as str]
    [examples.shared :as shared]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(ui/defcomp label [text]
  [ui/rect {:paint [{:fill   "FFDB2C80"}
                    {:stroke "808080"}]}
   [ui/padding {:padding 10}
    [ui/align {:x :left :y :top}
     [ui/label text]]]])

(ui/defcomp ui []
  (shared/table
    "Simple hug grid"
    [ui/grid {:cols 2}
     [label "Lorem"]
     [label "ipsum"]
     [label "dolor"]
     [label "sit"]
     [label "amet,"]
     [label "consectetur"]]
    
    "Incomplete"
    [ui/grid {:cols 2}
     [label "Lorem"]
     [label "ipsum"]
     [label "dolor"]
     [label "sit"]
     [label "amet,"]]
    
    "Even gap"
    [ui/grid {:cols 2
              :gap  8}
     [label "Lorem"]
     [label "ipsum"]
     [label "dolor"]
     [label "sit"]
     [label "amet,"]
     [label "consectetur"]]
    
    "Uneven gap"
    [ui/grid {:cols    2
              :col-gap 16
              :row-gap 8}
     [label "Lorem"]
     [label "ipsum"]
     [label "dolor"]
     [label "sit"]
     [label "amet,"]
     [label "consectetur"]]
    
    "Stretch"
    [ui/size {:height 200}
     [ui/grid {:cols [:hug {:stretch 1} :hug]
               :rows [:hug {:stretch 1} :hug]}
      (map #(vector label %)
        (str/split "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed"
          #"\s"))]]
    
    "Last row spec is repeated"
    [ui/size {:height 300}
     [ui/grid {:cols [:hug {:stretch 1} :hug]
               :rows [:hug {:stretch 1} :hug]}
      (map #(vector label %)
        (str/split "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua"
          #"\s"))]]
    
    "Colspan"
    [ui/grid {:cols 3}
     [label "Lorem"]
     [label "ipsum"]
     [label "dolor"]
     
     ^{:col-span 2}
     [label "sit"]
     [label "amet,"]
     
     [label "consectetur"]
     ^{:col-span 2}
     [label "adipiscing"]
     
     ^{:col-span 3}
     [label "elit,"]]))
