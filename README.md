# inheritable-var [![Build Status](https://travis-ci.org/jiacai2050/inheritable-var.svg?branch=master)](https://travis-ci.org/jiacai2050/inheritable-var) [![Clojars Project](https://img.shields.io/clojars/v/inheritable-var.svg)](https://clojars.org/inheritable-var)

A wrapper of [TransmittableThreadLocal](https://github.com/alibaba/transmittable-thread-local/blob/master/README-EN.md), an enhanced version of [InheritableThreadLocal](http://docs.oracle.com/javase/7/docs/api/java/lang/InheritableThreadLocal.html), to define inheritable variable between threads, even using thread pool.

A thread-inheritable var is expected now and then for dynamic + binding doesn't work very well in multithread. A simple demo from [cemerick](https://cemerick.com/2009/11/03/be-mindful-of-clojures-binding/):

```
user=> (def ^:dynamic *foo* 5)
#'user/*foo*
user=> (defn adder
         [param]
         (+ *foo* param))
#'user/adder
user=> (binding [*foo* 10]
         (doseq [v (pmap adder (repeat 3 5))]
           (println v)))
10
10
10
nil
```
FYI, more discussions:

- https://stackoverflow.com/questions/7387098/threadlocal-counter-in-clojure/7387368
- https://aphyr.com/posts/240-configuration-and-scope
- [Google Group: Get thread local bindings](https://groups.google.com/forum/#!searchin/clojure/inherit$20binding|sort:relevance/clojure/FmsX5SroZJ4/25VYrSmkeDkJ)

## Install

```
[inheritable-var "0.1.3"]
```

## Usage

## Simple thread
```
;; first require this lib
(ns your.ns
  (:require [inheritable-var.core :refer [->inheritable inheritable-var definheritable inheritable-binding]]))

;; create a thread inheritable variable
(def foo (inheritable-var (contantly "foo")))
;; or 
(definheritable foo "foo")

;; get current value
@foo

;; then you can `inheritable-binding` to change foo's value
(inheritable-binding [foo "hello world"]
  ;; you can do stuff in anthor thread here
  ;; @foo always return "hello world" not matter which thread is in
)

;; after inheritable-binding, @foo stay unchanged
@foo   ;; => "foo"
```
## Thread pool

`->inheritable` can decorate Runnable/Callable/Executor to inherit value from calling-thread instead of thread pool, which is mostly useless. 

- https://github.com/alibaba/transmittable-thread-local/blob/master/README-EN.md#2-transmit-value-even-using-thread-pool

```
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

```

## License

Copyright © 2017 Jiacai Liu 

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
