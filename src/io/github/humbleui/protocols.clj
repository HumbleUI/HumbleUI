(ns ^{:clojure.tools.namespace.repl/load false}
  io.github.humbleui.protocols)

(defprotocol ISettable
  (-set! [_ key value]))

(defprotocol IContext
  (-context [_ ctx]))

(defprotocol IComponent
  (-measure ^IPoint [_ ctx ^IPoint cs])
  (-draw            [_ ctx ^IRect rect canvas])
  (-event           [_ ctx event])
  (-iterate         [_ ctx cb]))
