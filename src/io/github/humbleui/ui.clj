(ns io.github.humbleui.ui
  (:require
    [clojure.java.io :as io]
    [clojure.math :as math]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.typeface :as typeface]
    [io.github.humbleui.window :as window]
    ; [io.github.humbleui.ui.focusable :as focusable]
    ; [io.github.humbleui.ui.listeners :as listeners]
    ; [io.github.humbleui.ui.text-field :as text-field]
    ; [io.github.humbleui.ui.with-cursor :as with-cursor]
    )
  (:import
    [io.github.humbleui.jwm Window]
    [io.github.humbleui.skija Canvas Color Data Font FontMetrics Paint TextLine]
    [io.github.humbleui.skija.shaper Shaper ShapingOptions]
    [io.github.humbleui.types IPoint IRange IRect Point Rect RRect]))

(when-not (.equalsIgnoreCase "false" (System/getProperty "io.github.humbleui.pprint-fn"))
  (defmethod print-method clojure.lang.AFunction [o ^java.io.Writer w]
    (.write w (clojure.lang.Compiler/demunge (.getName (class o))))))

(load "/io/github/humbleui/ui/core")
(load "/io/github/humbleui/ui/dynamic")
(load "/io/github/humbleui/ui/with_context")
(load "/io/github/humbleui/ui/sizing")


(defmacro deflazy
  ([name arglists file]
   `(deflazy ~name nil ~arglists ~file))
  ([name docstring arglists file]
   `(def ~(vary-meta name assoc :arglists (list 'quote arglists))
      ~@(if docstring [docstring] [])
      (delay
        (core/log "Loading" ~file)
        (load (str "/io/github/humbleui/ui/" ~file))
        @(resolve (quote ~(symbol "io.github.humbleui.ui" (str name "-ctor"))))))))

(deflazy gap       ([] [{:keys [width height]}]) "gap")
(deflazy label     ([& texts]) "label")
(deflazy paragraph ([text] [opts text]) "paragraph")
(deflazy image     ([src] [{:keys [sampling scale xpos ypos]} src]) "image")
(deflazy animation ([src] [{:keys [sampling scale xpos ypos]} src]) "image")
(deflazy svg       ([src] [{:keys [preserve-aspect-ratio xpos ypos scale]} src]) "svg")
(deflazy canvas    ([{:keys [on-paint on-event]}]) "canvas")

(deflazy padding        ([{:keys [padding horizontal vertical left right top bottom]} child]) "padding")
(deflazy rect           ([{:keys [paint]} child]) "rect")
(deflazy rounded-rect   ([{:keys [radius paint]} child]) "rect")
(deflazy clip           ([child]) "clip")
(deflazy clip-rrect     ([{:keys [radii]} child]) "clip")
(deflazy translate      ([{:keys [dx dy]} child]) "transform")
(deflazy with-bounds    ([child-ctor]) "with_bounds")
(deflazy backdrop       ([{:keys [dx dy]} child]) "backdrop")
(deflazy image-snapshot ([{:keys [dx dy]} child]) "image_snapshot")

(deflazy halign ([{:keys [position child-position]} child]) "align")
(deflazy valign ([{:keys [position child-position]} child]) "align")
(deflazy center ([child]) "align")

(deflazy vscroll    ([child] [opts child]) "scroll")
(deflazy vscrollbar ([child] [opts child]) "scroll")
(deflazy column     ([& children] [opts & children]) "column")
(deflazy row        ([& children] [opts & children]) "row")
(deflazy stack      ([& children]) "stack")
(deflazy grid       ([{:keys [rows cols]} & children]) "grid")

(deflazy shadow       ([opts] [opts child]) "shadow")
(deflazy shadow-inset ([opts] [opts child]) "shadow")

(deflazy hoverable     ([{:keys [on-hover on-out *hoverable?]} child]) "hoverable")
(deflazy clickable     ([{:keys [on-click on-click-capture]} child]) "clickable")
(deflazy toggleable    ([{:keys [value-on value-off *value on-change]} child]) "toggleable")
(deflazy draggable     ([{:keys [pos on-dragging on-drop]} child]) "draggable")
(deflazy button        ([{:keys [on-click]} child]) "button")
(deflazy toggle-button ([{:keys [*value]} child]) "button")
(deflazy slider        ([{:keys [*value min max step]}]) "slider")

(deflazy switch        ([{:keys [value-on value-off *value on-change]}]) "switch")
(deflazy checkbox      ([{:keys [value-on value-off *value on-change]} child]) "checkbox")
(deflazy tooltip       ([{:keys [tip left up anchor shackle]} child]) "tooltip")

(deflazy error         ([throwable]) "error")

(load "/io/github/humbleui/ui/theme")
(load "/io/github/humbleui/ui/window")

(core/import-vars
  ; focusable/focusable
  ; focusable/focus-controller
  ; listeners/event-listener
  ; listeners/key-listener
  ; listeners/mouse-listener
  ; listeners/on-key-focused
  ; listeners/text-listener
  ; text-field/text-input
  ; text-field/text-field
  ; with-cursor/with-cursor
  )

(defmacro start-app! [& body]
  `(core/thread
     (app/start
       (fn []
         ~@body))))
