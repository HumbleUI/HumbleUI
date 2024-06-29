(ns io.github.humbleui.signal-test
  (:require
    [clojure.test :as test :refer [deftest is are testing]]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.signal :as s]))

(deftest mutate-direct
  (let [*a (s/signal 10)]
    (is (= 10 @*a))
    (s/reset! *a 20)
    (is (= 20 @*a))
    (reset! *a 30)
    (is (= 30 @*a))))
  
(deftest depth-1
  (let [*a (s/signal 3)
        *b (s/signal 7)
        *c (s/signal (* @*a @*b))]
    (is (= 21 @*c))
    (s/reset! *a 5)
    (is (= 35 @*c))
    (s/reset! *b 9)
    (is (= 45 @*c)))
    
  (let [*a (s/signal 3)
        *b (s/signal 7)
        *c (s/signal (* @*b @*a))]
    (is (= 21 @*c))
    (s/reset! *a 5)
    (is (= 35 @*c))
    (s/reset! *b 9)
    (is (= 45 @*c))))

  
(deftest depth-2-linear
  (let [*a (s/signal 1)
        *b (s/signal (+ 10 @*a))
        *c (s/signal (+ 100 @*b))]
    (is (= 111 @*c))
    (s/reset! *a 2)
    (is (= :dirty (:state *b)))
    (is (= :check (:state *c)))
    (is (= 112 @*c))
    (is (= :clean (:state *b)))
    (is (= :clean (:state *c)))
    (is (= 12 @*b))
      
    (s/reset! *a 3)
    (is (= :dirty (:state *b)))
    (is (= :check (:state *c)))
    (is (= 13 @*b))
    (is (= :clean (:state *b)))
    (is (= :dirty (:state *c)))
    (is (= 113 @*c))))

(deftest depth-5-linear
  (let [*a (s/signal 1)
        *b (s/signal (+ 10 @*a))
        *c (s/signal (+ 100 @*b))
        *d (s/signal (+ 1000 @*c))
        *e (s/signal (+ 10000 @*d))]
    (is (= 11111 @*e))
    (s/reset! *a 2)
    (is (= :dirty (:state *b)))
    (is (= :check (:state *c)))
    (is (= :check (:state *d)))
    (is (= :check (:state *e)))
    (is (= 11111 (:value *e)))
    (is (= 11112 @*e)))
  
  (let [*a (s/signal 1)
        *b (s/signal (+ 10 @*a))
        *c (s/signal (+ 100 @*b))
        *d (s/signal (+ 1000 @*c))
        *e (s/signal (+ 10000 @*d))]
    (is (= 11111 @*e))
    (s/reset! *a 2)
    (is (= 111 (:value *c)))
    (is (= 112 @*c))
    (is (= :clean (:state *b)))
    (is (= :clean (:state *c)))
    (is (= :dirty (:state *d)))
    (is (= :check (:state *e)))))

(deftest short-circuit
  (let [*a   (s/signal 1)
        *b   (s/signal (+ 10 @*a))
        *c   (s/signal (= 0 (mod @*b 3)))
        *cnt (atom 0)
        *d   (s/signal (swap! *cnt inc) (str @*c))]
    (is (= "false" @*d))
    (is (= 1 @*cnt))
    
    (s/reset! *a 2)
    (is (= "true" @*d))
    (is (= 2 @*cnt))  
    
    (s/reset! *a 5)
    (is (= :dirty (:state *b)))
    (is (= :check (:state *c)))
    (is (= :check (:state *d)))
    (is (= "true" @*d))
    (is (= 2 @*cnt)) ;; *d cleaned without being recalculated
    (is (= :clean (:state *b)))
    (is (= :clean (:state *c)))
    (is (= :clean (:state *d)))))

(deftest depth-2-diamond
  (let [*a (s/signal 1)
        *b (s/signal (+ 10 @*a))
        *c (s/signal (+ 100 @*a))
        *d (s/signal (+ 1000 @*b @*c))]
    (is (= 1112 @*d))
    (s/reset! *a 2)
    (is (= 1114 @*d))
    (is (= 12 @*b))
    (is (= 102 @*c)))
  
  (let [*a (s/signal 1)
        *b (s/signal (+ 10 @*a))
        *c (s/signal (+ 100 @*a))
        *d (s/signal (+ 1000 @*c @*b))]
    (is (= 1112 @*d))
    (s/reset! *a 2)
    (is (= 1114 @*d))
    (is (= 12 @*b))
    (is (= 102 @*c))))

