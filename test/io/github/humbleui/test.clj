(ns io.github.humbleui.test
  (:require
   [clojure.test :as test]
   [io.github.humbleui.test-types]))

(defn -main [& args]
  (test/run-all-tests #"io\.github\.humbleui\..*"))