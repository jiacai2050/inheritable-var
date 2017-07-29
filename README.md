# inheritable-var [![Build Status](https://travis-ci.org/jiacai2050/inheritable-var.svg?branch=master)](https://travis-ci.org/jiacai2050/inheritable-var) [![Clojars Project](https://img.shields.io/clojars/v/inheritable-var.svg)](https://clojars.org/inheritable-var)

A wrapper of [InheritableThreadLocal](http://docs.oracle.com/javase/7/docs/api/java/lang/InheritableThreadLocal.html) to define thread-inheritable variable.

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
[inheritable-var "0.1.1"]
```

## Usage

```
;; first require this lib
(ns your.ns
  (:require [inheritable-var.core :refer [inheritable-var definheritable inheritable-binding]]))

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

## License

Copyright © 2017 Jiacai Liu 

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
