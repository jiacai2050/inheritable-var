(ns inheritable-var.core-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer [chan >! <!! go]]
            [inheritable-var.core :refer :all])
  (:import [java.util.concurrent CountDownLatch Executors ExecutorService]))

(deftest single-thread-test
  (let [foo (inheritable-var (constantly "foo"))
        bar (inheritable-var (constantly "bar"))]
    (testing "simple thread..."
      (inheritable-binding [foo "foo-in-thread"
                              bar "bar-in-thread"]
                             (let [latch (CountDownLatch. 1)]
                               (.start (Thread. ^Callable (fn []
                                                            (is (= "foo-in-thread" @foo))
                                                            (is (= "bar-in-thread" @bar))
                                                            (.countDown latch))))
                               (.await latch)))
      (is (= @foo "foo"))
      (is (= @bar "bar")))
    (testing "core/async..."
      (inheritable-binding [foo "foo-in-async"
                            bar "bar-in-async"]
                           (let [c (chan)
                                 _ (go (>! c [@foo @bar]))
                                 [foo-in-async bar-in-async] (<!! c)]
                             (is (= foo-in-async "foo-in-async"))
                             (is (= bar-in-async "bar-in-async"))))
      (is (= @foo "foo"))
      (is (= @bar "bar")))
    (testing "pmap ..."
      (let [x (inheritable-var (constantly 5))
            adder #(+ % @x)]
        (inheritable-binding [x 10]
                             (doseq [s (pmap adder (repeat 100 5))]
                               (is (= s 15))))))))

(deftest thread-pool-test
  (let [foo (inheritable-var (constantly "foo"))]
    (testing "->inheritable fn..."
      (inheritable-binding [foo "foo-in-executor"]
                           (let [^ExecutorService executor (Executors/newFixedThreadPool 1)
                                 foo-reset @(.submit executor (->inheritable (fn [] (.doSet foo "foo-reset" ) @foo)))
                                 foo-unchanged @(.submit executor (->inheritable (constantly @foo)))]
                             (is (= "foo-reset" foo-reset))
                             (is (= "foo-in-executor" foo-unchanged))))

      (is (= @foo "foo")))
    (testing "->inheritable executor..."
      (inheritable-binding [foo "foo-in-executor"]
                           (let [^ExecutorService executor (->inheritable (Executors/newFixedThreadPool 1))
                                 foo-reset @(.submit executor ^Callable (fn [] (.doSet foo "foo-reset") @foo))
                                 foo-unchanged @(.submit executor ^Callable (constantly @foo))]
                             (is (= "foo-reset" foo-reset))
                             (is (= "foo-in-executor" foo-unchanged))))
      (is (= @foo "foo")))))
