# inheritable-var [![Build Status](https://travis-ci.org/jiacai2050/inheritable-var.svg?branch=master)](https://travis-ci.org/jiacai2050/inheritable-var) [![Clojars Project](https://img.shields.io/clojars/v/inheritable-var.svg)](https://clojars.org/inheritable-var)

> A wrapper of [TransmittableThreadLocal](https://github.com/alibaba/transmittable-thread-local/blob/master/README-EN.md), an enhanced version of [InheritableThreadLocal](http://docs.oracle.com/javase/7/docs/api/java/lang/InheritableThreadLocal.html), to define inheritable variable between threads, even using thread pool.

[Binding conveyance](https://github.com/clojure/clojure/blob/master/changes.md#234-binding-conveyance) was introduced in Clojure 1.3 which pass dynamic binding to other threads (e.g. send, send-off, pmap, future)
```
(def ^:dynamic *num* 1)
(binding [*num* 2] (future (println *num*)))
;; prints "2", not "1"
```
But binding conveyance doesn't work well with self-defined thread(pool)
```
(def ^:dynamic foo "Main")
(let [^ExecutorService executor (Executors/newFixedThreadPool 2)]
  (binding [foo "Child thread"]
    (.submit executor ^Callable (fn [] (println foo)))
    (.submit executor ^Callable (fn [] (println foo)))
    (.shutdown executor)))
;; both print `Main`
```
[bound-fn](https://clojuredocs.org/clojure.core/bound-fn) can fix this
```
(def ^:dynamic foo "Main")
(let [^ExecutorService executor (Executors/newFixedThreadPool 1)]
  (binding [foo "Child thread"]
    (let [bounded-thread-fn (bound-fn [] (println foo))]
      (.submit executor ^Callable bounded-thread-fn)
      (.submit executor ^Callable bounded-thread-fn)
      (.shutdown executor))))
;; both print `Child thread`
```
Although `bound-fn` can work under multi-thread environment, it can't access new value if the dynamic var is rebound in thread.

`inheritable-var` solve all above problems in `def + binding` way, no more no less.

## Install

```
[inheritable-var "0.1.4"]
```

## Usage

### Simple thread
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
  
  
  (.doSet foo "another-value")  ;; you can temporarily rebind foo to another-value
)

;; after inheritable-binding, @foo stay unchanged
@foo   ;; => "foo"
```
### Thread pool

 Use `->inheritable` multimethod to decorate Runnable/Callable/Executor,  which allows inherit value from calling-thread instead of thread pool.

```
;; decorate Callable
(let [foo (inheritable-var (constantly "foo"))
      ^ExecutorService executor (Executors/newFixedThreadPool 1)]
  (inheritable-binding [foo "foo-in-executor"]
                       (let [foo-reset @(.submit executor (->inheritable (fn [] (.doSet foo "foo-reset" ) @foo)))
                             foo-unchanged @(.submit executor (->inheritable (constantly @foo)))]
                         (println foo-reset)
                         (println foo-unchanged)))

  (println "foo"))

;; decorate Executor
(let [foo (inheritable-var (constantly "foo"))
      ^ExecutorService executor (->inheritable (Executors/newFixedThreadPool 1))]
  (inheritable-binding [foo "foo-in-executor"]
                       (let [foo-reset @(.submit executor ^Callable (fn [] (.doSet foo "foo-reset") @foo))
                             foo-unchanged @(.submit executor ^Callable (constantly @foo))]
                         (println foo-reset)
                         (println foo-unchanged)))
  (println "foo"))


;; Both will print
;; foo-reset
;; foo-in-executor
;; foo
```

### built-in fns

In order to allow built-in fns to use inheritable-var, we need to decorate the thread-pool those fns use.

#### core/async

First make sure `core/async` is in your dependencies. Then

```
(require '[inheritable-var.executors :refer [inheritable-core-async!]])
(inheritable-core-async!)
```

#### pmap/future/agent-related

```
(require '[inheritable-var.executors :refer [inheritable-agent!]])
(inheritable-agent!)
```

### Java Agent

If you don't want to decorate Callable/Executor, you can use Java Agent to load inheritable-var at JVM startup.

Concrete steps please refer to [TransmittableThreadLocal doc](https://github.com/alibaba/transmittable-thread-local/blob/master/README-EN.md#23-use-java-agent-to-decorate-thread-pool-implementation-class).

## Dynamic binding discussion

- https://stackoverflow.com/questions/7387098/threadlocal-counter-in-clojure/7387368
- https://aphyr.com/posts/240-configuration-and-scope
- [Google Group: Get thread local bindings](https://groups.google.com/forum/#!searchin/clojure/inherit$20binding|sort:relevance/clojure/FmsX5SroZJ4/25VYrSmkeDkJ)
- https://cemerick.com/2009/11/03/be-mindful-of-clojures-binding/


## License

Copyright © 2017 Jiacai Liu 

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
