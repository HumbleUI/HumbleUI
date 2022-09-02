(ns io.github.humbleui.ui.toggle
  (:require
    [clojure.java.io :as io]
    [clojure.math :as math]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.window :as window]
    [io.github.humbleui.ui.clickable :as clickable])
  (:import
    [io.github.humbleui.types IPoint RRect]
    [io.github.humbleui.skija Color Font]))

(defn- toggle-height [ctx]
  (let [font       ^Font (:font-ui ctx)
        cap-height (.getCapHeight (.getMetrics font))
        extra      (-> cap-height (/ 8) math/ceil (* 4))] ;; half cap-height but increased so that it’s divisible by 4
    (+ cap-height extra)))

(defn start-animation [toggle]
  (let [; ratio = (now - start) / len
        ; start = now - ratio * len
        start  (:animation-start toggle)
        len    (:animation-length toggle)
        now    (core/now)
        ratio  (min 1 (/ (- now start) len))
        ratio' (- 1 ratio)
        start' (- now (* ratio' len))]
    (protocols/-set! toggle :animation-start start')))

(core/deftype+ Toggle [*state
                       animation-length
                       ^:mut state-cached
                       ^:mut animation-start
                       ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [height (toggle-height ctx)
          width  (math/round (* height 1.61803))]
      (IPoint. width height)))
  
  (-draw [this ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (let [scale    (:scale ctx)
          {x :x, y :y, w :width, h :height} rect
          enabled?     (boolean @*state)
          _            (when (not= enabled? state-cached)
                         (start-animation this)
                         (set! state-cached enabled?))
          active?      (boolean (:hui/active? ctx))
          now          (core/now)
          ratio        (min 1 (/ (- now animation-start) animation-length))
          animating?   (< ratio 1)
          {:hui.toggle/keys [fill-enabled
                             fill-disabled
                             fill-handle
                             fill-enabled-active
                             fill-disabled-active
                             fill-handle-active]} ctx
          fill         (core/match [enabled? animating? active?]
                         [true  true  _]     (paint/fill
                                               (Color/makeLerp (.getColor fill-enabled) (.getColor fill-disabled) ratio))
                         [false true  _]     (paint/fill
                                               (Color/makeLerp (.getColor fill-disabled) (.getColor fill-enabled) ratio))
                         [true  false false] fill-enabled
                         [false false false] fill-disabled
                         [true  false true]  fill-enabled-active
                         [false false true]  fill-disabled-active)
          padding      (/ h 16)
          handle-r     (-> h (- (* 2 padding)) (/ 2))
          handle-left  (-> x (+ padding) (+ handle-r))
          handle-right (-> x (+ w) (- padding) (- handle-r))
          handle-x     (if enabled?
                         (+ (* handle-right ratio) (* handle-left (- 1 ratio)))
                         (+ (* handle-left ratio) (* handle-right (- 1 ratio))))
          handle-y     (-> y (+ padding) (+ handle-r))
          handle-fill  (if active?
                         fill-handle-active
                         fill-handle)]
      (canvas/draw-rect canvas (RRect/makeXYWH x y w h (/ h 2)) fill)
      (canvas/draw-circle canvas handle-x handle-y handle-r handle-fill)
      (when animating?
        (.close fill)
        (window/request-frame (:window ctx)))))
  
  (-event [_ ctx event])
  
  (-iterate [this ctx cb]
    (cb this)))

(defn toggle
  ([*state]
   (toggle nil *state))
  ([opts *state]
   (clickable/clickable
     {:on-click (fn [_]
                  (swap! *state not))}
     (->Toggle *state 50 @*state 0 nil))))
