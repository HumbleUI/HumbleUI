(ns examples.calculator
  (:require
    [clojure.string :as str]
    [clojure.test :as test :refer [are deftest]]
    [io.github.humbleui.util :as util]
        [io.github.humbleui.signal :as signal]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Font Typeface]))

(def *state
  (signal/signal
    {:b "0"
     :screen :b}))

(defn stringify [n]
  (let [s (str n)]
    (if (str/ends-with? s ".0")
      (subs s 0 (- (count s) 2))
      s)))

(defn on-click
  ([s]
   (swap! *state on-click s))
  ([state s]
   (let [{:keys [a op b screen]} state]
     (case s
       "C"
       {:b "0" :screen :b}
      
       ("0" "1" "2" "3" "4" "5" "6" "7" "8" "9")
       (cond
         (= screen :a)
         (assoc state :screen :b, :b s)
        
         (= b "0")
         (assoc state :b s)
        
         (nil? b)
         (assoc state :b s)
        
         (= b "-0")
         (assoc state :b (str "-" s))
        
         :else
         (update state :b str s))
      
       "."
       (cond
         (= :a screen)
         (assoc state :screen :b, :b "0.")
        
         (not (str/includes? b "."))
         (update state :b str "."))

       ("+" "−" "×" "÷")
       (if op 
         (-> state (on-click "=") (assoc :op s))
         (assoc state :screen :a, :a b, :op s))
      
       "="
       (when (some? op)
         (let [a (some-> a parse-double)
               b (or (some-> b parse-double) a)]
           (case op
             "+" (assoc state :screen :a :a (stringify (+ a b)))
             "−" (assoc state :screen :a :a (stringify (- a b)))
             "×" (assoc state :screen :a :a (stringify (* a b)))
             "÷" (assoc state :screen :a :a (stringify (/ a b))))))
      
       "±"
       (if (str/starts-with? b "-")
         (update state :b subs 1)
         (update state :b #(str "-" %)))))))

(deftest test-logic
  (are [keys res] (let [state' (reduce #(on-click %1 (str %2)) {:b "0" :screen :b} keys)]
                    (= res (get state' (:screen state'))))
    "1"      "1"
    "12"     "12"
    "0000"   "0"
    ".0"     "0.0"
    "1.000"  "1.000"
    "12C"    "0"
    "1±"     "-1"
    "1±±"    "1"
    "0±"     "-0"
    "0±1"    "-1"
    "0±.1"   "-0.1"
    "1+"     "1"
    "1+2"    "2"
    "1+2="   "3"
    "1+2=="  "5"
    "1+2===" "7"
    "1+="    "2"
    "1+=="   "3"
    "1+==="  "4"
    "1+2+"   "3"
    "1+2+4"  "4"
    "1+2+4=" "7"
    ".1×10=" "1"
    "1÷2="   "0.5"
    "1+2=4"  "4"))
; "1+2=4+" "4"
    

(comment (test/run-test test-logic))

(defn button [text color]
  [ui/clickable
   {:on-click (fn [_]
                (on-click text))}
   (fn [state]
     (let [color' (if (:pressed state)
                    (bit-or 0x80000000 (bit-and 0xFFFFFF color))
                    color)]
       [ui/rect {:paint {:fill color'}}
        [ui/center
         [ui/label {:features ["tnum"]} text]]]))])

(def color-digit
  0xFF797979)

(def color-op
  0xFFFF9F0A)

(def color-clear
  0xFF646464)

(def color-display
  0xFF4E4E4E)

(def padding
  1)

(defn calc-ui [bounds]
  (let [height         (:height bounds)
        btn-height     (-> height (- (* 7 padding)) (/ 13) (* 2))
        cap-height'    (-> btn-height (/ 3) (Math/floor) (max 0))
        display-height (-> height (- (* 7 padding)) (/ 13) (* 3))
        cap-height''   (-> display-height (/ 3) (Math/floor) (max 0))]
    {:should-setup?
     (fn [bounds']
       (not= (:height bounds) (:height bounds')))
     :render
     (fn [bounds]
       (let [state @*state]
         [ui/with-context
          {:font-cap-height cap-height'
           :fill-text       {:fill 0xFFEBEBEB}}
          [ui/rect {:paint {:fill color-display}}
           [ui/padding {:padding padding}
          
            [ui/column {:gap padding}
             ;; display
             ^{:stretch 3}
             [ui/rect {:paint {:fill 0xFF404040}}
              [ui/padding {:horizontal #(/ (:height %) 3)}
               [ui/align {:x :right :y :center}
                (let [val (get state (:screen state))]
                  [ui/label {:font-cap-height cap-height'' :features ["tnum"]} val])]]]
          
             ;; ops row
             ^{:stretch 2}
             [ui/row {:gap padding}
              [ui/size {:width #(-> (:width %) (- (* 3 padding)) (/ 2) (+ padding))}
               [button "C" color-clear]]
              ^{:stretch 1} [button "±" color-clear]
              ^{:stretch 1} [button "÷" color-op]]
         
             ;; top digits row
             ^{:stretch 2}
             [ui/row {:gap padding}
              ^{:stretch 1} [button "7" color-digit]
              ^{:stretch 1} [button "8" color-digit]
              ^{:stretch 1} [button "9" color-digit]
              ^{:stretch 1} [button "×" color-op]]
          
             ;; middle digits row
             ^{:stretch 2}
             [ui/row {:gap padding}
              ^{:stretch 1} [button "4" color-digit]
              ^{:stretch 1} [button "5" color-digit]
              ^{:stretch 1} [button "6" color-digit]
              ^{:stretch 1} [button "−" color-op]]
          
             ;; bottom digits row
             ^{:stretch 2}
             [ui/row {:gap padding}
              ^{:stretch 1} [button "1" color-digit]
              ^{:stretch 1} [button "2" color-digit]
              ^{:stretch 1} [button "3" color-digit]
              ^{:stretch 1} [button "+" color-op]]
          
             ;; bottom row
             ^{:stretch 2}
             [ui/row {:gap padding}
              [ui/size {:width #(-> (:width %) (- (* 3 padding)) (/ 2) (+ padding))}
               [button "0" color-digit]]
              ^{:stretch 1} [button "." color-digit]
              ^{:stretch 1} [button "=" color-op]]]]]]))}))

(ui/defcomp ui []
  [ui/with-bounds 
   (fn [bounds]
     [calc-ui bounds])])
