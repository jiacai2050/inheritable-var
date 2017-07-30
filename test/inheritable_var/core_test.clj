(ns inheritable-var.core-test
  (:require [clojure.core.async :as async :refer [<!! >! chan go]]
            [clojure.test :refer :all]
            [inheritable-var.core :refer :all]
            [inheritable-var.executors :refer :all])
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
      (inheritable-core-async!)
      (dotimes [_ 20]
        (inheritable-binding [foo "foo-in-async"
                              bar "bar-in-async"]
                             (let [c (chan)
                                   _ (go (>! c [@foo @bar]))
                                   [foo-in-async bar-in-async] (<!! c)]
                               (is (= foo-in-async "foo-in-async"))
                               (is (= bar-in-async "bar-in-async")))))
      (is (= @foo "foo"))
      (is (= @bar "bar")))
    (testing "pmap/future ..."
      (inheritable-agent!)
      (dotimes [_ 20]
        (let [x (inheritable-var (constantly 5))
              adder #(+ % @x)]
          (inheritable-binding [x 10]
                               (doseq [s (pmap adder (repeat 3 5))]
                                 (is (= s 15)))
                               (is (= 10 @(future @x)))))))))

(deftest thread-pool-test
  (testing "->inheritable fn..."
    (let [foo (inheritable-var (constantly "foo"))
          ^ExecutorService executor (Executors/newFixedThreadPool 9)]
      (dotimes [_ 100]
        (inheritable-binding [foo "foo-in-executor"]
                             (let [foo-reset @(.submit executor (->inheritable (fn [] (.doSet foo "foo-reset" ) @foo)))
                                   foo-unchanged @(.submit executor (->inheritable (constantly @foo)))]
                               (is (= "foo-reset" foo-reset))
                               (is (= "foo-in-executor" foo-unchanged)))))

      (is (= @foo "foo"))))
  (testing "->inheritable executor..."
    (let [foo (inheritable-var (constantly "foo"))
          ^ExecutorService executor (->inheritable (Executors/newFixedThreadPool 9))]
      (dotimes [_ 100]
        (inheritable-binding [foo "foo-in-executor"]
                             (let [foo-reset @(.submit executor ^Callable (fn [] (.doSet foo "foo-reset") @foo))
                                   foo-unchanged @(.submit executor ^Callable (constantly @foo))]
                               (is (= "foo-reset" foo-reset))
                               (is (= "foo-in-executor" foo-unchanged)))))
      (is (= @foo "foo")))))
