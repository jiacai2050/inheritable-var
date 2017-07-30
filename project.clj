(defproject inheritable-var "0.1.3"
  :description "A wrapper of TransmittableThreadLocal, an enhanced version of InheritableThreadLocal, to define inheritable variable between threads, even using thread pool"
  :url "https://github.com/jiacai2050/inheritable-var"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.alibaba/transmittable-thread-local "2.1.1"]]
  :scm {:url "https://github.com/jiacai2050/inheritable-var"
        :name "github"}
  :profiles {:dev {:dependencies [[org.clojure/core.async "0.3.443"]]}}
  :deploy-repositories [["releases" :clojars]])
