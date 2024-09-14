(ns examples.file-picker
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [io.github.humbleui.ui :as ui])
  (:import
    [java.io File]))

(defn change-to [state ^File file]
  (assoc state
    :current  file
    :children (.listFiles file)
    :selected nil))

(def file-comparator
  (juxt
    (complement File/.isDirectory)
    (comp str/lower-case File/.getName)))

(defonce *state
  (ui/signal
    (-> {:width 200}
      (change-to (io/file (System/getProperty "user.dir"))))))

(ui/defcomp file-icon [^File file]
  (let [comps (->> ["􀈕" "􀈷"]
                (mapv #(vector ui/label {:font-family "SF Pro Text"} %)))
        width (->> comps
                (map #(-> % ui/make ui/measure :width))
                (reduce max 0))]
    (fn [^File file]
      [ui/size {:width width}
       [ui/align {:x :center}
        (if (.isDirectory file)
          (nth comps 0)
          (nth comps 1))]])))

(ui/defcomp root-comp [^File file]
  (let [*selected (ui/signal
                    (= file (:current @*state)))]
    (fn [^File file]
      [ui/clickable
       {:on-click
        (fn [_]
          (swap! *state change-to file))}
       [ui/rect {:paint {:fill (cond 
                                 @*selected [0.4 0.175 265]
                                 :else [1 0 0 0])
                         :model :oklch}}
        [ui/padding {:padding 8}
         [ui/with-context
          {:paint {:fill (cond
                           @*selected "FFF"
                           (.isHidden file) "AAA"
                           :else "000")}}
          [ui/row {:gap 4}
           [file-icon file]
           [ui/label (.getName file)]]]]]])))

(ui/defcomp roots-list []
  [ui/rect {:paint {:fill "FFF"}}
   [ui/vscroll
    [ui/padding {:vertical 8}
     [ui/column
      (for [file (->> (File/listRoots)
                   (mapcat ^[] File/.listFiles)
                   (filter File/.isDirectory)
                   (sort-by file-comparator))]
        [root-comp file])]]]])

(ui/defcomp file-comp [^File file]
  (let [*selected (ui/signal
                    (= file (:selected @*state)))]
    (fn [^File file]
      ; (println "render" file @*selected)
      [ui/clickable
       {:on-click
        (fn [e]
          (cond
            (and (.isDirectory file) (= 2 (:clicks e)))
            (swap! *state change-to file)
            
            :else
            (swap! *state assoc :selected file)))}
       (fn [state]
         [ui/rect {:paint {:fill (cond 
                                   @*selected [0.4 0.175 265]
                                   (:hovered state) [0.9 0.05 255]
                                   :else [1 0 0 0])
                           :model :oklch}}
          [ui/padding {:padding 8}
           [ui/with-context
            {:paint {:fill (cond
                             @*selected "FFF"
                             (.isHidden file) "AAA"
                             :else "000")}}
            [ui/row {:gap 4}
             [file-icon file]
             [ui/label (.getName file)]]]]])])))

(ui/defcomp file-list []
  [ui/rect {:paint {:fill "FFF"}}
   [ui/vscroll
    [ui/align {:y :top}
     [ui/padding {:vertical 8}      
      [ui/column
       (for [file (->> (:children @*state)
                    (sort-by file-comparator))]
         [file-comp file])]]]]])

(ui/defcomp buttons []
  (let [open  (ui/make [ui/button "Open"])
        close (ui/make [ui/button "Cancel"])
        width (->> [open close]
                (map #(:width (ui/measure %)))
                (reduce max 0))]
    (fn []
      [ui/align {:x :right}
       [ui/row {:gap 8}
        [ui/size {:width width} [ui/button "Open"]]
        [ui/size {:width width} [ui/button "Cancel"]]]])))

(ui/defcomp ui []
  [ui/padding {:padding 8}
   [ui/with-bounds
    (fn [bounds]
      [ui/grid {:cols [:hug :hug {:stretch 1}]
                :rows [:hug {:stretch 1} :hug]
                :row-gap 8}
       [ui/gap]
       [ui/gap]
       [ui/row {:gap 8 :align :center}
        [ui/button
         {:on-click
          (fn [_]
            (swap! *state change-to (io/file (System/getProperty "user.home"))))}
         [ui/label {:font-family "SF Pro Text"} "􀎞"]]
        [ui/button 
         {:on-click
          (fn [_]
            (swap! *state change-to
              (some-> @*state :current File/.getParentFile)))}
         [ui/label {:font-family "SF Pro Text"} "􀄶"]]
        [ui/label (File/.getCanonicalPath (:current @*state))]]
     
       [ui/size {:width (-> (:width @*state)
                          (max 0)
                          (min (- (:width bounds) 8)))}
        [roots-list]]
     
       [ui/draggable
        {:on-drag (fn [e]
                    (swap! *state update :width + (-> e :delta-last :x)))}
        [ui/gap {:width 8}]]
     
       [file-list]
     
       ^{:col-span 3}
       [buttons]])]])
