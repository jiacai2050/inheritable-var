(ns inheritable-var.core-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer [chan >! <!! go]]
            [inheritable-var.core :refer :all])
  (:import [java.util.concurrent CountDownLatch Executors ExecutorService]))

(deftest thread-transmitabble-test
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
    (testing "thread pool..."
      (inheritable-binding [foo "foo-in-executor"
                            bar "bar-in-executor"]
                           (let [^ExecutorService executor (Executors/newFixedThreadPool 1)
                                 [foo-in-executor bar-in-executor] @(.submit executor ^Callable (constantly [@foo @bar]))]
                             (is (= "foo-in-executor" foo-in-executor))
                             (is (= "bar-in-executor" bar-in-executor))))
      (is (= @foo "foo"))
      (is (= @bar "bar")))
    (testing "pmap ..."
      (let [x (inheritable-var (constantly 5))
            adder #(+ % @x)]
        (inheritable-binding [x 10]
                             (doseq [s (pmap adder (repeat 3 5))]
                               (is (= s 15))))))))
