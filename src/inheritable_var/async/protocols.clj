(ns inheritable-var.async.protocols)

(defprotocol Executor
  (exec [e runnable] "mocked"))
