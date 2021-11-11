(ns io.github.humbleui.window
  (:require
   [io.github.humbleui.macro :as macro])
  (:import
   [io.github.humbleui.jwm App Event EventWindowCloseRequest EventWindowScreenChange EventWindowResize EventFrame LayerGL ZOrder]
   [io.github.humbleui.skija BackendRenderTarget ColorSpace DirectContext FramebufferFormat PixelGeometry Surface SurfaceColorFormat SurfaceOrigin SurfaceProps]
   [java.util.function Consumer]))

(defrecord Window [jwm-window jwm-layer listener])

(defn jwm-window ^io.github.humbleui.jwm.Window [window]
  (:jwm-window window))

(defn make
  ":on-close         (fn [])
   :on-screen-change (fn [])
   :on-resize        (fn [{:keys [window-width window-height content-width content-height]}])
   :on-frame         (fn [])
   :on-paint         (fn [canvas])
   :on-event         (fn [event])"
  [{:keys [on-close on-screen-change on-resize on-frame on-paint on-event]}]
  (let [jwm-window (App/makeWindow)
        jwm-layer  (LayerGL.)
        _          (.attach jwm-layer jwm-window)
        *context   (volatile! nil)
        *target    (volatile! nil)
        *surface   (volatile! nil)
        paint      (when on-paint
                     (fn []
                       (when (not (.-_closed jwm-window))
                         (.makeCurrent jwm-layer)
                         (vswap! *context #(or % (DirectContext/makeGL)))
                         (vswap! *target  #(or % (BackendRenderTarget/makeGL (.getWidth jwm-layer) (.getHeight jwm-layer) 0 8 0 FramebufferFormat/GR_GL_RGBA8)))
                         (vswap! *surface #(or % (Surface/makeFromBackendRenderTarget @*context @*target SurfaceOrigin/BOTTOM_LEFT SurfaceColorFormat/RGBA_8888 (ColorSpace/getSRGB) (SurfaceProps. PixelGeometry/RGB_H))))
                         (on-paint (.getCanvas @*surface))
                         (.flushAndSubmit @*surface)
                         (.swapBuffers jwm-layer))))
        listener   (fn listener [e]
                     (when on-event
                       (on-event e))
                     (cond
                       (instance? EventWindowCloseRequest e)
                       (do
                         (when on-close (on-close))
                         (vswap! *context #(do (macro/doto-some % .abandon .close) nil))
                         (vswap! *surface #(do (macro/doto-some % .close) nil))
                         (vswap! *target #(do (macro/doto-some % .close) nil))
                         (.close jwm-layer)
                         (.close jwm-window))

                       (instance? EventWindowScreenChange e)
                       (do
                         (when on-screen-change (on-screen-change))
                         (.reconfigure jwm-layer)
                         (let [outer  (.getWindowRect jwm-window)
                               inner  (.getContentRect jwm-window)
                               resize (EventWindowResize. (.getWidth outer) (.getHeight outer) (.getWidth inner) (.getHeight inner))]
                           (listener resize)))

                       (instance? EventWindowResize e)
                       (do
                         (when on-resize
                           (on-resize {:window-width   (.getWindowWidth e)
                                       :window-height  (.getWindowHeight e)
                                       :content-width  (.getContentWidth e)
                                       :content-height (.getContentHeight e)}))
                         (.resize jwm-layer (.getContentWidth e) (.getContentHeight e))
                         (vswap! *surface #(do (macro/doto-some % .close) nil))
                         (vswap! *target #(do (macro/doto-some % .close) nil))
                         (vswap! *context #(do (macro/doto-some % .abandon .close) nil))
                         (when paint (paint)))

                       (instance? EventFrame e)
                       (when paint (paint))))]
    (.setEventListener jwm-window (reify Consumer (accept [this e] (listener e))))
    (listener EventWindowScreenChange/INSTANCE)
    (Window. jwm-window jwm-layer listener)))

(defn set-title [window title]
  (.setTitle (jwm-window window) title)
  window)

(defn set-visible [window value]
  (.setVisible (jwm-window window) value)
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
