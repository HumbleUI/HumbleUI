(in-ns 'io.github.humbleui.ui)

(when-not (.equalsIgnoreCase "false" (System/getProperty "io.github.humbleui.pprint-fn"))
  (defmethod print-method clojure.lang.AFunction [o ^java.io.Writer w]
    (.write w (clojure.lang.Compiler/demunge (.getName (class o))))))

(defmethod print-method io.github.humbleui.types.Point [o ^java.io.Writer w]
  (.write w (str o)))

(defmethod print-method io.github.humbleui.types.IPoint [o ^java.io.Writer w]
  (.write w (str o)))

(defmethod print-method io.github.humbleui.types.Rect [o ^java.io.Writer w]
  (.write w (str o)))

(defmethod print-method io.github.humbleui.types.IRect [o ^java.io.Writer w]
  (.write w (str o)))

(defmethod print-method io.github.humbleui.types.RRect [o ^java.io.Writer w]
  (.write w (str o)))