(ns io.github.humbleui.ui
  (:require
    [clojure.java.io :as io]
    [clojure.math :as math]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.profile :as profile]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.window :as window]
    [io.github.humbleui.ui.clickable :as clickable]
    [io.github.humbleui.ui.dynamic :as dynamic]
    [io.github.humbleui.ui.focusable :as focusable]
    [io.github.humbleui.ui.key-listener :as key-listener]
    [io.github.humbleui.ui.rect :as rect]
    [io.github.humbleui.ui.slider :as slider]
    [io.github.humbleui.ui.stack :as stack]
    [io.github.humbleui.ui.text-field :as text-field]
    [io.github.humbleui.ui.theme :as theme]
    [io.github.humbleui.ui.toggle :as toggle]
    [io.github.humbleui.ui.with-context :as with-context]
    [io.github.humbleui.ui.with-cursor :as with-cursor])
  (:import
    [java.lang AutoCloseable]
    [java.io File]
    [io.github.humbleui.types IPoint IRect Point Rect RRect]
    [io.github.humbleui.skija Canvas Data Font FontMetrics Image Paint Surface TextLine]
    [io.github.humbleui.skija.shaper Shaper ShapingOptions]
    [io.github.humbleui.skija.svg SVGDOM SVGSVG SVGLength SVGPreserveAspectRatio SVGPreserveAspectRatioAlign SVGPreserveAspectRatioScale]))

(set! *warn-on-reflection* true)

