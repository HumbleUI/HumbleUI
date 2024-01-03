(ns io.github.humbleui.core-test
  (:require
    [clojure.test :as test :refer [deftest is are testing]]
    [io.github.humbleui.core :as core]))

(deftest test-loop+
  (testing "basic"
    (is (= 2
          (core/loop+ [x 1]
            (if (not= 1 x)
              x
              (recur [x 2]))))))
  
  (testing "incomplete recur"
    (is (= [2 3]
          (core/loop+ [x 1
                       y 3]
            (if (not= 1 x)
              [x y]
              (recur [x 2]))))))
  
  (testing "empty recur"
    (is (= [1 2]
          (let [*done? (volatile! false)]
            (core/loop+ [x 1 y 2]
              (if @*done?
                [x y]
                (do
                  (vreset! *done? true)
                  (recur))))))))
  
  (testing "incomplete recur + shadowing"
    (is (= [2 3]
          (core/loop+ [x 1
                       y 3]
            (if (not= 1 x)
              [x y]
              (let [y 4]
                (recur [x 2])))))))
  
  (testing "dependencies in recur"
    (is (= [2 3]
          (core/loop+ [x 1
                       y 4]
            (if (not= 1 x)
              [x y]
              (recur [x 2
                      y (+ x 1)]))))))
        
  (testing "arbitrary order"
    (is (= [4 3]
          (core/loop+ [x 1
                       y 2]
            (if (not= 1 x)
              [x y]
              (recur [y 3
                      x 4]))))))
  
  (testing "example"
    (= [10 10 10]
      (core/loop+ [x 0
                   y 0
                   z 0]
        (cond
          (< x 10) (recur [x (inc x)])
          (< y 10) (recur [x 0
                           y (inc y)])
          (< z 10) (recur [x 0
                           y 0
                           z (inc z)])
          :else    [x y z])))))

(comment
  (test/run-test effect)
  (test/test-ns *ns*))
