(ns examples.grid
  (:require
    [examples.shared :as shared]
    [clojure.string :as str]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.typeface :as typeface]
    [io.github.humbleui.ui :as ui]))

(let [[head & tail]
      (->> (slurp "dev/examples/currency.csv")
        (str/split-lines)
        (mapv #(str/split % #",")))]
  (def header head)
  (def currencies tail))

(def *state
  (ui/signal
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

(defn ui []
  (let [{:keys [currencies
                sort-col
                sort-dir]} @*state]
    [ui/align {:y :center}
     [ui/vscroll
      [ui/align {:x :center}
       [ui/padding {:padding 20}
        [ui/grid {:cols (count header)
                  :rows (inc (count currencies))}
         (concat
           (for [[th i] (util/zip header (range))]
             [ui/clickable
              {:on-click (on-click i)}
              [ui/padding {:padding 10}
               [ui/reserve-width
                {:probes [[ui/label (str th " ⏶")]
                          [ui/label (str th " ⏷")]]}
                [ui/align {:x :left}
                 [ui/label {:font-weight :bold}
                  (str th
                    (case (when (= i sort-col)
                            sort-dir)
                      :asc  " ⏶"
                      :desc " ⏷"
                      nil   ""))]]]]])
           (for [row currencies
                 s   row]
             [ui/padding {:padding 10}
              [ui/label s]]))]]]]]))
