(ns io.github.humbleui.event-test
  (:require
    [clojure.test :as test :refer [deftest is are testing]]
    [io.github.humbleui.event :as event]))

(def ^:private mask->set #'event/mask->set)

(deftest mask->set-test
  (testing "when mask is zero or empty keys"
    (are [mask keys res] (= (mask->set mask keys) res)
      0 [] #{}
      0 [:a :b :c] #{}
      42 [] #{}))

  (testing "basic cases"
    (are [mask keys res] (= (mask->set mask keys) res)
      1 [:a] #{:a}
      2 [:a] #{}
      3 [:a :c] #{:a :c}
      4 [:a :b :c :d] #{:c}
      6 [:a :b :c :d] #{:b :c}
      7 [:a :b :c] #{:a :b :c}
      16 [:a :b :c :d :t :z] #{:t}
      32 [:a :b :c :d :t :z] #{:z}
      255 [:a :b :c :d :t :z] #{:a :b :c :d :t :z}))

  (testing "with very big mask"
    (are [mask keys res] (= (mask->set mask keys) res)
      1000007 [:a] #{:a}
      2000000 [:a] #{}))

  (testing "with very long keys set"
    (let [very-long-keys (mapv #(-> % str keyword) (range 100000))]
      (is (= (mask->set 11 very-long-keys) #{:0 :1 :3})))))

(comment 
  (test/test-ns *ns*))
    