(deftest depth-2-asymmetrical
  (let [*a (s/signal 1)
        *b (s/signal (+ 10 @*a))
        *c (s/signal (+ @*a @*b))]
    (is (= 12 @*c))
    (s/reset! *a 2)
    (is (= 14 @*c)))
  
  (let [*a (s/signal 1)
        *b (s/signal (+ 10 @*a))
        *c (s/signal (+ @*b @*a))]
    (is (= 12 @*c))
    (s/reset! *a 2)
    (is (= 14 @*c))))

(deftest depth-2-dynamic
  (let [*a (s/signal 1)
        *b (s/signal 2)
        *c (s/signal :a)
        *cnt (atom 0)
        *d (s/signal
             (swap! *cnt inc)
             (case @*c
               :a (str @*a)
               :b (str @*b)))]
    (is (= "1" @*d))
    (is (= 1 @*cnt))
    
    (s/reset! *b 3)
    (is (= "1" @*d))
    (is (= 1 @*cnt))
    
    (s/reset! *c :b)
    (is (= "3" @*d))
    (is (= 2 @*cnt))
    
    (s/reset! *b 4)
    (is (= "4" @*d))
    (is (= 3 @*cnt))
    
    (s/reset! *a 5)
    (is (= "4" @*d))
    (is (= 3 @*cnt))))

(deftest effect
  (let [*a   (s/signal 1)
        *cnt (atom 0)
        *e   (s/effect [*a]
               (swap! *cnt inc))]
    (is (= 0 @*cnt))
    @*a
    (is (= 0 @*cnt))
    (s/reset! *a 2)
    (is (= 1 @*cnt)))
  
  (let [*a   (s/signal 1)
        *b   (s/signal (+ 10 @*a))
        *c   (s/signal (mod @*b 3))
        *cnt (atom 0)
        *e   (s/effect [*c]
               (swap! *cnt inc))]
    (is (= 0 @*cnt))
    
    (s/reset! *a 2)
    (is (= 1 @*cnt))
    
    (s/reset! *a 3)
    (is (= 2 @*cnt))
    
    (s/reset! *a 6)
    (is (= 2 @*cnt))))

(deftest incremental-mapv
  (let [*range (s/signal (range 0 5))
        *calc  (atom [])
        *a     (s/mapv
                 (fn [i]
                   (swap! *calc conj i)
                   (str i))
                 *range)]
    (is (= ["0" "1" "2" "3" "4"] @*a))
    (is (= [0 1 2 3 4] @*calc))
    (is (= [0 1 2 3 4] (:cache *a)))
    
    (s/reset! *range (range 0 7))
    (is (= ["0" "1" "2" "3" "4" "5" "6"] @*a))
    (is (= [0 1 2 3 4 5 6] @*calc))
    
    (s/reset! *range (range 0 4))
    (is (= ["0" "1" "2" "3"] @*a))
    (is (= [0 1 2 3 4 5 6] @*calc))
    
    (s/reset! *range (range 0 4))
    (is (= ["0" "1" "2" "3"] @*a))
    (is (= [0 1 2 3 4 5 6] @*calc))
    
    (s/reset! *range (range 2 4))
    (is (= ["2" "3"] @*a))
    (is (= [0 1 2 3 4 5 6] @*calc))
    
    (s/reset! *range (range -2 4))
    (is (= ["-2" "-1" "0" "1" "2" "3"] @*a))
    (is (= [0 1 2 3 4 5 6 -2 -1 0 1] @*calc))
    
    (s/reset! *range (range -2 7))
    (is (= ["-2" "-1" "0" "1" "2" "3" "4" "5" "6"] @*a))
    (is (= [0 1 2 3 4 5 6  -2 -1 0 1 4 5 6] @*calc))))

(comment
  (test/run-test effect)
  (test/test-ns *ns*))
