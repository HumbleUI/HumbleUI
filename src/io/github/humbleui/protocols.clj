(ns io.github.humbleui.protocols)

(defprotocol IComponent
  (-measure [_ ctx cs])
  (-draw    [_ ctx rect canvas])
  (-event   [_ event]))
