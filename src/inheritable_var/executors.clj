(ns inheritable-var.executors
  (:require [inheritable-var.core :refer [->inheritable]])
  (:import [java.util.concurrent Executors ThreadFactory]
           java.util.concurrent.atomic.AtomicLong))

(def ^:private core-async-enable? 
  (try
    (require '[clojure.core.async.impl.concurrent :as conc]
             '[clojure.core.async.impl.dispatch :as dispatch]
             '[clojure.core.async.impl.protocols :as protocols])
    true
    (catch Throwable e
      (require '[inheritable-var.async.conc :as conc]
               '[inheritable-var.async.dispatch :as dispatch]
               '[inheritable-var.async.protocols :as protocols])
      false)))

(defn inheritable-core-async! []
  (if core-async-enable?
    (let [pool-size (delay (or (when-let [prop (System/getProperty "clojure.core.async.pool-size")]
                                 (Long/parseLong prop))
                               8))
          async-executor (->inheritable (Executors/newFixedThreadPool @pool-size
                                                                      (conc/counted-thread-factory "inheritable-dispatch-%d" true)))
          async-executor (reify protocols/Executor
                           (protocols/exec [this r]
                             (.execute async-executor ^Callable r)))]
      ;; https://stackoverflow.com/a/38577871/2163429
      (alter-var-root #'dispatch/executor
                      (constantly (delay async-executor))))
    (println "If you use core/async please add it in your dependency!")))

(defn- create-thread-factory [^String fmt ^AtomicLong counter]
  (reify ThreadFactory
    (^Thread newThread [this ^Runnable runnable]
      (let [^Thread task (Thread. runnable)]
        (.setName task (format fmt (.getAndIncrement counter)))
        task))))

(defn inheritable-agent! []
  (let [cpu (.. Runtime getRuntime availableProcessors)
        pooled-counter (AtomicLong. 0)
        pooled-executor (->inheritable (Executors/newFixedThreadPool (+ 2 cpu) (create-thread-factory "inheritable-agent-send-pool-%d" pooled-counter)))
        solo-counter (AtomicLong. 0)
        solo-executor (->inheritable (Executors/newCachedThreadPool (create-thread-factory "inheritable-agent-send-off-pool-%d" solo-counter)))]
    (set-agent-send-executor! pooled-executor)
    (set-agent-send-off-executor! solo-executor)))
