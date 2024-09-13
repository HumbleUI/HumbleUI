(ns examples.wordle
  (:refer-clojure :exclude [key type])
  (:require
    [clojure.string :as str]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui]))

(def padding 4)

;; https://github.com/AllValley/WordleDictionary
(def *dictionary
  (delay
    (->> (slurp "dev/examples/wordle_dictionary.txt")
      (str/split-lines)
      (map str/upper-case)
      (set))))

(def *solutions
  (delay
    (->> (slurp "dev/examples/wordle_solutions.txt")
      (str/split-lines)
      (mapv str/upper-case))))

(defn empty-state []
  {:word    (rand-nth @*solutions)
   :guesses []
   :typing  ""})

(def *state
  (ui/signal (empty-state)
    #_{:word    "QUART"
       :guesses ["CRANE" "START" ...]
       :typing  "SSRAT"}))

(defn won? [{:keys [word guesses]}]
  (= (last guesses) word))

(defn type! [code]
  (let [{:keys [typing] :as state} @*state
        typed (count typing)]
    (when-not (won? state)
      (cond
        (and (< typed 5) (string? code) (contains? (into #{} (map str) "ABCDEFGHIJKLMNOPQRSTUVWXYZ") code))
        (swap! *state update :typing str code)

        (and (> typed 0) (= :backspace code))
        (swap! *state update :typing subs 0 (dec typed))
        
        (and (= 5 typed) (= :enter code))
        (when (contains? @*dictionary typing)
          (swap! *state #(-> % (assoc :typing "") (update :guesses conj typing))))))))

(defn color [word letter idx]
  (cond
    (= (str (nth word idx)) (str letter)) :green
    (str/includes? word (str letter)) :yellow
    :else :gray))

(defn merge-colors [a b]
  (cond
    (= :green b)  :green
    (= :green a)  :green
    (= :yellow b) :yellow
    (= :yellow a) :yellow
    :else         :gray))

(defn colors [word guesses]
  (apply merge-with merge-colors {}
    (for [guess guesses
          [letter idx] (map vector guess (range))]
      {(str letter) (color word letter idx)})))

(defn field []
  (let [{:keys [stroke-light-gray stroke-dark-gray fill-green fill-yellow fill-dark-gray fill-white fill-black]} ui/*ctx*
        {:keys [word guesses typing] :as state} @*state
        fill (fn [letter idx]
               (case (color word letter idx)
                 :green  fill-green
                 :yellow fill-yellow
                 :gray   fill-dark-gray))]
    [ui/column {:gap padding}
     (for [guess guesses]
       [ui/row {:gap padding}
        (for [[letter idx] (map vector guess (range))]
          [ui/rect {:paint (fill letter idx)}
           [ui/size {:width 50, :height 50}
            [ui/center
             [ui/label {:font-cap-height 18, :font-weight :bold, :paint fill-white} letter]]]])])
     (when-not (won? state)
       (let [colors (colors word guesses)]        
         (list
           [ui/gap {:height padding}]
           [ui/row {:gap padding}
            (for [idx (range 0 5)]
              (if-some [letter (when (< idx (count typing))
                                 (str (nth typing idx)))]
                [ui/rect {:paint stroke-dark-gray}
                 [ui/size {:width 50, :height 50}
                  [ui/center
                   (let [color (cond
                                 (= :gray (colors letter))
                                 fill-dark-gray
                                            
                                 (some #(= (str (nth % idx)) letter) guesses)
                                 (fill letter idx)
                                              
                                 (some? (colors letter))
                                 fill-yellow
                                            
                                 :else
                                 fill-black)]
                     [ui/label {:font-cap-height 18, :font-weight :bold, :paint color} letter])]]]
                [ui/rect {:paint stroke-light-gray}
                 [ui/gap {:width 50 :height 50}]]))])))]))

(defn key
  ([char]
   (key {:width 25 :code char} char))
  ([{:keys [width code]} char]
   [ui/clickable
    {:on-click (fn [_] (type! code))}
    (let [{:keys [fill-green fill-yellow fill-dark-gray fill-light-gray fill-black fill-white]} ui/*ctx*
          color (get (:colors ui/*ctx*) char)]
      [ui/rect {:paint (case color
                         :green  fill-green
                         :yellow fill-yellow
                         :gray   fill-dark-gray
                         nil     fill-light-gray)}
       [ui/size {:width width, :height 35}
        [ui/center
         [ui/label {:font-weight :bold
                    :paint (if (some? color) fill-white fill-black)} char]]]])]))
  
(defn keyboard []
  (let [{:keys [word guesses]} @*state]
    [ui/with-context {:colors (colors word guesses)}
     [ui/column {:gap padding}
      [ui/align {:x :center}
       [ui/row  {:gap padding}
        (map (fn [%] [key (str %)]) "ABCDEFGHIJ")]]
      [ui/align {:x :center}
       [ui/row  {:gap padding}
        (map (fn [%] [key (str %)]) "KLMNOPQRST")]]
      [ui/align {:x :center}
       [ui/row  {:gap padding}
        [key {:width (+ (* 2 25) padding), :code :enter} "⏎"]
        (map (fn [%] [key (str %)]) "UVWXYZ")
        [key {:width (+ (* 2 25) padding), :code :backspace} "⌫"]]]]]))

(defn ui []
  [ui/with-context
   {:fill-white        {:fill 0xFFFFFFFF}
    :fill-black        {:fill 0xFF000000}
    :fill-light-gray   {:fill 0xFFD4D6DA}
    :fill-dark-gray    {:fill 0xFF777C7E}
    :fill-green        {:fill 0xFF6AAA64}
    :fill-yellow       {:fill 0xFFC9B457}
    :stroke-light-gray {:stroke 0xFFD4D6DA, :width 2}
    :stroke-dark-gray  {:stroke 0xFF777C7E, :width 2}}
   [ui/key-listener {:on-key-down #(type! (:key %))}
    [ui/text-listener {:on-input #(type! (str/upper-case %))}
     [ui/padding {:padding 20}
      [ui/column
       [ui/align {:x :center}
        [ui/clickable
         {:on-click (fn [_] (reset! *state (empty-state)))}
         [ui/padding {:padding 10}
          [ui/label {:font-weight :bold, :paint {:fill 0xFF000000}} "↻ Reset"]]]]
       [ui/gap {:height padding}]
       ^{:stretch 1} [ui/gap]
       [ui/align {:x :center}
        [field]]
       ^{:stretch 1} [ui/gap]
       [ui/gap {:height padding}]
       [ui/align {:x :center}
        [keyboard]]]]]]])
