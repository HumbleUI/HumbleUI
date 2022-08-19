(ns io.github.humbleui.protocols)

(defprotocol ISettable
  (-set! [_ key value]))

(defprotocol IComponent
  (-measure ^IPoint [_ ctx ^IPoint cs])
  (-draw    [_ ctx ^IRect rect canvas])
  (-event   [_ ctx event]))
