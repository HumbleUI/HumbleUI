(ns examples.shared 
  (:require
    [clj-reload.core :as reload]
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    [clojure.pprint :as pprint]
    [clojure.string :as str]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.util :as util]
    [io.github.humbleui.debug :as debug]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.signal :as signal]
    [io.github.humbleui.typeface :as typeface]
    [io.github.humbleui.ui :as ui]
    [io.github.humbleui.window :as window]))

(defn load-state []
  (let [file (io/file ".state")]
    (when (.exists file)
      (edn/read-string (slurp file)))))

(defn save-state [m]
  (let [file   (io/file ".state")
        state  (or (load-state) {})
        state' (merge state m)]
    (spit file (pr-str state'))))

(defmacro def-durable-signal [name init]
  (let [key (keyword (clojure.core/name name))]
    `(do
       (defonce ~name
         (ui/signal
           (or
             (get (load-state) ~key)
             ~init)))
       (add-watch ~name ::save-state
         (fn [_# _# _# new#]
           (save-state {~key new#}))))))

(defmethod reload/keep-methods 'def-durable-signal [_]
  (reload/keep-methods 'defonce))

(defmethod reload/keep-methods 'shared/def-durable-signal [_]
  (reload/keep-methods 'defonce))

(defmacro restore-durable-signal [name]
  (let [key (keyword (clojure.core/name name))]
    `(do
       (when-some [val# (~key (load-state))]
         (reset! ~name val#))

       (add-watch ~name ::save-state
         (fn [_# _# _# new#]
           (save-state {~key new#}))))))

(restore-durable-signal debug/*paint?)

(restore-durable-signal debug/*pacing?)

(restore-durable-signal debug/*events?)

(restore-durable-signal debug/*outlines?)

(restore-durable-signal debug/*continuous-render?)

(defonce *window
  (promise))

(defn set-floating! [window floating]
  (when window
    (app/doui
      (if floating
        (window/set-z-order window :floating)
        (window/set-z-order window :normal)))))

^:clj-reload/keep
(def-durable-signal *floating?
  false)

(add-watch *floating? ::set-floating!
  (fn [_ _ _ new]
    (set-floating! @*window new)))

(defn slurp-source [file key]
  (let [content      (slurp (io/resource file))
        key-str      (pr-str key)
        idx          (str/index-of content key)
        content-tail (subs content (+ idx (count key-str)))
        reader       (clojure.lang.LineNumberingPushbackReader.
                       (java.io.StringReader.
                         content-tail))
        indent       (re-find #"\s+" content-tail)
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
                :let [left ['ui/padding {:padding 10}
                            ['ui/align {:x :left :y :top}
                             row]]
                      lines (slurp-source *file* (str "\"" name "\""))
                      right ['ui/padding {:padding 10}
                             ['ui/column {:gap 10}
                              ['ui/label {:font-weight :bold} name]
                              (cons 'list
                                (map
                                  #(vector 'ui/label
                                     {:font-family "monospace"
                                      :font-cap-height 8}
                                     %)
                                  lines))]]]]
            (list 'list left right))]]]]])
