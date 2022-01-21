(ns io.github.humbleui.protocols)

(defprotocol IComponent
  (-measure [_ ctx cs])
  (-draw    [_ ctx cs canvas])
  (-event   [_ event]))
