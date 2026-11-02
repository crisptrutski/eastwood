(ns eastwood.qualified-calls-test
  (:require
   [clojure.test :refer [deftest is]]
   [eastwood.copieddeps.dep1.clojure.tools.analyzer.ast :as ast]
   [eastwood.copieddeps.dep2.clojure.tools.analyzer.jvm :as analyzer]))

(deftest widened-loop-call-validation-test
  (let [form     '(fn [value]
                    (loop [s "a" n 1]
                      (if (pos? n)
                        (recur value 0)
                        (String/.length s))))
        analyzed (analyzer/analyze form)
        call     (first (filter #(and (= :instance-call (:op %))
                                      (= 'length (:method %)))
                                (ast/nodes analyzed)))]
    (is (:validated? call))
    (is (= String (:class call)))
    (is (= Integer/TYPE (:tag call)))))
