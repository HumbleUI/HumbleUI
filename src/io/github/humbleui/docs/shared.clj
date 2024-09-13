(ns io.github.humbleui.docs.shared 
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [io.github.humbleui.ui :as ui]))

(defn slurp-source [file key]
  (let [content      (slurp (io/resource file))
        key-str      (pr-str key)
        idx          (str/index-of content key-str)
        content-tail (subs content (+ idx (count key-str)))
        reader       (clojure.lang.LineNumberingPushbackReader.
                       (java.io.StringReader.
                         content-tail))
        indent       (re-find #"[ ]+(?=[\S])" content-tail)
        [_ form-str] (read+string reader)]
    (->> form-str
      str/split-lines
      (map #(if (str/starts-with? % indent)
              (subs % (count indent))
              %)))))

(defmacro table [& rows]
  `[ui/align {:y :center}
    [ui/vscroll
     [ui/align {:x :center}
      [ui/padding {:padding 20}
       [ui/grid {:cols 2}
        ~@(for [[name row] (partition 2 rows)
                :let [left ['ui/size {:width #(* 0.5 (:width %))}
                            ['ui/padding {:padding 10}
                             ['ui/align {:x :center :y :top}
                              row]]]
                      lines (slurp-source *file* name)
                      right ['ui/size {:width #(* 0.5 (:width %))}
                             ['ui/padding {:padding 10}
                             ['ui/column {:gap 10}
                              ['ui/label {:font-weight :bold} name]
                              (cons 'list
                                (map
                                  #(vector 'ui/label
                                     {:font-family "monospace"
                                      :font-cap-height 8}
                                     %)
                                  lines))]]]]]
            (list 'list left right))]]]]])
