(ns io.github.humbleui.window
  (:require
    [clojure.java.io :as io]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.debug :as debug]
    [io.github.humbleui.event :as event])
  (:import
    [io.github.humbleui.jwm App MouseCursor Platform TextInputClient Window ZOrder]
    [io.github.humbleui.jwm.skija LayerD3D12Skija LayerGLSkija LayerMetalSkija]
    [io.github.humbleui.skija ColorSpace Surface]
    [io.github.humbleui.types IRect]
    [java.util.function Consumer]))

(defn close [^Window window]
  (.close window))

(defn closed? [^Window window]
  (.isClosed window))

(defn scale [^Window window]
  (.getScale (.getScreen window)))

(defn screen [^Window window]
  (some-> window .getScreen (@#'app/screen->clj)))

(defn window-rect [^Window window]
  (.getWindowRect window))

(defn content-rect [^Window window]
  (.getContentRect window))

(defonce *windows
  (atom {}))

(defn make
  ":on-close-request (fn [window])
   :on-close         (fn [])
   :on-screen-change (fn [window])
   :on-resize        (fn [window])
   :on-paint         (fn [window canvas])
   :on-event         (fn [window event])"
  ^Window
  [{:keys [on-close-request on-close on-screen-change on-resize on-paint on-event]
    :or {on-close-request close}}]
  (let [window       (App/makeWindow)
        _            (swap! *windows assoc window {})
        layer        (condp = Platform/CURRENT
                       Platform/MACOS   (LayerMetalSkija.)
                       Platform/WINDOWS (LayerD3D12Skija.)
                       Platform/X11     (LayerGLSkija.))
        listener     (reify Consumer
                       (accept [_ jwm-event]
                         (let [e    (event/event->map jwm-event)
                               type (:event e)]
                           (when-not (#{nil :frame :frame-skija} type)
                             (debug/on-event-start))
                           
                           (when on-event
                             (when e
                               (when-not (#{:frame :frame-skija} type)
                                 (util/catch-and-log
                                   (on-event window e)))))
                           
                           (case type
                             :window-close-request
                             (when on-close-request
                               (util/catch-and-log
                                 (on-close-request window)))
                             
                             :window-close
                             (do
                               (swap! *windows dissoc window)
                               (when on-close
                                 (util/catch-and-log
                                   (on-close))))
                             
                             :window-screen-change
                             (when on-screen-change
                               (util/catch-and-log
                                 (on-screen-change window)))
                             
                             :window-resize
                             (when on-resize
                               (util/catch-and-log
                                 (on-resize window)))
                             
                             :frame-skija
                             (when on-paint
                               (let [canvas (.getCanvas ^Surface (:surface e))
                                     layer  (.save canvas)]
                                 (try
                                   (debug/measure
                                     (on-paint window canvas))
                                   (debug/draw-frames canvas window)
                                   (catch Throwable e
                                     (util/log-error e)
                                     (.clear canvas (unchecked-int 0xFFCC3333)))
                                   (finally
                                     (.restoreToCount canvas layer))))
                               (when @debug/*continuous-render?
                                 (.requestFrame ^Window window)))
                             
                             nil))))
        input-client-fn #(when on-event
                           (on-event window {:event :get-text-input-client}))
        input-client (reify TextInputClient
                       (getRectForMarkedRange [_ selection-start selection-end]
                         (or
                           (util/catch-and-log
                             (when-some [{:keys [^TextInputClient client ctx]} (input-client-fn)]
                               (binding [util/*text-input-ctx* ctx]
                                 (.getRectForMarkedRange client selection-start selection-end))))
                           (IRect/makeXYWH 0 0 0 0)))
                       
                       (getSelectedRange [_]
                         (or
                           (util/catch-and-log
                             (when-some [{:keys [^TextInputClient client ctx]} (input-client-fn)]
                               (binding [util/*text-input-ctx* ctx]
                                 (.getSelectedRange client))))
                           (util/irange -1 -1)))
                         
                       (getMarkedRange [_]
                         (or
                           (util/catch-and-log
                             (when-some [{:keys [^TextInputClient client ctx]} (input-client-fn)]
                               (binding [util/*text-input-ctx* ctx]
                                 (.getMarkedRange client))))
                           (util/irange -1 -1)))
                       
                       (getSubstring [_ start end]
                         (util/catch-and-log
                           (when-some [{:keys [^TextInputClient client ctx]} (input-client-fn)]
                             (binding [util/*text-input-ctx* ctx]
                               (.getSubstring client start end))))))]
    (.setLayer window layer)
    (when (= :macos app/platform)
      ;; TODO load real monitor profile
      (set! (.-_colorSpace ^LayerMetalSkija layer) (ColorSpace/getDisplayP3)))
    (.setEventListener window listener)
    ; (.setTextInputEnabled window true)
    (.setTextInputClient window input-client)
    window))

(defn set-title [^Window window ^String title]
  (swap! *windows update window assoc :title title)
  (.setTitle window title)
  window)

(defn set-icon [^Window window ^String path]
  (.setIcon window (io/file path))
  window)

(defn set-visible [^Window window ^Boolean value]
  (.setVisible window value)
  window)

(defn focus [^Window window]
  (.focus window)
  window)

(defn bring-to-front [^Window window]
  (.bringToFront window)
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

(defn z-order [^Window window]
  (condp = (.getZOrder window)
    ZOrder/NORMAL       :normal
    ZOrder/FLOATING     :floating
    ZOrder/MODAL_PANEL  :modal-panel
    ZOrder/MAIN_MENU    :main-menu
    ZOrder/STATUS       :status
    ZOrder/POP_UP_MENU  :pop-up-menu
    ZOrder/SCREEN_SAVER :screen-saver))

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
