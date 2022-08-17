(ns io.github.humbleui.protocols)

(defprotocol ISettable
  (-set! [_ key value]))

(defprotocol IComponent
  (-measure [_ ctx cs])
  (-draw    [_ ctx rect canvas])
  (-event   [_ ctx event]))
