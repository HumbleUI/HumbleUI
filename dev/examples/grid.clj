(ns examples.grid
  (:require
    [clojure.string :as str]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.typeface :as typeface]
    [io.github.humbleui.ui :as ui]))

(def face-bold
  (typeface/make-from-resource "io/github/humbleui/fonts/Inter-Bold.ttf"))

(let [[head & tail]
      (->> (slurp "dev/examples/currency.csv")
        (str/split-lines)
        (mapv #(str/split % #",")))]
  (def header head)
  (def currencies tail))

(def *state
  (atom
    {:sort-col   0
     :sort-dir   :asc
     :currencies currencies}))

(defn on-click [i]
  (fn [e]
    (swap! *state
      (fn [{:keys [sort-col sort-dir] :as state}]
        (cond
          (not= i sort-col)
          {:sort-col   i
           :sort-dir   :asc
           :currencies (->> currencies (sort-by #(nth % i)))}
          
          (= :asc sort-dir)
          {:sort-col   i
           :sort-dir   :desc
           :currencies (->> currencies (sort-by #(nth % i)) reverse)}
          
          (= :desc sort-dir)
          {:sort-col   i
           :sort-dir   :asc
           :currencies (->> currencies (sort-by #(nth % i)))})))))

(def ui
  (ui/with-scale scale
    (let [font-bold (font/make-with-cap-height face-bold (* scale 10))]
      (ui/vscrollbar
        (ui/halign 0.5
          (ui/dynamic _ [{:keys [currencies sort-col sort-dir]} @*state]
            (ui/grid
              (concat
                [(for [[th i] (core/zip header (range))]
                   (ui/clickable
                     {:on-click (on-click i)}
                     (ui/padding 10
                       (ui/with-context
                         {:font-ui font-bold}
                         (ui/max-width
                           [(ui/label (str th " ▲"))
                            (ui/label (str th " ▼"))]
                           (ui/halign 0
                             (ui/label
                               (str th
                                 (case (when (= i sort-col)
                                         sort-dir)
                                   :asc  " ▲"
                                   :desc " ▼"
                                   nil   "")))))))))]
                (for [row currencies]
                  (for [s row]
                    (ui/padding 10
                      (ui/label s))))))))))))
