(ns io.github.humbleui.window
  (:require
   [io.github.humbleui.core :as core]
   [io.github.humbleui.macro :as macro])
  (:import
   ; [io.github.humbleui.core Point Size Rect]
   [io.github.humbleui.jwm App Event EventWindowCloseRequest EventWindowScreenChange EventWindowResize EventFrame LayerGL ZOrder]
   [io.github.humbleui.skija BackendRenderTarget ColorSpace DirectContext FramebufferFormat PixelGeometry Surface SurfaceColorFormat SurfaceOrigin SurfaceProps]
   [java.util.function Consumer]))

(defrecord Window [jwm-window jwm-layer listener])

(defn jwm-window ^io.github.humbleui.jwm.Window [window]
  (:jwm-window window))

(defn make
  ":on-close         (fn [window])
   :on-screen-change (fn [window])
   :on-resize        (fn [window {:keys [window-width window-height content-width content-height]}])
   :on-paint         (fn [window canvas])
   :on-event         (fn [window event])"
  [{:keys [on-close on-screen-change on-resize on-paint on-event]}]
  (let [jwm-window (App/makeWindow)
        jwm-layer  (LayerGL.)
        _          (.attach jwm-layer jwm-window)
        *context   (volatile! nil)
        *target    (volatile! nil)
        *surface   (volatile! nil)
        *window    (volatile! nil)
        paint      (when on-paint
                     (fn []
                       (when-some [window @*window]
                         (.makeCurrent jwm-layer)
                         (vswap! *context #(or % (DirectContext/makeGL)))
                         (vswap! *target  #(or % (BackendRenderTarget/makeGL (.getWidth jwm-layer) (.getHeight jwm-layer) 0 8 0 FramebufferFormat/GR_GL_RGBA8)))
                         (vswap! *surface #(or % (Surface/makeFromBackendRenderTarget @*context @*target SurfaceOrigin/BOTTOM_LEFT SurfaceColorFormat/RGBA_8888 (ColorSpace/getSRGB) (SurfaceProps. PixelGeometry/RGB_H))))
                         (let [canvas (.getCanvas ^Surface @*surface)
                               layer  (.save canvas)]
                           (try
                             (on-paint window canvas)
                             (catch Exception e
                               (.printStackTrace e)
                               (.clear canvas (unchecked-int 0xFFCC3333)))
                             (finally
                               (.restoreToCount canvas layer))))
                         (.flushAndSubmit @*surface)
                         (.swapBuffers jwm-layer))))
        listener   (fn listener [e]
                     (when on-event
                       (on-event @*window e))
                     (cond
                       (instance? EventWindowCloseRequest e)
                       (do
                         (when on-close (on-close @*window))
                         (vswap! *context #(do (macro/doto-some % .abandon .close) nil))
                         (vswap! *surface #(do (macro/doto-some % .close) nil))
                         (vswap! *target #(do (macro/doto-some % .close) nil))
                         (.close jwm-layer)
                         (.close jwm-window)
                         (vreset! *window nil))

                       (instance? EventWindowScreenChange e)
                       (do
                         (when on-screen-change (on-screen-change @*window))
                         (.reconfigure jwm-layer)
                         (let [outer  (.getWindowRect jwm-window)
                               inner  (.getContentRect jwm-window)
                               resize (EventWindowResize. (.getWidth outer) (.getHeight outer) (.getWidth inner) (.getHeight inner))]
                           (listener resize)))

                       (instance? EventWindowResize e)
                       (do
                         (when on-resize
                           (on-resize
                             @*window
                             {:window-width   (.getWindowWidth e)
                              :window-height  (.getWindowHeight e)
                              :content-width  (.getContentWidth e)
                              :content-height (.getContentHeight e)}))
                         (.resize jwm-layer (.getContentWidth e) (.getContentHeight e))
                         (vswap! *surface #(do (macro/doto-some % .close) nil))
                         (vswap! *target #(do (macro/doto-some % .close) nil))
                         (vswap! *context #(do (macro/doto-some % .abandon .close) nil))
                         (when paint (paint)))

                       (instance? EventFrame e)
                       (when paint (paint))))
        _          (.setEventListener jwm-window (reify Consumer (accept [this e] (listener e))))
        window     (Window. jwm-window jwm-layer listener)]
    (vreset! *window window)
    (listener EventWindowScreenChange/INSTANCE)
    window))

(defn scale [window]
  (.getScale (.getScreen (jwm-window window))))

(defn set-title [window title]
  (.setTitle (jwm-window window) title)
  window)

(defn set-visible [window value]
  (.setVisible (jwm-window window) value)
  window)

(defn get-window-rect [window]
  (let [rect (.getWindowRect (jwm-window window))]
    (core/->Rect (.getLeft rect) (.getTop rect) (.getWidth rect) (.getHeight rect))))

(defn set-window-position [window x y]
  (.setWindowPosition (jwm-window window) x y)
  window)

(defn set-window-size [window width height]
  (.setWindowSize (jwm-window window) width height)
  window)

(defn set-content-size [window width height]
  (.setContentSize (jwm-window window) width height)
  window)

(defn set-z-order [window order]
  (.setZOrder (jwm-window window)
    (case order
      :normal       ZOrder/NORMAL
      :floating     ZOrder/FLOATING
      :modal-panel  ZOrder/MODAL_PANEL
      :main-menu    ZOrder/MAIN_MENU
      :status       ZOrder/STATUS
      :pop-up-menu  ZOrder/POP_UP_MENU
      :screen-saver ZOrder/SCREEN_SAVER))
  window)

(defn request-frame [window]
  (.requestFrame (jwm-window window))
  window)

(defn close [window]
  ((:listener window) EventWindowCloseRequest/INSTANCE)
  nil)
