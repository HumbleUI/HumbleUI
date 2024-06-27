(in-ns 'io.github.humbleui.ui)

(defn error-ctor [^Throwable e]
  [valign {:position 0}
   [halign {:position 0}
    [rect {:paint (paint/fill 0xFFCC0000)}
     [padding {:padding 10}
      [label {:paint (paint/fill 0xFFFFB0B0)}
       (str (class e) ": " (.getMessage e))]]]]])