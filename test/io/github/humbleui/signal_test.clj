(ns io.github.humbleui.signal-test
  (:require
    [clojure.test :as test :refer [deftest is are testing]]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.signal :as signal]))

(deftest mutate-direct
  (let [*a (signal/signal 10)]
    (is (= 10 @*a))
    (signal/reset! *a 20)
    (is (= 20 @*a))))
  
(deftest depth-1
  (let [*a (signal/signal 3)
        *b (signal/signal 7)
        *c (signal/computed (* @*a @*b))]
    (is (= 21 @*c))
    (signal/reset! *a 5)
    (is (= 35 @*c))
    (signal/reset! *b 9)
    (is (= 45 @*c)))
    
  (let [*a (signal/signal 3)
        *b (signal/signal 7)
        *c (signal/computed (* @*b @*a))]
    (is (= 21 @*c))
    (signal/reset! *a 5)
    (is (= 35 @*c))
    (signal/reset! *b 9)
    (is (= 45 @*c))))

  
(deftest depth-2-linear
  (let [*a (signal/signal 1)
        *b (signal/computed (+ 10 @*a))
        *c (signal/computed (+ 100 @*b))]
    (is (= 111 @*c))
    (signal/reset! *a 2)
    (is (= :dirty (:state *b)))
    (is (= :check (:state *c)))
    (is (= 112 @*c))
    (is (= :clean (:state *b)))
    (is (= :clean (:state *c)))
    (is (= 12 @*b))
      
    (signal/reset! *a 3)
    (is (= :dirty (:state *b)))
    (is (= :check (:state *c)))
    (is (= 13 @*b))
    (is (= :clean (:state *b)))
    (is (= :dirty (:state *c)))
    (is (= 113 @*c))))

(deftest depth-5-linear
  (let [*a (signal/signal 1)
        *b (signal/computed (+ 10 @*a))
        *c (signal/computed (+ 100 @*b))
        *d (signal/computed (+ 1000 @*c))
        *e (signal/computed (+ 10000 @*d))]
    (is (= 11111 @*e))
    (signal/reset! *a 2)
    (is (= :dirty (:state *b)))
    (is (= :check (:state *c)))
    (is (= :check (:state *d)))
    (is (= :check (:state *e)))
    (is (= 11111 (:value *e)))
    (is (= 11112 @*e)))
  
  (let [*a (signal/signal 1)
        *b (signal/computed (+ 10 @*a))
        *c (signal/computed (+ 100 @*b))
        *d (signal/computed (+ 1000 @*c))
        *e (signal/computed (+ 10000 @*d))]
    (is (= 11111 @*e))
    (signal/reset! *a 2)
    (is (= 111 (:value *c)))
    (is (= 112 @*c))
    (is (= :clean (:state *b)))
    (is (= :clean (:state *c)))
    (is (= :dirty (:state *d)))
    (is (= :check (:state *e)))))

(deftest short-circuit
  (let [*a   (signal/signal 1)
        *b   (signal/computed (+ 10 @*a))
        *c   (signal/computed (= 0 (mod @*b 3)))
        *cnt (atom 0)
        *d   (signal/computed (swap! *cnt inc) (str @*c))]
    (is (= "false" @*d))
    (is (= 1 @*cnt))
    
    (signal/reset! *a 2)
    (is (= "true" @*d))
    (is (= 2 @*cnt))  
    
    (signal/reset! *a 5)
    (is (= :dirty (:state *b)))
    (is (= :check (:state *c)))
    (is (= :check (:state *d)))
    (is (= "true" @*d))
    (is (= 2 @*cnt)) ;; *d cleaned without being recalculated
    (is (= :clean (:state *b)))
    (is (= :clean (:state *c)))
    (is (= :clean (:state *d)))))

(deftest depth-2-diamond
  (let [*a (signal/signal 1)
        *b (signal/computed (+ 10 @*a))
        *c (signal/computed (+ 100 @*a))
        *d (signal/computed (+ 1000 @*b @*c))]
    (is (= 1112 @*d))
    (signal/reset! *a 2)
    (is (= 1114 @*d))
    (is (= 12 @*b))
    (is (= 102 @*c)))
  
  (let [*a (signal/signal 1)
        *b (signal/computed (+ 10 @*a))
        *c (signal/computed (+ 100 @*a))
        *d (signal/computed (+ 1000 @*c @*b))]
    (is (= 1112 @*d))
    (signal/reset! *a 2)
    (is (= 1114 @*d))
    (is (= 12 @*b))
    (is (= 102 @*c))))

