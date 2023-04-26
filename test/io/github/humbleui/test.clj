(ns io.github.humbleui.test
  (:require
    [clojure.test :as test]
    [io.github.humbleui.event-test]
    [io.github.humbleui.signal-test]
    [io.github.humbleui.ui.text-field-test]))

(defn -main [& {:as args}]
  (let [re (or (get args "--only")
             "io\\.github\\.humbleui\\..*-test")]
    (test/run-all-tests (re-pattern re))))
