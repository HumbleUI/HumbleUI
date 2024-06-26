(ns examples.util
  (:require
    [clj-reload.core :as reload]
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    [io.github.humbleui.app :as app]
    [io.github.humbleui.debug :as debug]
    [io.github.humbleui.signal :as signal]
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
         (signal/signal
           (or
             (get (load-state) ~key)
             ~init)))
       (add-watch ~name ::save-state
         (fn [_# _# _# new#]
           (save-state {~key new#}))))))

(defmethod reload/keep-methods 'def-durable-signal [_]
  (reload/keep-methods 'defonce))

(defmethod reload/keep-methods 'util/def-durable-signal [_]
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

