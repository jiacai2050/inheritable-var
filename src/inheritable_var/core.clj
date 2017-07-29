(ns inheritable-var.core
  (:import [clojure.lang IDeref Settable]))

(defn inheritable-var [init-fn & [child-fn]]
  (let [child-fn (when-not child-fn identity)
        stub (proxy [InheritableThreadLocal] []
               (initialValue [] (init-fn))
               (childValue [parent-value] (child-fn parent-value)))]
    (reify
      IDeref
      (deref [this]
        (.get stub))
      Settable 
      (doSet [this value]
        (.set stub value))
      (doReset [this value]
        (.doSet this value)))))

(defmacro definheritable [name value]
  `(def ~name (inheritable-var (constantly ~value))))

;; copied from clojure.core
(defmacro ^{:private true} assert-args
  [& pairs]
  `(do (when-not ~(first pairs)
         (throw (IllegalArgumentException.
                 (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))))
       ~(let [more (nnext pairs)]
          (when more
            (list* `assert-args more)))))

(defn set-inheritable-binding! [bindings-map]
  (doseq [[k v] bindings-map]
    (.doSet k v)))

(defmacro inheritable-binding [name-vals-vec & body]
  (assert-args
   (vector? name-vals-vec) "a vector for bindings"
   (even? (count name-vals-vec)) "an even number of forms in binding vector")
  (let [[ks vs] (reduce (fn [[ks vs] [k v]]
                          [(conj ks k) (conj vs v)])
                        []
                        (partition 2 name-vals-vec))]
    `(let [inner-binding# (hash-map ~@name-vals-vec)
           outer-binding# (into {} (for [[k# v#] inner-binding#]
                                     [k# (deref k#)]))]
       (try
         (set-inheritable-binding! inner-binding#)
         ~@body
         (finally 
           (set-inheritable-binding! outer-binding#))))))
