(ns io.github.humbleui.signal-test
  (:require
    [clojure.test :as test :refer [deftest is are testing]]
    [io.github.humbleui.signal :refer [signal mutate!]]))

(deftest mutate-direct
  (let [*a (signal 10)]
    (is (= 10 @*a))
    (mutate! *a 20)
    (is (= 20 @*a))))
  
(deftest depth-1
  (let [*a (signal 3)
        *b (signal 7)
        *c (computed (* @*a @*b))]
    (is (= 21 @*c))
    (mutate! *a 5)
    (is (= 35 @*c))
    (mutate! *b 9)
    (is (= 45 @*c)))
    
  (let [*a (signal 3)
        *b (signal 7)
        *c (computed (* @*b @*a))]
    (is (= 21 @*c))
    (mutate! *a 5)
    (is (= 35 @*c))
    (mutate! *b 9)
    (is (= 45 @*c))))
  
(deftest depth-2-linear
  (let [*a (signal 1)
        *b (computed (+ 10 @*a))
        *c (computed (+ 100 @*b))]
    (is (= 111 @*c))
    (mutate! *a 2)
    (is (= :dirty (:state *b)))
    (is (= :check (:state *c)))
    (is (= 112 @*c))
    (is (= :clean (:state *b)))
    (is (= :clean (:state *c)))
    (is (= 12 @*b))
      
    (mutate! *a 3)
    (is (= :dirty (:state *b)))
    (is (= :check (:state *c)))
    (is (= 13 @*b))
    (is (= :clean (:state *b)))
    (is (= :dirty (:state *c)))
    (is (= 113 @*c))))

(deftest depth-5-linear
  (let [*a (signal 1)
        *b (computed (+ 10 @*a))
        *c (computed (+ 100 @*b))
        *d (computed (+ 1000 @*c))
        *e (computed (+ 10000 @*d))]
    (is (= 11111 @*e))
    (mutate! *a 2)
    (is (= :dirty (:state *b)))
    (is (= :check (:state *c)))
    (is (= :check (:state *d)))
    (is (= :check (:state *e)))
    (is (= 11111 (:value *e)))
    (is (= 11112 @*e)))
  
  (let [*a (signal 1)
        *b (computed (+ 10 @*a))
        *c (computed (+ 100 @*b))
        *d (computed (+ 1000 @*c))
        *e (computed (+ 10000 @*d))]
    (is (= 11111 @*e))
    (mutate! *a 2)
    (is (= 111 (:value *c)))
    (is (= 112 @*c))
    (is (= :clean (:state *b)))
    (is (= :clean (:state *c)))
    (is (= :dirty (:state *d)))
    (is (= :check (:state *e)))))

(deftest short-circuit
  (let [*a   (signal 1)
        *b   (computed (+ 10 @*a))
        *c   (computed (= 0 (mod @*b 3)))
        *cnt (atom 0)
        *d   (computed (swap! *cnt inc) (str @*c))]
    (is (= "false" @*d))
    (is (= 1 @*cnt))
    
    (mutate! *a 2)
    (is (= "true" @*d))
    (is (= 2 @*cnt))  
    
    (mutate! *a 5)
    (is (= :dirty (:state *b)))
    (is (= :check (:state *c)))
    (is (= :check (:state *d)))
    (is (= "true" @*d))
    (is (= 2 @*cnt)) ;; *d cleaned without being recalculated
    (is (= :clean (:state *b)))
    (is (= :clean (:state *c)))
    (is (= :clean (:state *d)))))

(deftest depth-2-diamond
  (let [*a (signal 1)
        *b (computed (+ 10 @*a))
        *c (computed (+ 100 @*a))
        *d (computed (+ 1000 @*b @*c))]
    (is (= 1112 @*d))
    (mutate! *a 2)
    (is (= 1114 @*d))
    (is (= 12 @*b))
    (is (= 102 @*c)))
  
  (let [*a (signal 1)
        *b (computed (+ 10 @*a))
        *c (computed (+ 100 @*a))
        *d (computed (+ 1000 @*c @*b))]
    (is (= 1112 @*d))
    (mutate! *a 2)
    (is (= 1114 @*d))
    (is (= 12 @*b))
    (is (= 102 @*c))))

(deftest depth-2-asymmetrical
  (let [*a (signal 1)
        *b (computed (+ 10 @*a))
        *c (computed (+ @*a @*b))]
    (is (= 12 @*c))
    (mutate! *a 2)
    (is (= 14 @*c)))
  
  (let [*a (signal 1)
        *b (computed (+ 10 @*a))
        *c (computed (+ @*b @*a))]
    (is (= 12 @*c))
    (mutate! *a 2)
    (is (= 14 @*c))))

(deftest depth-2-dynamic
  (let [*a (signal 1)
        *b (signal 2)
        *c (signal :a)
        *cnt (atom 0)
        *d (computed
             (swap! *cnt inc)
             (case @*c
               :a (str @*a)
               :b (str @*b)))]
    (is (= "1" @*d))
    (is (= 1 @*cnt))
    
    (mutate! *b 3)
    (is (= "1" @*d))
    (is (= 1 @*cnt))
    
    (mutate! *c :b)
    (is (= "3" @*d))
    (is (= 2 @*cnt))
    
    (mutate! *b 4)
    (is (= "4" @*d))
    (is (= 3 @*cnt))
    
    (mutate! *a 5)
    (is (= "4" @*d))
    (is (= 3 @*cnt))))

(comment
  (test/test-ns *ns*))
