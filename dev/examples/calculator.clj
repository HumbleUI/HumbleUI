(ns examples.calculator
  (:require
    [clojure.string :as str]
    [io.github.humbleui.ui :as ui])
  (:import
    [io.github.humbleui.skija Paint]))

(set! *warn-on-reflection* true)

(def *a  (atom 0))
(def *op (atom "+"))
(def *b  (atom nil))

(defn button
  ([text] (button text (fn [])))
  ([text on-click]
   (ui/clickable
     on-click
     (ui/dynamic ctx [{:keys [hui/active? hui/hovered? font-ui fill-text]} ctx]
       (let [color (cond
                     active?  0xFFA2C7EE
                     :else    0xFFB2D7FE)]
         (ui/fill (doto (Paint.) (.setColor (unchecked-int color)))
           (ui/halign 0.5
             (ui/valign 0.5
               (ui/label text font-ui fill-text)))))))))

(defn clear! []
  (reset! *a 0)
  (reset! *op "+")
  (reset! *b nil))

(defn eq! []
  (case @*op
    "+" (swap! *b #(+ @*a %))
    "-" (swap! *b #(- @*a %))
    "*" (swap! *b #(* @*a %))
    "/" (swap! *b #(/ @*a %)))
  (reset! *a 0)
  (reset! *op "+"))

(defn append! [n]
  (swap! *b #(+ (* (or % 0) 10) n)))

(defn op! [op]
  (eq!)
  (reset! *a @*b)
  (reset! *op op)
  (reset! *b nil))

(def ui
  (ui/padding 10 10
    (ui/column
      [:stretch 1 (ui/fill (doto (Paint.) (.setColor (unchecked-int 0xFFB2D7FE)))
                    (ui/padding 10 10
                      (ui/halign 1
                        (ui/valign 0.5
                          (ui/dynamic ctx [{:keys [font-ui fill-text]} ctx
                                           a  @*a
                                           op @*op
                                           b  @*b]
                            (ui/label (str (or b a)) font-ui fill-text))))))]
      [:hug nil (ui/gap 0 10)]
      [:stretch 1 (ui/row
                    [:stretch 1 (button "7" #(append! 7))]
                    [:hug nil (ui/gap 10 0)]
                    [:stretch 1 (button "8" #(append! 8))]
                    [:hug nil (ui/gap 10 0)]
                    [:stretch 1 (button "9" #(append! 9))]
                    [:hug nil (ui/gap 10 0)]
                    [:stretch 1 (button "+" #(op! "+"))])]
      [:hug nil (ui/gap 0 10)]
      [:stretch 1 (ui/row
                    [:stretch 1 (button "4" #(append! 4))]
                    [:hug nil (ui/gap 10 0)]
                    [:stretch 1 (button "5" #(append! 5))]
                    [:hug nil (ui/gap 10 0)]
                    [:stretch 1 (button "6" #(append! 6))]
                    [:hug nil (ui/gap 10 0)]
                    [:stretch 1 (button "-" #(op! "-"))])]
      [:hug nil (ui/gap 0 10)]
      [:stretch 1 (ui/row
                    [:stretch 1 (button "1" #(append! 1))]
                    [:hug nil (ui/gap 10 0)]
                    [:stretch 1 (button "2" #(append! 2))]
                    [:hug nil (ui/gap 10 0)]
                    [:stretch 1 (button "3"  #(append! 3))]
                    [:hug nil (ui/gap 10 0)]
                    [:stretch 1 (button "×" #(op! "*"))])]
      [:hug nil (ui/gap 0 10)]
      [:stretch 1 (ui/row
                    [:stretch 1 (button "C" clear!)]
                    [:hug nil (ui/gap 10 0)]
                    [:stretch 1 (button "0" #(append! 0))]
                    [:hug nil (ui/gap 10 0)]
                    [:stretch 1 (button "=" eq!)]
                    [:hug nil (ui/gap 10 0)]
                    [:stretch 1 (button "÷" #(op! "/"))])])))
