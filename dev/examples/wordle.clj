(ns examples.wordle
  (:refer-clojure :exclude [key type])
  (:require
    [clojure.string :as str]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Font Paint Typeface]))

(set! *warn-on-reflection* true)

(def ^Typeface typeface
  (Typeface/makeFromFile "dev/fonts/Inter-Bold.ttf"))

(def padding 4)

;; https://github.com/AllValley/WordleDictionary
(def dictionary
  (->> (slurp "dev/examples/wordle_dictionary.txt")
    (str/split-lines)
    (map str/upper-case)
    (set)))

(def solutions
  (->> (slurp "dev/examples/wordle_solutions.txt")
    (str/split-lines)
    (mapv str/upper-case)))

(defn empty-state []
  {:word    (rand-nth solutions)
   :guesses []
   :typing  ""})

(def *state
  (atom (empty-state)
    #_{:word    "QUART"
     :guesses ["CRANE" "START"]
     :typing  "SSRAT"}))

(defn won? [{:keys [word guesses]}]
  (= (last guesses) word))

(defn type [code]
  (let [{:keys [typing] :as state} @*state
        typed (count typing)]
    (when-not (won? @*state)
      (cond
        (and (< typed 5) (contains? (into #{} (map str) "ABCDEFGHIJKLMNOPQRSTUVWXYZ") code))
        (swap! *state update :typing str code)

        (and (> typed 0) (= "Backspace" code))
        (swap! *state update :typing subs 0 (dec typed))
        
        (and (= 5 typed) (= "Enter" code))
        (if (contains? dictionary typing)
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

(def field
  (ui/dynamic ctx [{:keys [font-large stroke-light-gray stroke-dark-gray fill-green fill-yellow fill-dark-gray fill-white fill-black]} ctx
                   {:keys [word guesses typing] :as state} @*state]
    (let [fill (fn [letter idx]
                 (case (color word letter idx)
                   :green  fill-green
                   :yellow fill-yellow
                   :gray   fill-dark-gray))]
      (ui/column
        (interpose (ui/gap 0 padding)
          (for [guess guesses]
            (ui/row
              (interpose (ui/gap padding 0)
                (for [[letter idx] (map vector guess (range))]
                  (ui/fill (fill letter idx)
                    (ui/width 50
                      (ui/halign 0.5
                        (ui/height 50
                          (ui/valign 0.5
                            (ui/label (str letter) font-large fill-white)))))))))))
        (when-not (won? state)
          (let [colors (colors word guesses)]        
            (list
              (ui/gap 0 padding)
              (ui/row
                (interpose (ui/gap padding 0)
                  (for [idx (range 0 5)]
                    (if-some [letter (when (< idx (count typing))
                                       (str (nth typing idx)))]
                      (ui/fill stroke-dark-gray
                        (ui/width 50
                          (ui/halign 0.5
                            (ui/height 50
                              (ui/valign 0.5
                                (let [color (cond
                                              (= :gray (colors letter))
                                              fill-dark-gray
                                            
                                              (some #(= (str (nth % idx)) letter) guesses)
                                              (fill letter idx)
                                              
                                              (some? (colors letter))
                                              fill-yellow
                                            
                                              :else
                                              fill-black)]
                                  (ui/label letter font-large color)))))))
                      (ui/fill stroke-light-gray
                        (ui/gap 50 50)))))))))))))

(defn key
  ([char] (key char {:width 25 :code char}))
  ([char {:keys [width code]}]
   (ui/clickable
     #(type code)
     (ui/dynamic ctx [{:keys [font-small fill-green fill-yellow fill-dark-gray fill-light-gray fill-black fill-white]} ctx
                      color (get (:colors ctx) char)]
       (ui/fill
         (case color
           :green  fill-green
           :yellow fill-yellow
           :gray   fill-dark-gray
           nil     fill-light-gray)
         (ui/width width
           (ui/halign 0.5
             (ui/height 35
               (ui/valign 0.5
                 (ui/label char font-small (if (some? color) fill-white fill-black)))))))))))
  
(def keyboard
  (ui/dynamic ctx [{:keys [font-small fill-light-gray fill-black]} ctx
                   {:keys [word guesses]} @*state]
    (ui/with-context {:colors (colors word guesses)}    
      (ui/column
        (ui/gap 0 padding)
        (ui/halign 0.5
          (ui/row
            (interpose (ui/gap padding 0)
              (map #(key (str %)) "ABCDEFGHIJ"))))
        (ui/gap 0 padding)
        (ui/halign 0.5
          (ui/row
            (interpose (ui/gap padding 0)
              (map #(key (str %)) "KLMNOPQRST"))))
        (ui/gap 0 padding)
        (ui/halign 0.5
          (ui/row
            (key "⏎" {:width (+ (* 2 25) padding), :code "Enter"})
            (ui/gap padding 0)
            (interpose (ui/gap padding 0)
              (map #(key (str %)) "UVWXYZ"))
            (ui/gap padding 0)
            (key "⌫" {:width (+ (* 2 25) padding), :code "Backspace"})))))))

(def ui
  (ui/on-key-down #(type (:hui.event.key/key %))
    (ui/padding padding padding
      (ui/dynamic ctx [{:keys [scale face-ui]} ctx]
        (let [font-small (Font. typeface (float (* scale 13)))
              fill-black (paint/fill 0xFF000000)
              fill-light-gray (paint/fill 0xFFD4D6DA)]
          (ui/with-context
            {:font-large      (Font. typeface (float (* scale 26)))
             :font-small      font-small
             :fill-white      (paint/fill 0xFFFFFFFF)
             :fill-black      fill-black
             :fill-light-gray fill-light-gray
             :fill-dark-gray  (paint/fill 0xFF777C7E)
             :fill-green      (paint/fill 0xFF6AAA64)
             :fill-yellow     (paint/fill 0xFFC9B457)
             :stroke-light-gray (paint/stroke 0xFFD4D6DA (* 2 scale))
             :stroke-dark-gray  (paint/stroke 0xFF777C7E (* 2 scale))}
            (ui/column
              (ui/halign 0.5
                (ui/clickable
                  #(reset! *state (empty-state))
                  (ui/padding 10 10
                    (ui/label "↻ Reset" font-small fill-black))))
              (ui/gap 0 padding)
              [:stretch 1 nil]
              (ui/halign 0.5 field)
              [:stretch 1 nil]
              (ui/gap 0 padding)
              (ui/halign 0.5 keyboard))))))))

(require 'user :reload)
(reset! user/*example "Wordle")