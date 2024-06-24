(in-ns 'io.github.humbleui.ui)

(def ^:private checkbox-states
  {[true  false]          (core/lazy-resource "ui/checkbox/on.svg")
   [true  true]           (core/lazy-resource "ui/checkbox/on_active.svg")
   [false false]          (core/lazy-resource "ui/checkbox/off.svg")
   [false true]           (core/lazy-resource "ui/checkbox/off_active.svg")
   [:indeterminate false] (core/lazy-resource "ui/checkbox/indeterminate.svg")
   [:indeterminate true]  (core/lazy-resource "ui/checkbox/indeterminate_active.svg")})

(defn- checkbox-size [ctx]
  (let [font       (:font-ui ctx)
        cap-height (:cap-height (font/metrics font))
        extra      (-> cap-height (/ 8) math/ceil (* 4))] ;; half cap-height but increased so that it’s divisible by 4
    (/
      (+ cap-height extra)
      (:scale ctx))))

(defn checkbox-ctor [opts child]
  (let [value-checked   (:value-checked opts true)
        value-unchecked (:value-unchecked opts)
        *value          (or (:*value opts) (signal/signal value-unchecked))
        on-click        (fn [_]
                          (swap! *value #(not %)))]
    {:should-setup?
     (fn [opts' _]
       (not (keys-match? [:value-checked :value-unchecked :*value] opts opts')))
     :render
     (fn [opts child]
       (let [value @*value]
         [clickable
          {:on-click on-click}
          (fn [state]
            (let [size (checkbox-size *ctx*)]
              [row
               [valign {:position 0.5}
                [width {:width size}
                 [height {:height size}
                  [svg @(checkbox-states [(cond
                                            (= :indeterminate value) :indeterminate
                                            (= value-checked value)  true
                                            :else                    false)
                                          (= :pressed state)])]]]]
               [gap {:width (/ size 3)}]
               [valign {:position 0.5}
                child]]))]))}))
