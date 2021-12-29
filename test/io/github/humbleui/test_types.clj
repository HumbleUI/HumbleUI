(ns io.github.humbleui.test-types
  (:require
   [clojure.test :as test :refer [deftest is are testing]])
  (:import
   [clojure.lang ArityException]
   [io.github.humbleui.types IPoint]))

(deftest test-ipoint
  (let [p (IPoint. 1 2)]
    (is (= 2 (count p)))
    (is (= [:x :y] (keys p)))
    (is (= [1 2] (vals p)))
    (is (= [[:x 1] [:y 2]] (seq p)))
    (is (= p (IPoint. 1 2)))
    (is (= (IPoint. 1 2) p))

    (is (= 1 (:x p)))
    (is (= 1 (:x p 3)))
    (is (= 3 (:z p 3)))

    (is (= 1 (p :x)))
    (is (= 1 (p :x 3)))
    (is (= 3 (p :z 3)))
    (is (thrown-with-msg? ArityException #"Wrong number of args \(0\) passed to: io\.github\.humbleui\.types\.IPoint" (p)))
    (is (thrown-with-msg? ArityException #"Wrong number of args \(3\) passed to: io\.github\.humbleui\.types\.IPoint" (p 1 2 3)))

    (is (= 1 (get p :x)))
    (is (= 1 (get p :x 3)))
    (is (= 3 (get p :z 3)))

    (doseq [p' [(assoc p :x 3)
                (assoc p :x 3.0)
                (assoc p :x (float 3))
                (assoc p :x (/ 6 2))
                (conj p [:x 3])
                (conj p {:x 3})
                (into p [[:x 3]])]]
      (testing p'
        (is (= 3 (:x p')))
        (is (= 2 (:y p')))
        (is (= IPoint (type p')))))))