(ns examples.calculator
  (:require
    [clojure.string :as str]
    [clojure.test :as test :refer [is are deftest testing]]
    [io.github.humbleui.core :refer [cond+]]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint Font]))

(set! *warn-on-reflection* true)

(def *state (atom {:b "0" :screen :b}))

(defn stringify [n]
  (let [s (str n)]
    (if (str/ends-with? s ".0")
      (subs s 0 (- (count s) 2))
      s)))

(defn on-click
  ([s] (swap! *state on-click s))
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
    "1+2=4"  "4"
    ; "1+2=4+" "4"
    ))

(comment (test/run-test test-logic))

(defn button [text]
  (ui/clickable
    #(on-click text)
    (ui/dynamic ctx [{:keys [hui/active? hui/hovered? font-ui fill-text]} ctx]
      (let [color (cond
                    active?  0xFFA2C7EE
                    :else    0xFFB2D7FE)]
        (ui/fill (doto (Paint.) (.setColor (unchecked-int color)))
          (ui/halign 0.5
            (ui/valign 0.5
              (ui/label text font-ui fill-text))))))))

(def ui
  (ui/with-bounds ::bounds
    (ui/dynamic ctx [{:keys [font-ui scale]} ctx
                     height (:height (::bounds ctx))]
      (let [font-ui     ^Font font-ui
            size        (.getSize font-ui)
            cap-height  (-> font-ui .getMetrics .getCapHeight)
            btn-height  (-> height (- 70) (/ 6))
            cap-height' (-> btn-height (/ 3) (* scale) (Math/floor))
            size'       (-> size (/ cap-height) (* cap-height'))]
        (ui/dynamic _ [size' size']
          (ui/with-context {:font-ui (.makeWithSize font-ui size')}  
            (ui/padding 10 10
              (ui/column
                [:stretch 1 (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFFB2D7FE)))
                              (ui/padding 10 10
                                (ui/halign 1
                                  (ui/valign 0.5
                                    (ui/dynamic ctx [{:keys [font-ui fill-text]} ctx
                                                     val (get @*state (:screen @*state))]
                                      (ui/label val font-ui fill-text))))))]
                [:hug nil (ui/gap 0 10)]
                [:stretch 1 (ui/row
                              [:stretch 2 (button "C")]
                              [:hug nil (ui/gap 10 0)]
                              [:stretch 1 (button "±")]
                              [:hug nil (ui/gap 10 0)]
                              [:stretch 1 (button "÷")])]
                [:hug nil (ui/gap 0 10)]
                [:stretch 1 (ui/row
                              [:stretch 1 (button "7")]
                              [:hug nil (ui/gap 10 0)]
                              [:stretch 1 (button "8")]
                              [:hug nil (ui/gap 10 0)]
                              [:stretch 1 (button "9")]
                              [:hug nil (ui/gap 10 0)]
                              [:stretch 1 (button "×")])]
                [:hug nil (ui/gap 0 10)]
                [:stretch 1 (ui/row
                              [:stretch 1 (button "4")]
                              [:hug nil (ui/gap 10 0)]
                              [:stretch 1 (button "5")]
                              [:hug nil (ui/gap 10 0)]
                              [:stretch 1 (button "6")]
                              [:hug nil (ui/gap 10 0)]
                              [:stretch 1 (button "−")])]
                [:hug nil (ui/gap 0 10)]
                [:stretch 1 (ui/row
                              [:stretch 1 (button "1")]
                              [:hug nil (ui/gap 10 0)]
                              [:stretch 1 (button "2")]
                              [:hug nil (ui/gap 10 0)]
                              [:stretch 1 (button "3" )]
                              [:hug nil (ui/gap 10 0)]
                              [:stretch 1 (button "+")])]
                [:hug nil (ui/gap 0 10)]
                [:stretch 1 (ui/row
                              [:stretch 2 (button "0")]
                              [:hug nil (ui/gap 10 0)]
                              [:stretch 1 (button ".")]
                              [:hug nil (ui/gap 10 0)]
                              [:stretch 1 (button "=")])]))))))))
