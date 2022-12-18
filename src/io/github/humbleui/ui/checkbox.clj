(ns io.github.humbleui.ui.checkbox
  (:require
    [clojure.math :as math]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.ui.align :as align]
    [io.github.humbleui.ui.clickable :as clickable]
    [io.github.humbleui.ui.containers :as containers]
    [io.github.humbleui.ui.dynamic :as dynamic]
    [io.github.humbleui.ui.gap :as gap]
    [io.github.humbleui.ui.sizing :as sizing]
    [io.github.humbleui.ui.svg :as svg]
    [io.github.humbleui.ui.with-context :as with-context]))

(def ^:private checkbox-states
  {[true  false]          (core/lazy-resource "ui/checkbox/on.svg")
   [true  true]           (core/lazy-resource "ui/checkbox/on_active.svg")
   [false false]          (core/lazy-resource "ui/checkbox/off.svg")
   [false true]           (core/lazy-resource "ui/checkbox/off_active.svg")
   [:indeterminate false] (core/lazy-resource "ui/checkbox/indeterminate.svg")
   [:indeterminate true]  (core/lazy-resource "ui/checkbox/indeterminate_active.svg")})

(defn- checkbox-size [font]
  (let [cap-height (:cap-height (font/metrics font))
        extra      (-> cap-height (/ 8) math/ceil (* 4))] ;; half cap-height but increased so that it’s divisible by 4
    (+ cap-height extra)))

(defn checkbox [*state label]
  (clickable/clickable
    {:on-click (fn [_] (swap! *state not))}
    (dynamic/dynamic ctx [size (/ (checkbox-size (:font-ui ctx))
                         (:scale ctx))]
      (containers/row
        (align/valign 0.5
          (dynamic/dynamic ctx [state  @*state
                        active (:hui/active? ctx)]
            (sizing/width size
              (sizing/height size
                (svg/svg @(checkbox-states [state (boolean active)]))))))
        (gap/gap (/ size 3) 0)
        (align/valign 0.5
          (with-context/with-context {:hui/checked? true}
            label))))))
