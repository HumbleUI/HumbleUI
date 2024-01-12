(ns io.github.humbleui.ui
  (:require
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
    ; [io.github.humbleui.ui.animation :as animation]
    ; [io.github.humbleui.ui.backdrop :as backdrop]
    ; [io.github.humbleui.ui.button :as button]
    ; [io.github.humbleui.ui.canvas :as canvas]
    ; [io.github.humbleui.ui.checkbox :as checkbox]
    ; [io.github.humbleui.ui.clip :as clip]
    ; [io.github.humbleui.ui.draggable :as draggable]
    ; [io.github.humbleui.ui.focusable :as focusable]
    ; [io.github.humbleui.ui.grid :as grid]
    ; [io.github.humbleui.ui.image :as image]
    ; [io.github.humbleui.ui.image-snapshot :as image-snapshot]
    ; [io.github.humbleui.ui.listeners :as listeners]
    ; [io.github.humbleui.ui.padding :as padding]
    ; [io.github.humbleui.ui.paragraph :as paragraph]
    ; [io.github.humbleui.ui.shadow :as shadow]
    ; [io.github.humbleui.ui.slider :as slider]
    ; [io.github.humbleui.ui.stack :as stack]
    ; [io.github.humbleui.ui.svg :as svg]
    ; [io.github.humbleui.ui.text-field :as text-field]
    ; [io.github.humbleui.ui.toggle :as toggle]
    ; [io.github.humbleui.ui.tooltip :as tooltip]
    [io.github.humbleui.ui.window :as window]
    ; [io.github.humbleui.ui.with-bounds :as with-bounds]
    ; [io.github.humbleui.ui.with-cursor :as with-cursor]
    )
  (:import
    [io.github.humbleui.jwm Window]
    [io.github.humbleui.skija Canvas Font FontMetrics Paint TextLine]
    [io.github.humbleui.skija.shaper Shaper ShapingOptions]
    [io.github.humbleui.types IPoint IRange IRect Point Rect RRect]))

(when-not (.equalsIgnoreCase "false" (System/getProperty "io.github.humbleui.pprint-fn"))
  (defmethod print-method clojure.lang.AFunction [o ^java.io.Writer w]
    (.write w (clojure.lang.Compiler/demunge (.getName (class o))))))

(load "/io/github/humbleui/ui/core")
(load "/io/github/humbleui/ui/dynamic")
(load "/io/github/humbleui/ui/with_context")
(load "/io/github/humbleui/ui/theme")

(defmacro deflazy
  ([name arglists file]
   `(deflazy ~name nil ~arglists ~file))
  ([name docstring arglists file]
   `(def ~(vary-meta name assoc :arglists arglists)
      ~@(if docstring [docstring] [])
      (delay
        (core/log "Loading" ~file)
        (load ~(str "/io/github/humbleui/ui/" file))
        ~name))))

(deflazy gap    '([] [{:keys [width height]}]) "gap")
(deflazy label  '([& texts]) "label")

(deflazy padding '([{:keys [padding horizontal vertical left right top bottom]} child]) "padding")
(deflazy rect '([{:keys [paint]} child]) "rect")
(deflazy rounded-rect '([{:keys [radius paint]} child]) "rect")

(deflazy halign '([{:keys [position child-position]} child]) "align")
(deflazy valign '([{:keys [position child-position]} child]) "align")
(deflazy center '([child]) "align")
(deflazy width '([{:keys [width]} child]) "sizing")
(deflazy height '([{:keys [height]} child]) "sizing")

(deflazy vscroll    '([child] [opts child]) "scroll")
(deflazy vscrollbar '([child] [opts child]) "scroll")
(deflazy column '([& children] [opts & children]) "containers")
(deflazy row '([& children] [opts & children]) "containers")

(deflazy hoverable '([{:keys [on-hover on-out *hoverable?]} child]) "hoverable")
(deflazy clickable '([{:keys [on-click on-click-capture]} child]) "clickable")

(core/import-vars
  ; animation/animation
  ; backdrop/backdrop
  ; button/button
  ; canvas/canvas
  ; checkbox/checkbox
  ; clip/clip
  ; clip/clip-rrect
  ; draggable/draggable
  ; focusable/focusable
  ; focusable/focus-controller
  ; gap/gap
  ; grid/grid
  ; image/image
  ; image-snapshot/image-snapshot
  ; listeners/event-listener
  ; listeners/key-listener
  ; listeners/mouse-listener
  ; listeners/on-key-focused
  ; listeners/text-listener
  ; paragraph/paragraph
  ; shadow/shadow
  ; shadow/shadow-inset
  ; sizing/max-width
  ; slider/slider
  ; stack/stack
  ; svg/svg
  ; text-field/text-input
  ; text-field/text-field
  ; toggle/toggle
  ; tooltip/tooltip
  window/window
  ; with-bounds/with-bounds
  ; with-cursor/with-cursor
  )

(defmacro start-app! [& body]
  `(core/thread
     (app/start
       (fn []
         ~@body))))
