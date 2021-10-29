(ns io.github.humbleui.macro)

(defmacro doto-some [x & forms]
  `(let [x# ~x]
     (when (some? x#)
       (doto x# ~@forms))))