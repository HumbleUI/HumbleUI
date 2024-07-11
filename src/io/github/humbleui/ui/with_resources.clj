(in-ns 'io.github.humbleui.ui)

(defn closeable [x]
  {:value x
   :after-unmount #(core/close x)})

(defmacro with-resources [bindings & body]
  `(ui/with
     ~(vec
        (mapcat
          (fn [[s v]]
            [s (list 'io.github.humbleui.ui/closeable v)])
          (partition 2 bindings)))
     ~@body))
