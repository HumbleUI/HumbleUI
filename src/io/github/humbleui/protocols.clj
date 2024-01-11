(ns ^{:clojure.tools.namespace.repl/load false}
  io.github.humbleui.protocols)

(def *debug?
  (atom false))

(defprotocol ISettable
  (-set! [_ key value]))

(defn -update! [this key f & args]
  (-set! this key (apply f (get this key) args)))

(defprotocol IComponent
  (-context              [_ ctx])
  (-measure ^IPoint      [_ ctx ^IPoint cs])
  (-measure-impl ^IPoint [_ ctx ^IPoint cs])
  (-draw                 [_ ctx ^IRect rect canvas])
  (-draw-impl            [_ ctx ^IRect rect canvas])
  (-event                [_ ctx event])
  (-event-impl           [_ ctx event])
  (-iterate              [_ ctx cb])
  (-reconcile            [_ ctx new-el])
  (-reconcile-impl       [_ ctx new-el])
  (-should-reconcile?    [_ ctx new-el])
  (-unmount              [_])
  (-unmount-impl         [_]))
