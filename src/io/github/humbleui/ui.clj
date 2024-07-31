(ns io.github.humbleui.ui
  (:refer-clojure :exclude [iterate])
  (:require
    [clojure.java.io :as io]
    [clojure.math :as math]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.typeface :as typeface]
    [io.github.humbleui.window :as window])
  (:import
    [java.io File]
    [io.github.humbleui.jwm Window]
    [io.github.humbleui.skija Canvas Color Data Font FontMetrics Paint TextLine]
    [io.github.humbleui.skija.shaper Shaper ShapingOptions]
    [io.github.humbleui.types IPoint IRange IRect Point Rect RRect]))

(when-not (.equalsIgnoreCase "false" (System/getProperty "io.github.humbleui.pprint-fn"))
  (defmethod print-method clojure.lang.AFunction [o ^java.io.Writer w]
    (.write w (clojure.lang.Compiler/demunge (.getName (class o))))))

(binding [*warn-on-reflection* true]
  (load "/io/github/humbleui/ui/core")
  (load "/io/github/humbleui/ui/reconcile")
  (load "/io/github/humbleui/ui/nodes")
  (load "/io/github/humbleui/ui/defcomp")
  (load "/io/github/humbleui/ui/dynamic")
  (load "/io/github/humbleui/ui/with")
  (load "/io/github/humbleui/ui/with_context")
  (load "/io/github/humbleui/ui/with_resources")
  (load "/io/github/humbleui/ui/size")
  (load "/io/github/humbleui/ui/font"))

(def *loaded
  (atom #{}))

(defmacro deflazy
  ([name arglists file]
   `(deflazy ~name nil ~arglists ~file))
  ([name docstring arglists file]
   `(def ~(vary-meta name assoc :arglists (list 'quote arglists))
      ~@(if docstring [docstring] [])
      (delay
        (when-not (@*loaded ~file)
          (util/log (str "Loading ui/" ~file))
          (binding [*warn-on-reflection* true]
            (load (str "/io/github/humbleui/ui/" ~file)))
          (swap! *loaded conj ~file))
        @(resolve (quote ~(symbol "io.github.humbleui.ui" (str name "-ctor"))))))))

(def gap size)
(deflazy label     ([& texts]) "label")
(deflazy paragraph ([text] [opts text]) "paragraph")
(deflazy image     ([{:keys [file sampling scale xpos ypos]}]) "image")
(deflazy animation ([{:keys [file sampling scale xpos ypos]}]) "image")
(deflazy svg       ([{:keys [file preserve-aspect-ratio scale xpos ypos]}]) "svg")
(deflazy canvas    ([{:keys [on-paint on-event]}]) "canvas")

(deflazy padding           ([{:keys [padding horizontal vertical left right top bottom]} child]) "padding")
(deflazy rect              ([{:keys [paint radius]} child]) "rect")
(deflazy clip              ([{:keys [radius]} child]) "clip")
(deflazy translate         ([{:keys [dx dy]} child]) "transform")
(deflazy with-bounds       ([child-ctor]) "with_bounds")
(deflazy backdrop          ([{:keys [dx dy]} child]) "backdrop")
(deflazy image-snapshot    ([{:keys [dx dy]} child]) "image_snapshot")
(deflazy reserve-width     ([{:keys [probes]} child]) "reserve_width")

(deflazy align  ([{:keys [x y child-x child-y]} child]) "align")
(deflazy center ([child]) "align")

(deflazy vscrollable ([child] [opts child]) "vscrollable")
(deflazy vscroll     ([child] [opts child]) "vscroll")
(deflazy column      ([& children] [opts & children]) "column")
(deflazy row         ([& children] [opts & children]) "row")
(deflazy stack       ([& children]) "stack")
(deflazy grid        ([{:keys [rows cols]} & children]) "grid")

(deflazy shadow       ([opts] [opts child]) "shadow")
(deflazy shadow-inset ([opts] [opts child]) "shadow")

(deflazy hoverable     ([{:keys [on-hover on-out *hoverable?]} child]) "hoverable")
(deflazy clickable     ([{:keys [on-click on-click-capture]} child]) "clickable")
(deflazy toggleable    ([{:keys [value-on value-off *value on-change]} child]) "toggleable")
(deflazy draggable     ([{:keys [pos on-dragging on-drop]} child]) "draggable")
(deflazy button-look   ([state child]) "button")
(deflazy button        ([{:keys [on-click]} child]) "button")
(deflazy toggle-button ([{:keys [*value]} child]) "button")
(deflazy link          ([{:keys [on-click visited]} child]) "link")
(deflazy slider        ([{:keys [*value min max step]}]) "slider")

(deflazy switch        ([{:keys [value-on value-off *value on-change]}]) "switch")
(deflazy checkbox      ([{:keys [value-on value-off *value on-change]} child]) "checkbox")
(deflazy tooltip       ([{:keys [tip left up anchor shackle]} child]) "tooltip")

(deflazy event-listener ([{:keys [event on-event capture?]} child]) "listeners")
(deflazy on-key-focused ([{:keys [keymap]} child]) "listeners")
(deflazy key-listener   ([{:keys [on-key-up on-key-down]} child]) "listeners")
(deflazy mouse-listener ([{:keys [on-move on-scroll on-button on-over on-out]} child]) "listeners")
(deflazy text-listener  ([{:keys [on-input]} child]) "listeners")

(deflazy focusable        ([{:keys [focused on-focus on-blur]} child]) "focusable")
(deflazy focus-controller ([child]) "focusable")
(deflazy with-cursor      ([{:keys [cursor]}]) "with_cursor")
(deflazy text-input       ([] [{:keys [*value *state on-change]}]) "text_field")
(deflazy text-field       ([] [{:keys [*value *state on-change focused on-focus on-blur keymap]}]) "text_field")

(deflazy error   ([throwable]) "error")
(deflazy profile ([{:keys [value]} child]) "profile")

(binding [*warn-on-reflection* true]
  (load "/io/github/humbleui/ui/theme")
  (load "/io/github/humbleui/ui/window"))

(defmacro start-app! [& body]
  `(util/thread
     (app/start
       (fn []
         ~@body))))
