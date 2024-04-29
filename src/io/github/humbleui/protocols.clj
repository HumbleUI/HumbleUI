(ns io.github.humbleui.protocols)

(defprotocol ISettable
  (-set! [_ key value]))

(defprotocol IComponent
  (-context              [_ ctx])
  (-measure ^IPoint      [_ ctx ^IPoint cs])
  (-measure-impl ^IPoint [_ ctx ^IPoint cs])
  (-draw                 [_ ctx ^IRect rect canvas])
  (-draw-impl            [_ ctx ^IRect rect canvas])
  (-event                [_ ctx event])
  (-event-impl           [_ ctx event])
  (-iterate              [_ ctx cb])
  (-child-elements       [_ ctx new-el])
  (-reconcile            [_ ctx new-el])
  (-reconcile-impl       [_ ctx new-el])
  (-should-reconcile?    [_ ctx new-el])
  (-unmount              [_])
  (-unmount-impl         [_]))
