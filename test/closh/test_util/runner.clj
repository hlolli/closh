(ns closh.test-util.runner
  (:require [clojure.test :refer [run-tests]]
            [closh.compiler-test]))

(defn -main[]
  (time
    (run-tests
     'closh.compiler-test)))
