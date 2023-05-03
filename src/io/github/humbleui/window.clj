(ns io.github.humbleui.window
  (:require
    [clojure.java.io :as io]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.debug :as debug]
    [io.github.humbleui.event :as event]
    [io.github.humbleui.protocols :as protocols])
  (:import
    [io.github.humbleui.jwm App MouseCursor Platform TextInputClient Window ZOrder]
    [io.github.humbleui.jwm.skija LayerD3D12Skija LayerGLSkija LayerMetalSkija]
    [io.github.humbleui.skija Surface]
    [io.github.humbleui.types IRect]
    [java.util.function Consumer]))

(defn close [^Window window]
  (.close window))

(defn closed? [^Window window]
  (.isClosed window))

(defn scale [^Window window]
  (.getScale (.getScreen window)))

(defn window-rect [^Window window]
  (.getWindowRect window))

(defn content-rect [^Window window]
  (.getContentRect window))

(defn make
  ":on-close-request (fn [window])
   :on-close         (fn [])
   :on-screen-change (fn [window])
   :on-resize        (fn [window])
   :on-paint         (fn [window canvas])
   :on-event         (fn [window event])"
  [{:keys [on-close-request on-close on-screen-change on-resize on-paint on-event]
    :or {on-close-request close}}]
  (let [window       (App/makeWindow)
        layer        (condp = Platform/CURRENT
                       Platform/MACOS   (LayerMetalSkija.)
                       Platform/WINDOWS (LayerD3D12Skija.)
                       Platform/X11     (LayerGLSkija.))
        listener     (reify Consumer
                       (accept [_ jwm-event]
                         (let [e    (event/event->map jwm-event)
                               type (:event e)]
                           (when (not= :frame-skija type)
                             (debug/on-start :event))
                           
                           (when on-event
                             (when e
                               (when-not (#{:frame :frame-skija} type)
                                 (core/catch-and-log
                                   (on-event window e)))))
                           
                           (case type
                             :window-close-request
                             (when on-close-request
                               (core/catch-and-log
                                 (on-close-request window)))
                             
                             :window-close
                             (when on-close
                               (core/catch-and-log
                                 (on-close)))
                             
                             :window-screen-change
                             (when on-screen-change
                               (core/catch-and-log
                                 (on-screen-change window)))
                             
                             :window-resize
                             (when on-resize
                               (core/catch-and-log
                                 (on-resize window)))
                             
                             :frame-skija
                             (when on-paint
                               (let [canvas (.getCanvas ^Surface (:surface e))
                                     layer  (.save canvas)]
                                 (try
                                   (debug/on-start :paint)
                                   (on-paint window canvas)
                                   (debug/on-end :paint)
                                   (when @protocols/*debug?
                                     (canvas/with-canvas canvas
                                       (let [scale (scale window)
                                             rect  (content-rect window)]
                                         (canvas/translate canvas
                                           (- (:width rect) (* scale (+ debug/width 5 debug/width 10)))
                                           (- (:height rect) (* scale (+ debug/height 10))))
                                         (canvas/scale canvas scale)
                                         (debug/draw canvas :paint)
                                         (canvas/translate canvas (+ debug/width 10) 0)
                                         (debug/draw canvas :event))))
                                   (catch Throwable e
                                     (core/log-error e)
                                     (.clear canvas (unchecked-int 0xFFCC3333)))
                                   (finally
                                     (.restoreToCount canvas layer)))))
                             
                             nil)
                           (when (not= :frame-skija type)
                             (debug/on-end :event)))))
        input-client-fn #(when on-event
                           (on-event window {:event :get-text-input-client}))
        input-client (reify TextInputClient
                       (getRectForMarkedRange [_ selection-start selection-end]
                         (or
                           (core/catch-and-log
                             (when-some [{:keys [^TextInputClient client ctx]} (input-client-fn)]
                               (binding [core/*ctx* ctx]
                                 (.getRectForMarkedRange client selection-start selection-end))))
                           (IRect/makeXYWH 0 0 0 0)))
                       
                       (getSelectedRange [_]
                         (or
                           (core/catch-and-log
                             (when-some [{:keys [^TextInputClient client ctx]} (input-client-fn)]
                               (binding [core/*ctx* ctx]
                                 (.getSelectedRange client))))
                           (core/irange -1 -1)))
                         
                       (getMarkedRange [_]
                         (or
                           (core/catch-and-log
                             (when-some [{:keys [^TextInputClient client ctx]} (input-client-fn)]
                               (binding [core/*ctx* ctx]
                                 (.getMarkedRange client))))
                           (core/irange -1 -1)))
                       
                       (getSubstring [_ start end]
                         (core/catch-and-log
                           (when-some [{:keys [^TextInputClient client ctx]} (input-client-fn)]
                             (binding [core/*ctx* ctx]
                               (.getSubstring client start end))))))]
    (.setLayer window layer)
    (.setEventListener window listener)
    ; (.setTextInputEnabled window true)
    (.setTextInputClient window input-client)
    window))

(defn set-title [^Window window ^String title]
  (.setTitle window title)
  window)

(defn set-icon [^Window window ^String path]
  (.setIcon window (io/file path))
  window)

(defn set-visible [^Window window ^Boolean value]
  (.setVisible window value)
  window)

(defn set-window-position [^Window window ^long x ^long y]
  (.setWindowPosition window x y)
  window)

(defn set-window-size [^Window window ^long width ^long height]
  (.setWindowSize window width height)
  window)

(defn set-content-size [^Window window ^long width ^long height]
  (.setContentSize window width height)
  window)

(defn set-full-screen [^Window window value]
  (.setFullScreen window value)
  window)

(defn full-screen? [^Window window]
  (.isFullScreen window))

(defn hide-mouse-cursor-until-moved
  ([^Window window]
   (.hideMouseCursorUntilMoved window))
  ([^Window window value]
   (.hideMouseCursorUntilMoved window value)))

(defn set-z-order [^Window window order]
  (.setZOrder window
    (case order
      :normal       ZOrder/NORMAL
      :floating     ZOrder/FLOATING
      :modal-panel  ZOrder/MODAL_PANEL
      :main-menu    ZOrder/MAIN_MENU
      :status       ZOrder/STATUS
      :pop-up-menu  ZOrder/POP_UP_MENU
      :screen-saver ZOrder/SCREEN_SAVER))
  window)

(defn request-frame [^Window window]
  (.requestFrame window)
  window)

(def cursors
  {:arrow         MouseCursor/ARROW
   :crosshair     MouseCursor/CROSSHAIR
   :help          MouseCursor/HELP
   :pointing-hand MouseCursor/POINTING_HAND
   :ibeam         MouseCursor/IBEAM
   :not-allowed   MouseCursor/NOT_ALLOWED
   :wait          MouseCursor/WAIT
   :win-uparrow   MouseCursor/WIN_UPARROW
   :resize-ns     MouseCursor/RESIZE_NS
   :resize-we     MouseCursor/RESIZE_WE
   :resize-nesw   MouseCursor/RESIZE_NESW
   :resize-nwse   MouseCursor/RESIZE_NWSE})

(defn set-cursor [^Window window cursor]
  (let [c (cursors cursor)]
    (assert (some? c) (str "Unknown cursor " cursor))
    (.setMouseCursor window c)))
