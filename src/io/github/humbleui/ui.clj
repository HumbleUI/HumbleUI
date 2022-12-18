(ns io.github.humbleui.ui
  (:require
    [io.github.humbleui.app :as app]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.protocols :as protocols]
    [io.github.humbleui.ui.align :as align]
    [io.github.humbleui.ui.animation :as animation]
    [io.github.humbleui.ui.backdrop :as backdrop]
    [io.github.humbleui.ui.button :as button]
    [io.github.humbleui.ui.canvas :as canvas]
    [io.github.humbleui.ui.checkbox :as checkbox]
    [io.github.humbleui.ui.clickable :as clickable]
    [io.github.humbleui.ui.clip :as clip]
    [io.github.humbleui.ui.containers :as containers]
    [io.github.humbleui.ui.draggable :as draggable]
    [io.github.humbleui.ui.dynamic :as dynamic]
    [io.github.humbleui.ui.focusable :as focusable]
    [io.github.humbleui.ui.gap :as gap]
    [io.github.humbleui.ui.hoverable :as hoverable]
    [io.github.humbleui.ui.image :as image]
    [io.github.humbleui.ui.image-snapshot :as image-snapshot]
    [io.github.humbleui.ui.label :as label]
    [io.github.humbleui.ui.listeners :as listeners]
    [io.github.humbleui.ui.padding :as padding]
    [io.github.humbleui.ui.rect :as rect]
    [io.github.humbleui.ui.scroll :as scroll]
    [io.github.humbleui.ui.shadow :as shadow]
    [io.github.humbleui.ui.sizing :as sizing]
    [io.github.humbleui.ui.slider :as slider]
    [io.github.humbleui.ui.stack :as stack]
    [io.github.humbleui.ui.svg :as svg]
    [io.github.humbleui.ui.text-field :as text-field]
    [io.github.humbleui.ui.theme :as theme]
    [io.github.humbleui.ui.toggle :as toggle]
    [io.github.humbleui.ui.tooltip :as tooltip]
    [io.github.humbleui.ui.window :as window]
    [io.github.humbleui.ui.with-bounds :as with-bounds]
    [io.github.humbleui.ui.with-context :as with-context]
    [io.github.humbleui.ui.with-cursor :as with-cursor]))

(core/import-vars
  align/halign
  align/valign
  align/center
  animation/animation
  backdrop/backdrop
  button/button
  canvas/canvas
  checkbox/checkbox
  clickable/clickable
  clip/clip
  clip/clip-rrect
  containers/column
  containers/row
  draggable/draggable
  focusable/focusable
  focusable/focus-controller
  gap/gap
  hoverable/hoverable
  image/image
  image-snapshot/image-snapshot
  label/label
  listeners/event-listener
  listeners/key-listener
  listeners/mouse-listener
  listeners/on-key-focused
  listeners/text-listener
  padding/padding
  rect/rect
  rect/rounded-rect
  scroll/vscroll
  scroll/vscrollbar
  shadow/shadow
  shadow/shadow-inset
  sizing/width
  sizing/height
  sizing/max-width
  slider/slider
  stack/stack
  svg/svg
  theme/default-theme
  text-field/text-input
  text-field/text-field
  toggle/toggle
  tooltip/tooltip
  window/window
  with-bounds/with-bounds
  with-context/with-context
  with-cursor/with-cursor)

(defmacro dynamic [ctx-sym bindings & body]
  (dynamic/dynamic-impl ctx-sym bindings body))

(defmacro start-app! [& body]
  `(core/thread
     (app/start
       (fn []
         ~@body))))