(def ^{:arglists '([opts child])} clickable
  clickable/clickable)

(defmacro dynamic [ctx-sym bindings & body]
  (dynamic/dynamic-impl ctx-sym bindings body))

(def ^{:arglists '([child] [opts child])} focusable
  focusable/focusable)

(def ^{:arglists '([child])} focus-controller
  focusable/focus-controller)

(def ^{:arglists '([opts child])} key-listener
  key-listener/key-listener)

(def ^{:arglists '([paint child])} rect
  rect/rect)

(def ^{:arglists '([opts paint child])} rounded-rect
  rect/rounded-rect)

(def ^{:arglists '([*state] [opts *state])} slider
  slider/slider)

(def ^{:arglists '([*state] [opts *state])} stack
  stack/stack)

(def ^{:arglists '([comp] [opts comp])} default-theme
  theme/default-theme)

(def ^{:arglists '([*state] [opts *state])} text-input
  text-field/text-input)

(def ^{:arglists '([*state] [opts *state])} text-field
  text-field/text-field)

(def ^{:arglists '([*state] [opts *state])} toggle
  toggle/toggle)

(def ^{:arglists '([data child])} with-context  
  with-context/with-context)

(def ^{:arglists '([cursor child])} with-cursor
  with-cursor/with-cursor)

;; with-bounds

(core/deftype+ WithBounds [key child ^:mut child-rect]
  protocols/IContext
  (-context [_ ctx]
    (let [scale  (:scale ctx)
          width  (-> (:width child-rect) (/ scale))
          height (-> (:height child-rect) (/ scale))]
      (assoc ctx key (IPoint. width height))))
  
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [width  (-> (:width cs) (/ (:scale ctx)))
          height (-> (:height cs) (/ (:scale ctx)))]
      (core/measure child (assoc ctx key (IPoint. width height)) cs)))
  
  (-draw [this ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (core/draw-child child (protocols/-context this ctx) child-rect canvas))
  
  (-event [this ctx event]
    (core/event-child child (protocols/-context this ctx) event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child (protocols/-context this ctx) cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn with-bounds [key child]
  (->WithBounds key child nil))


;; label

(core/deftype+ Label [^String text ^Font font ^Paint paint ^TextLine line ^FontMetrics metrics]
  protocols/IComponent
  (-measure [_ ctx cs]
    (IPoint.
      (Math/ceil (.getWidth line))
      (Math/ceil (.getCapHeight metrics))))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (.drawTextLine canvas line (:x rect) (+ (:y rect) (Math/ceil (.getCapHeight metrics))) paint))
  
  (-event [_ ctx event])
  
  (-iterate [this ctx cb]
    (cb this))
  
  AutoCloseable
  (close [_]
    #_(.close line))) ; TODO

(defn label
  ([text]
   (label text nil))
  ([text opts]
   (dynamic ctx [font ^Font (or (:font opts) (:font-ui ctx))
                 paint ^Paint (or (:paint opts) (:fill-text ctx))]
     (let [text     (str text)
           features (reduce #(.withFeatures ^ShapingOptions %1 ^String %2) ShapingOptions/DEFAULT (:features opts))
           line     (.shapeLine core/shaper text font ^ShapingOptions features)]
       (->Label text font paint line (.getMetrics ^Font font))))))

;; gap

(core/deftype+ Gap [width height]
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [{:keys [scale]} ctx]
      (IPoint. (math/ceil (* scale width)) (math/ceil (* scale height))))) 
  (-draw [_ ctx rect canvas])
  
  (-event [_ ctx event])
  
  (-iterate [this ctx cb]
    (cb this)))

(defn gap [width height]
  (->Gap width height))


;; halign

(core/deftype+ HAlign [child-coeff coeff child ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [layer      (.save canvas)
          child-size (core/measure child ctx (IPoint. (:width rect) (:height rect)))
          left       (+ (:x rect)
                       (* (:width rect) coeff)
                       (- (* (:width child-size) child-coeff)))]
      (set! child-rect (IRect/makeXYWH left (:y rect) (:width child-size) (:height rect)))
      (core/draw-child child ctx child-rect canvas)))
  
  (-event [_ ctx event]
    (core/event-child child ctx event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn halign
  ([coeff child] (halign coeff coeff child))
  ([child-coeff coeff child] (->HAlign child-coeff coeff child nil)))


;; valign

(core/deftype+ VAlign [child-coeff coeff child ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [layer      (.save canvas)
          child-size (core/measure child ctx (IPoint. (:width rect) (:height rect)))
          top        (+ (:y rect)
                       (* (:height rect) coeff)
                       (- (* (:height child-size) child-coeff)))]
      (set! child-rect (IRect/makeXYWH (:x rect) top (:width rect) (:height child-size)))
      (core/draw-child child ctx child-rect canvas)))
  
  (-event [_ ctx event]
    (core/event-child child ctx event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn valign
  ([coeff child] (valign coeff coeff child))
  ([child-coeff coeff child] (->VAlign child-coeff coeff child nil)))


;; width

(core/deftype+ Width [value child ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [width'     (core/dimension value cs ctx)
          child-size (core/measure child ctx (assoc cs :width width'))]
      (assoc child-size :width width')))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (core/draw-child child ctx child-rect canvas))
  
  (-event [_ ctx event]
    (core/event-child child ctx event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn width [value child]
  (->Width value child nil))


;; height

(core/deftype+ Height [value child ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [height'    (core/dimension value cs ctx)
          child-size (core/measure child ctx (assoc cs :height height'))]
      (assoc child-size :height height')))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (core/draw child ctx rect canvas))
  
  (-event [_ ctx event]
    (core/event-child child ctx event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn height [value child]
  (->Height value child nil))


;; column

(core/deftype+ Column [children ^:mut child-rects]
  protocols/IComponent
  (-measure [_ ctx cs]
    (reduce
      (fn [{:keys [width height]} child]
        (let [child-size (core/measure child ctx cs)]
          (IPoint. (max width (:width child-size)) (+ height (:height child-size)))))
      (IPoint. 0 0)
      (keep #(nth % 2) children)))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [[known space] (core/loopr
                          [known []
                           space (:height rect)]
                          [[mode _ child] children]
                          (if (= :hug mode)
                            (let [cs   (IPoint. (:width rect) space)
                                  size (core/measure child ctx cs)]
                              (recur (conj known size) (- space (:height size))))
                            (recur (conj known nil) space)))
          stretch (transduce (keep (fn [[mode value _]] (when (= :stretch mode) value))) + 0 children)
          layer   (.save canvas)]
      (loop [height   0
             rects    []
             known    known
             children children]
        (if-some [[mode value child] (first children)]
          (let [child-height (long
                               (case mode
                                 :hug     (:height (first known))
                                 :stretch (-> space (/ stretch) (* value) (math/round))))
                child-rect (IRect/makeXYWH (:x rect) (+ (:y rect) height) (max 0 (:width rect)) (max 0 child-height))]
            (core/draw-child child ctx child-rect canvas)
            (recur
              (+ height child-height)
              (conj rects child-rect)
              (next known)
              (next children)))
          (set! child-rects rects)))))
  
  (-event [_ ctx event]
    (reduce
      (fn [acc [_ _ child]]
        (core/eager-or acc (core/event-child child ctx event)))
      false
      children))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (some (fn [[_ _ child]] (protocols/-iterate child ctx cb)) children)))
  
  AutoCloseable
  (close [_]
    (doseq [[_ _ child] children]
      (core/child-close child))))

(defn- flatten-container [children]
  (into []
    (mapcat
      #(cond
         (nil? %)        []
         (vector? %)     [%]
         (sequential? %) (flatten-container %)
         :else           [[:hug nil %]]))
    children))

(defn column [& children]
  (->Column (flatten-container children) nil))


;; row

(core/deftype+ Row [children ^:mut child-rects]
  protocols/IComponent
  (-measure [_ ctx cs]
    (reduce
      (fn [{:keys [width height]} child]
        (let [child-size (core/measure child ctx cs)]
          (IPoint. (+ width (:width child-size)) (max height (:height child-size)))))
      (IPoint. 0 0)
      (keep #(nth % 2) children)))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [[known space] (core/loopr
                          [known []
                           space (:width rect)]
                          [[mode _ child] children]
                          (if (= :hug mode)
                            (let [cs   (IPoint. space (:height rect))
                                  size (core/measure child ctx cs)]
                              (recur (conj known size) (- space (:width size))))
                            (recur (conj known nil) space)))
          stretch (transduce (keep (fn [[mode value _]] (when (= :stretch mode) value))) + 0 children)
          layer   (.save canvas)]
      (loop [width    0
             rects    []
             known    known
             children children]
        (if-some [[mode value child] (first children)]
          (let [child-size (case mode
                             :hug     (first known)
                             :stretch (IPoint. (-> space (/ stretch) (* value) (math/round)) (:height rect)))
                child-rect (IRect/makeXYWH (+ (:x rect) width) (:y rect) (max 0 (:width child-size)) (max 0 (:height rect)))]
            (core/draw-child child ctx child-rect canvas)
            (recur
              (+ width (long (:width child-size)))
              (conj rects )
              (next known)
              (next children)))
          (set! child-rects rects)))))
  
  (-event [_ ctx event]
    (reduce
      (fn [acc [_ _ child]]
        (core/eager-or acc (core/event-child child ctx event) false))
      false
      children))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (some (fn [[_ _ child]] (protocols/-iterate child ctx cb)) children)))
  
  AutoCloseable
  (close [_]
    (doseq [[_ _ child] children]
      (core/child-close child))))

(defn row [& children]
  (->Row (flatten-container children) nil))


;; padding

(core/deftype+ Padding [left top right bottom child ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [left'      (core/dimension left cs ctx)
          right'     (core/dimension right cs ctx)
          top'       (core/dimension top cs ctx)
          bottom'    (core/dimension bottom cs ctx)
          child-cs   (IPoint. (- (:width cs) left' right') (- (:height cs) top' bottom'))
          child-size (core/measure child ctx child-cs)]
      (IPoint.
        (+ (:width child-size) left' right')
        (+ (:height child-size) top' bottom'))))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [left'    (core/dimension left rect ctx)
          right'   (core/dimension right rect ctx)
          top'     (core/dimension top rect ctx)
          bottom'  (core/dimension bottom rect ctx)
          layer    (.save canvas)
          width'   (- (:width rect) left' right')
          height'  (- (:height rect) top' bottom')]
      (set! child-rect (IRect/makeXYWH (+ (:x rect) left') (+ (:y rect) top') (max 0 width') (max 0 height')))
      (core/draw-child child ctx child-rect canvas)))
  
  (-event [_ ctx event]
    (core/event-child child ctx event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn padding
  ([p child] (->Padding p p p p child nil))
  ([w h child] (->Padding w h w h child nil))
  ([l t r b child] (->Padding l t r b child nil)))


;; clip

(core/deftype+ Clip [child ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))
  
  (-draw [_ ctx ^IRect rect ^Canvas canvas]
    (canvas/with-canvas canvas
      (set! child-rect rect)
      (canvas/clip-rect canvas rect)
      (core/draw child ctx child-rect canvas)))
  
  (-event [_ ctx event]
    (core/event-child child ctx event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn clip [child]
  (->Clip child nil))


;; image

(core/deftype+ AnImage [^Image img]
  protocols/IComponent
  (-measure [_ ctx cs]
    (IPoint. (.getWidth img) (.getHeight img)))
  
  (-draw [_ ctx ^IRect rect ^Canvas canvas]
    (.drawImageRect canvas img (.toRect rect)))
  
  (-event [_ ctx event])
  
  (-iterate [this ctx cb]
    (cb this))
  
  AutoCloseable
  (close [_]
    #_(.close img))) ;; TODO

(defn image [src]
  (let [image (Image/makeFromEncoded (core/slurp-bytes src))]
    (->AnImage image)))


;; svg

(core/deftype+ SVG [^SVGDOM dom ^SVGPreserveAspectRatio scaling ^:mut ^Image image]
  protocols/IComponent
  (-measure [_ ctx cs]
    cs)
  
  (-draw [_ ctx ^IRect rect ^Canvas canvas]
    (let [{:keys [x y width height]} rect]
      (when (or (nil? image)
              (not= (.getWidth image) (:width rect))
              (not= (.getHeight image) (:height rect)))
        (when image
          (.close image))
      
        (set! image
          (with-open [surface (Surface/makeRasterN32Premul width height)]
            (let [image-canvas (.getCanvas surface)
                  root (.getRoot dom)]
              (.setWidth root (SVGLength. width))
              (.setHeight root (SVGLength. height))
              (.setPreserveAspectRatio root scaling)
              (.render dom image-canvas)
              (.makeImageSnapshot surface)))))
      
      (.drawImageRect canvas image (.toRect rect))))
  
  (-event [_ ctx event])
  
  (-iterate [this ctx cb]
    (cb this))
  
  AutoCloseable
  (close [_]
    #_(.close dom))) ;; TODO

(defn svg-opts->scaling [opts]
  (let [{:keys [preserve-aspect-ratio xpos ypos scale]
         :or {preserve-aspect-ratio true
              xpos :mid
              ypos :mid
              scale :meet}} opts]
    (if preserve-aspect-ratio
      (case [xpos ypos scale]
        [:min :min :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMIN  SVGPreserveAspectRatioScale/MEET)
        [:min :mid :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMID  SVGPreserveAspectRatioScale/MEET)
        [:min :max :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMAX  SVGPreserveAspectRatioScale/MEET)
        [:mid :min :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMIN  SVGPreserveAspectRatioScale/MEET)
        [:mid :mid :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMID  SVGPreserveAspectRatioScale/MEET)
        [:mid :max :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMAX  SVGPreserveAspectRatioScale/MEET)
        [:max :min :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMIN  SVGPreserveAspectRatioScale/MEET)
        [:max :mid :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMID  SVGPreserveAspectRatioScale/MEET)
        [:max :max :meet]  (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMAX  SVGPreserveAspectRatioScale/MEET)
        [:min :min :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMIN  SVGPreserveAspectRatioScale/SLICE)
        [:min :mid :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMID  SVGPreserveAspectRatioScale/SLICE)
        [:min :max :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMIN_YMAX  SVGPreserveAspectRatioScale/SLICE)
        [:mid :min :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMIN  SVGPreserveAspectRatioScale/SLICE)
        [:mid :mid :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMID  SVGPreserveAspectRatioScale/SLICE)
        [:mid :max :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMID_YMAX  SVGPreserveAspectRatioScale/SLICE)
        [:max :min :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMIN  SVGPreserveAspectRatioScale/SLICE)
        [:max :mid :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMID  SVGPreserveAspectRatioScale/SLICE)
        [:max :max :slice] (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/XMAX_YMAX  SVGPreserveAspectRatioScale/SLICE))
      (SVGPreserveAspectRatio. SVGPreserveAspectRatioAlign/NONE SVGPreserveAspectRatioScale/MEET))))

(defn svg
  ([src] (svg src nil))
  ([src opts]
   (let [dom (with-open [data (Data/makeFromBytes (core/slurp-bytes src))]
               (SVGDOM. data))
         scaling (svg-opts->scaling opts)]
     (->SVG dom scaling nil))))


;; clip-rrect

(core/deftype+ ClipRRect [radii child ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (let [{:keys [scale]} ctx
          radii' (into-array Float/TYPE (map #(* scale %) radii))
          rrect  (RRect/makeComplexXYWH (:x rect) (:y rect) (:width rect) (:height rect) radii')
          layer  (.save canvas)]
      (try
        (set! child-rect rect)
        (.clipRRect canvas rrect true)
        (core/draw child ctx child-rect canvas)
        (finally
          (.restoreToCount canvas layer)))))
  
  (-event [_ ctx event]
    (core/event-child child ctx event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn clip-rrect [r child]
  (->ClipRRect [r] child nil))


;; hoverable

(core/deftype+ Hoverable [on-hover on-out child ^:mut child-rect ^:mut hovered?]
  protocols/IContext
  (-context [_ ctx]
    (cond-> ctx
      hovered? (assoc :hui/hovered? true)))

  protocols/IComponent
  (-measure [this ctx cs]
    (core/measure child (protocols/-context this ctx) cs))
  
  (-draw [this ctx rect canvas]
    (set! child-rect rect)
    (core/draw-child child (protocols/-context this ctx) child-rect canvas))
  
  (-event [this ctx event]
    (core/eager-or
      (core/event-child child (protocols/-context this ctx) event)
      (when (= :mouse-move (:event event))
        (let [hovered?' (.contains ^IRect child-rect (IPoint. (:x event) (:y event)))]
          (when (not= hovered? hovered?')
            (set! hovered? hovered?')
            (if hovered?'
              (when on-hover (on-hover))
              (when on-out (on-out)))
            true)))))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child (protocols/-context this ctx) cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn hoverable
  "Enable the child element to respond to mouse hover events.

  If no callback, the event can still effect rendering through use of dynamic
  context as follows:

    (ui/dynamic ctx [hovered? (:hui/hovered? ctx)]
       # here we know the hover state of the object
       ...)

  You can also respond to hover events by providing optional :on-hover and/or
  :on-out callbacks in an options map as the first argument. The callback
  functions take no arguments and ignore their return value."
  ([child]
   (->Hoverable nil nil child nil false))
  ([{:keys [on-hover on-out]} child]
   (->Hoverable on-hover on-out child nil false)))


;; vscroll

(core/deftype+ VScroll [child ^:mut offset ^:mut ^IRect self-rect ^:mut child-size]
  protocols/IComponent
  (-measure [_ ctx cs]
    (let [child-cs (assoc cs :height Integer/MAX_VALUE)]
      (set! child-size (protocols/-measure child ctx child-cs))
      (IPoint. (:width child-size) (:height cs))))
  
  (-draw [_ ctx ^IRect rect ^Canvas canvas]
    (when (nil? child-size)
      (set! child-size (protocols/-measure child ctx (IPoint. (:width rect) Integer/MAX_VALUE))))
    (set! self-rect rect)
    (set! offset (core/clamp offset (- (:height rect) (:height child-size)) 0))
    (let [layer      (.save canvas)
          child-rect (-> rect
                       (update :y + offset)
                       (assoc :height Integer/MAX_VALUE))]
      (try
        (.clipRect canvas (.toRect rect))
        (core/draw child ctx child-rect canvas)
        (finally
          (.restoreToCount canvas layer)))))
  
  (-event [_ ctx event]
    (cond
      (= :mouse-scroll (:event event))
      (when (.contains self-rect (IPoint. (:x event) (:y event)))
        (or
          (core/event-child child ctx event)
          (let [offset' (-> offset
                          (+ (:delta-y event))
                          (core/clamp (- (:height self-rect) (:height child-size)) 0))]
            (when (not= offset offset')
              (set! offset offset')
              true))))
      
      (= :mouse-button (:event event))
      (when (.contains self-rect (IPoint. (:x event) (:y event)))
        (core/event-child child ctx event))
      
      :else
      (core/event-child child ctx event)))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn vscroll [child]
  (->VScroll child 0 nil nil))


;; vscrollbar

(core/deftype+ VScrollbar [child ^Paint fill-track ^Paint fill-thumb ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (core/draw-child child ctx child-rect canvas)
    (when (> (:height (:child-size child)) (:height child-rect))
      (let [{:keys [scale]} ctx
            content-y (- (:offset child))
            content-h (:height (:child-size child))
            scroll-y  (:y child-rect)
            scroll-h  (:height child-rect)
            scroll-r  (:right child-rect)
            
            padding (* 4 scale)
            track-w (* 4 scale)
            track-x (+ (:x rect) (:width child-rect) (- track-w) (- padding))
            track-y (+ scroll-y padding)
            track-h (- scroll-h (* 2 padding))
            track   (RRect/makeXYWH track-x track-y track-w track-h (* 2 scale))
            
            thumb-w       (* 4 scale)
            min-thumb-h   (* 16 scale)
            thumb-y-ratio (/ content-y content-h)
            thumb-y       (-> (* track-h thumb-y-ratio) (core/clamp 0 (- track-h min-thumb-h)) (+ track-y))
            thumb-b-ratio (/ (+ content-y scroll-h) content-h)
            thumb-b       (-> (* track-h thumb-b-ratio) (core/clamp min-thumb-h track-h) (+ track-y))
            thumb         (RRect/makeLTRB track-x thumb-y (+ track-x thumb-w) thumb-b (* 2 scale))]
        (.drawRRect canvas track fill-track)
        (.drawRRect canvas thumb fill-thumb))))

  (-event [_ ctx event]
    (core/event-child child ctx event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    ;; TODO causes crash
    ; (.close fill-track)
    ; (.close fill-thumb)
    (core/child-close child)))

(defn vscrollbar [child]
  (when-not (instance? VScroll child)
    (throw (ex-info (str "Expected VScroll, got: " (type child)) {:child child})))
  (->VScrollbar child (paint/fill 0x10000000) (paint/fill 0x60000000) nil))


;; canvas

(core/deftype+ ACanvas [on-paint on-event ^:mut my-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (IPoint. (:width cs) (:height cs)))
  
  (-draw [_ ctx ^IRect rect ^Canvas canvas]
    (set! my-rect rect)
    (when on-paint
      (let [layer (.save canvas)]
        (try
          (.clipRect canvas (.toRect rect))
          (.translate canvas (:x rect) (:y rect))
          (on-paint ctx canvas (IPoint. (:width rect) (:height rect)))
          (finally
            (.restoreToCount canvas layer))))))
  
  (-event [_ ctx event]
    (when on-event
      (let [event' (if (every? event [:x :y])
                     (-> event
                       (update :x - (:x my-rect))
                       (update :y - (:y my-rect)))
                     event)]
        (on-event ctx event'))))
  
  (-iterate [this ctx cb]
    (cb this)))

(defn canvas [{:keys [on-paint on-event]}]
  (->ACanvas on-paint on-event nil))

;; text-listener

(core/deftype+ TextListener [on-input child ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))
  
  (-draw [_ ctx rect ^Canvas canvas]
    (set! child-rect rect)
    (core/draw-child child ctx rect canvas))
  
  (-event [_ ctx event]
    (core/eager-or
      (when (= :text-input (:event event))
        (on-input (:text event)))
      (core/event-child child ctx event)))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))
  
  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn text-listener [{:keys [on-input]} child]
  (->TextListener on-input child nil))


;; button

(defn button
  ([on-click child]
   (button on-click nil child))
  ([on-click opts child]
   (dynamic ctx [{:keys [leading]} ctx]
     (let [{:keys [bg bg-active bg-hovered border-radius padding-left padding-top padding-right padding-bottom]
            p :padding} opts
           bg-active      (or bg-active bg-hovered bg 0xFFA2C7EE)
           bg-hovered     (or bg-hovered bg 0xFFCFE8FC)
           bg             (or bg 0xFFB2D7FE)
           border-radius  (or border-radius 4)
           padding-left   (or padding-left   p 20)
           padding-top    (or padding-top    p leading)
           padding-right  (or padding-right  p 20)
           padding-bottom (or padding-bottom p leading)]
       (clickable
         {:on-click (when on-click
                      (fn [_] (on-click)))}
         (clip-rrect border-radius
           (dynamic ctx [{:keys [hui/active? hui/hovered?]} ctx]
             (rect
               (cond
                 active?  (paint/fill bg-active)
                 hovered? (paint/fill bg-hovered)
                 :else    (paint/fill bg))
               (padding padding-left padding-top padding-right padding-bottom
                 (halign 0.5
                   (with-context
                     {:hui/active? false
                      :hui/hovered? false}
                     child)))))))))))


;; checkbox

(def ^:private checkbox-states
  {[true  false]          (core/lazy-resource "ui/checkbox/on.svg")
   [true  true]           (core/lazy-resource "ui/checkbox/on_active.svg")
   [false false]          (core/lazy-resource "ui/checkbox/off.svg")
   [false true]           (core/lazy-resource "ui/checkbox/off_active.svg")
   [:indeterminate false] (core/lazy-resource "ui/checkbox/indeterminate.svg")
   [:indeterminate true]  (core/lazy-resource "ui/checkbox/indeterminate_active.svg")})

(defn- checkbox-size [^Font font]
  (let [cap-height (.getCapHeight (.getMetrics font))
        extra      (-> cap-height (/ 8) math/ceil (* 4))] ;; half cap-height but increased so that it’s divisible by 4
    (+ cap-height extra)))

(defn checkbox [*state label]
  (clickable
    {:on-click (fn [_] (swap! *state not))}
    (dynamic ctx [size (/ (checkbox-size (:font-ui ctx))
                         (:scale ctx))]
      (row
        (valign 0.5
          (dynamic ctx [state  @*state
                        active (:hui/active? ctx)]
            (width size
              (height size
                (svg @(checkbox-states [state (boolean active)]))))))
        (gap (/ size 3) 0)
        (valign 0.5
          (with-context {:hui/checked? true}
            label))))))


;; tooltip
(core/deftype+ RelativeRect [relative child opts ^:mut rel-rect ^:mut child-rect]
  protocols/IComponent
  (-measure [_ ctx cs]
    (core/measure child ctx cs))

  (-draw [_ ctx ^IRect rect ^Canvas canvas]
    (let [{:keys [left up anchor shackle]
           :or {left 0 up 0
                anchor :top-left shackle :top-right}} opts
          child-size (core/measure child ctx (IPoint. (:width rect) (:height rect)))
          child-rect' (IRect/makeXYWH (:x rect) (:y rect) (:width child-size) (:height child-size))
          rel-cs (core/measure relative ctx (IPoint. 0 0))
          rel-cs-width (:width rel-cs) rel-cs-height (:height rel-cs)
          rel-rect' (condp = [anchor shackle]
                      [:top-left :top-left]         (IRect/makeXYWH (- (:x child-rect') left) (- (:y child-rect') up) rel-cs-width rel-cs-height)
                      [:top-right :top-left]        (IRect/makeXYWH (- (:x child-rect') rel-cs-width left) (- (:y child-rect') up) rel-cs-width rel-cs-height)
                      [:bottom-right :top-left]     (IRect/makeXYWH (- (:x child-rect') rel-cs-width left) (- (:y child-rect') rel-cs-height up) rel-cs-width rel-cs-height)
                      [:bottom-left :top-left]      (IRect/makeXYWH (- (:x child-rect') left) (- (:y child-rect') rel-cs-height up) rel-cs-width rel-cs-height)
                      [:top-left :top-right]        (IRect/makeXYWH (+ (:x child-rect') (- (:width child-rect') left)) (- (:y child-rect') up) rel-cs-width rel-cs-height)
                      [:top-right :top-right]       (IRect/makeXYWH (+ (:x child-rect') (- (:width child-rect') rel-cs-width left)) (- (:y child-rect') up) rel-cs-width rel-cs-height)
                      [:bottom-left :top-right]     (IRect/makeXYWH (+ (:x child-rect') (- (:width child-rect') left)) (- (:y child-rect') rel-cs-height up) rel-cs-width rel-cs-height)
                      [:bottom-right :top-right]    (IRect/makeXYWH (+ (:x child-rect') (- (:width child-rect') rel-cs-width left)) (- (:y child-rect') rel-cs-height up) rel-cs-width rel-cs-height)
                      [:top-left :bottom-right]     (IRect/makeXYWH (+ (:x child-rect') (- (:width child-rect') left)) (+ (:y child-rect') (- (:height child-rect') up)) rel-cs-width rel-cs-height)
                      [:top-right :bottom-right]    (IRect/makeXYWH (+ (:x child-rect') (- (:width child-rect') rel-cs-width left)) (+ (:y child-rect') (- (:height child-rect') up)) rel-cs-width rel-cs-height)
                      [:bottom-right :bottom-right] (IRect/makeXYWH (+ (:x child-rect') (- (:width child-rect') rel-cs-width left)) (+ (:y child-rect') (- (:height child-rect') rel-cs-height up)) rel-cs-width rel-cs-height)
                      [:bottom-left :bottom-right]  (IRect/makeXYWH (+ (:x child-rect') (- (:width child-rect') left)) (+ (:y child-rect') (- (:height child-rect') rel-cs-height up)) rel-cs-width rel-cs-height)
                      [:top-left :bottom-left]      (IRect/makeXYWH (- (:x child-rect') left) (+ (:y child-rect') (- (:height child-rect') up)) rel-cs-width rel-cs-height)
                      [:top-right :bottom-left]     (IRect/makeXYWH (- (:x child-rect') rel-cs-width left) (+ (:y child-rect') (- (:height child-rect') up)) rel-cs-width rel-cs-height)
                      [:bottom-left :bottom-left]   (IRect/makeXYWH (- (:x child-rect') left) (+ (:y child-rect') (- (:height child-rect') rel-cs-height up)) rel-cs-width rel-cs-height)
                      [:bottom-right :bottom-left]  (IRect/makeXYWH (- (:x child-rect') rel-cs-width left) (+ (:y child-rect') (- (:height child-rect') rel-cs-height up)) rel-cs-width rel-cs-height))]
      (set! child-rect child-rect')
      (set! rel-rect rel-rect')
      (core/draw-child child ctx child-rect canvas)
      (core/draw-child relative ctx rel-rect canvas)))

  (-event [_ ctx event]
    (core/event-child child ctx event))
  
  (-iterate [this ctx cb]
    (or
      (cb this)
      (protocols/-iterate child ctx cb)))

  AutoCloseable
  (close [_]
    (core/child-close child)))

(defn relative-rect
  ([relative child] (relative-rect {} relative child))
  ([opts relative child]
   (->RelativeRect relative child opts nil nil)))

(defn tooltip
  ([tip child] (tooltip {} tip child))
  ([opts tip child]
   (valign 0
     (halign 0
       (hoverable
         (dynamic ctx [{:keys [hui/active? hui/hovered?]} ctx]
           (let [tip' (cond
                        active?  tip
                        hovered? tip
                        :else    (gap 0 0))]
             (relative-rect opts tip' child))))))))

; (require 'user :reload)

(comment
  (do
    (println)
    (set! *warn-on-reflection* true)
    (require 'io.github.humbleui.ui :reload)))