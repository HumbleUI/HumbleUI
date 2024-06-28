(in-ns 'io.github.humbleui.ui)

(defn with-cursor-ctor [opts child]
  (let [window (:window *ctx*)
        cursor (core/checked-get opts :cursor keyword?)]
    [hoverable
     {:on-hover
      (fn [_]
        (window/set-cursor window cursor))
      :on-out
      (fn [_]
        (window/set-cursor window :arrow))}
     child]))