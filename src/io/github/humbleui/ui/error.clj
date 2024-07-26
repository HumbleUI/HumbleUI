(in-ns 'io.github.humbleui.ui)

(defn error-ctor [^Throwable e]
  [rect {:paint (paint/fill 0xFFCC0000)}
   [center
    [padding {:padding 10}
     [label {:paint (paint/fill 0xFFFFB0B0)}
      (str (class e) ": " (.getMessage e))]]]])
