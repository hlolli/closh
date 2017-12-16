(ns closh.test-util.runner
  (:require [goog.object :as gobj]
            [clojure.test :refer [report run-tests]]
            [closh.core-test]
            [closh.util-test]
            [closh.completion-test]))

(defmethod report [:cljs.test/default :end-run-tests] [m]
  (if (cljs.test/successful? m)
    (gobj/set js/process "exitCode" 0)
    (gobj/set js/process "exitCode" 1)))

(time
 (run-tests
   'closh.core-test
   'closh.util-test
   'closh.completion-test))