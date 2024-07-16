(in-ns 'io.github.humbleui.ui)

(def ^:private checkbox-states
  {[true  false]  (util/lazy-resource "ui/checkbox/on.svg")
   [true  true]   (util/lazy-resource "ui/checkbox/on_pressed.svg")
   [false false]  (util/lazy-resource "ui/checkbox/off.svg")
   [false true]   (util/lazy-resource "ui/checkbox/off_pressed.svg")
   [:mixed false] (util/lazy-resource "ui/checkbox/mixed.svg")
   [:mixed true]  (util/lazy-resource "ui/checkbox/mixed_pressed.svg")})

(defn- checkbox-size [ctx]
  (let [cap-height (cap-height)
        scale      (scale)
        extra      (-> cap-height (* scale) (/ 8) math/ceil (* 4) (/ scale))] ;; half cap-height but increased so that it’s divisible by 4
    (+ cap-height extra)))

(defn checkbox-ctor
  ([child]
   (checkbox-ctor {} child))
  ([opts child]
   (let [value-on  (if (contains? opts :value-on) (:value-on opts) true)
         value-off (if (contains? opts :value-off) (:value-off opts) false)
         *value    (or (:*value opts) (signal/signal value-off))
         opts'     (assoc opts
                     :value-on  value-on
                     :value-off value-off
                     :*value    *value)]
     {:should-setup?
      (fn [opts' child]
        (not (keys-match? [:value-on :value-off :*value] opts opts')))
      :render
      (fn render
        ([child]
         (render {} child))
        ([opts child]
         [toggleable opts'
          (fn [state]
            (let [checkbox-size (checkbox-size *ctx*)]
              [row
               [align {:y :center}
                [size {:width checkbox-size, :height checkbox-size}
                 [svg {:src @(checkbox-states [(cond
                                                 (= :mixed @*value) :mixed
                                                 (:selected state)  true
                                                 :else              false)
                                               (boolean (:pressed state))])}]]]
               [gap {:width (/ checkbox-size 3)}]
               [align {:y :center}
                child]]))]))})))
