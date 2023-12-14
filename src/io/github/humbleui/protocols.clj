(ns ^{:clojure.tools.namespace.repl/load false}
  io.github.humbleui.protocols)

(def *debug?
  (atom false))

(defprotocol ISettable
  (-set! [_ key value]))

(defn -update! [this key f & args]
  (-set! this key (apply f (get this key) args)))

(defprotocol IContext
  (-context [_ ctx]))

(defprotocol IComponent
  (-measure ^IPoint      [_ ctx ^IPoint cs])
  (-measure-impl ^IPoint [_ ctx ^IPoint cs])
  (-draw                 [_ ctx ^IRect rect canvas])
  (-draw-impl            [_ ctx ^IRect rect canvas])
  (-event                [_ ctx event])
  (-event-impl           [_ ctx event])
  (-iterate              [_ ctx cb]))

(defprotocol ILifecycle
  (-on-mount [_])
  (-on-mount-impl [_])
  (-on-unmount [_])
  (-on-unmount-impl [_]))

(defprotocol IVDom
  (-reconcile-impl [_ new-el])
  (-compatible-impl [_ new-el]))