(deftest depth-2-asymmetrical
  (let [*a (signal/signal 1)
        *b (signal/computed (+ 10 @*a))
        *c (signal/computed (+ @*a @*b))]
    (is (= 12 @*c))
    (signal/reset! *a 2)
    (is (= 14 @*c)))
  
  (let [*a (signal/signal 1)
        *b (signal/computed (+ 10 @*a))
        *c (signal/computed (+ @*b @*a))]
    (is (= 12 @*c))
    (signal/reset! *a 2)
    (is (= 14 @*c))))

(deftest depth-2-dynamic
  (let [*a (signal/signal 1)
        *b (signal/signal 2)
        *c (signal/signal :a)
        *cnt (atom 0)
        *d (signal/computed
             (swap! *cnt inc)
             (case @*c
               :a (str @*a)
               :b (str @*b)))]
    (is (= "1" @*d))
    (is (= 1 @*cnt))
    
    (signal/reset! *b 3)
    (is (= "1" @*d))
    (is (= 1 @*cnt))
    
    (signal/reset! *c :b)
    (is (= "3" @*d))
    (is (= 2 @*cnt))
    
    (signal/reset! *b 4)
    (is (= "4" @*d))
    (is (= 3 @*cnt))
    
    (signal/reset! *a 5)
    (is (= "4" @*d))
    (is (= 3 @*cnt))))

(deftest effect
  (let [*a   (signal/signal 1)
        *cnt (atom 0)
        *e   (signal/effect [*a]
               (swap! *cnt inc))]
    (is (= 0 @*cnt))
    @*a
    (is (= 0 @*cnt))
    (signal/reset! *a 2)
    (is (= 1 @*cnt)))
  
  (let [*a   (signal/signal 1)
        *b   (signal/computed (+ 10 @*a))
        *c   (signal/computed (mod @*b 3))
        *cnt (atom 0)
        *e   (signal/effect [*c]
               (swap! *cnt inc))]
    (is (= 0 @*cnt))
    
    (signal/reset! *a 2)
    (is (= 1 @*cnt))
    
    (signal/reset! *a 3)
    (is (= 2 @*cnt))
    
    (signal/reset! *a 6)
    (is (= 2 @*cnt))))

(deftest incremental-mapv
  (let [*from (signal/signal 0)
        *to   (signal/signal 5)
        *calc (atom [])
        *a    (signal/mapv
                (fn [i]
                  (swap! *calc conj i)
                  (str i))
                (range @*from @*to))]
    (is (= ["0" "1" "2" "3" "4"] @*a))
    (is (= [0 1 2 3 4] @*calc))
    (is (= [0 1 2 3 4] (:cache *a)))
    
    (signal/reset! *to 7)
    (is (= ["0" "1" "2" "3" "4" "5" "6"] @*a))
    (is (= [0 1 2 3 4 5 6] @*calc))
    
    (signal/reset! *to 4)
    (is (= ["0" "1" "2" "3"] @*a))
    (is (= [0 1 2 3 4 5 6] @*calc))
    
    (signal/reset! *to 4)
    (is (= ["0" "1" "2" "3"] @*a))
    (is (= [0 1 2 3 4 5 6] @*calc))
    
    (signal/reset! *from 2)
    (is (= ["2" "3"] @*a))
    (is (= [0 1 2 3 4 5 6] @*calc))
    
    (signal/reset! *from -2)
    (is (= ["-2" "-1" "0" "1" "2" "3"] @*a))
    (is (= [0 1 2 3 4 5 6 -2 -1 0 1] @*calc))
    
    (signal/reset! *to 7)
    (is (= ["-2" "-1" "0" "1" "2" "3" "4" "5" "6"] @*a))
    (is (= [0 1 2 3 4 5 6  -2 -1 0 1 4 5 6] @*calc))))

(comment
  (test/run-test effect)
  (test/test-ns *ns*))
