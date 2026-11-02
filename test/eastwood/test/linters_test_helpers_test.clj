(ns eastwood.test.linters-test-helpers-test
  (:require
   [clojure.test :refer [deftest is]]
   [eastwood.lint :as lint]
   [eastwood.test.linters-test :as linters-test])
  (:import
   (clojure.lang ExceptionInfo)))

(defn- warnings-from [result]
  (with-redefs [lint/lint-ns (fn [& _args] result)]
    (linters-test/lint-ns-noprint 'fixture [] lint/default-opts)))

(deftest successful-warning-data-test
  (is (= [{:linter :unused-ret-vals}]
         (warnings-from {:lint-results [{:lint-warning [{:warn-data {:linter :unused-ret-vals}}]}]}))))

(deftest analyzer-errors-propagate-test
  (is (thrown-with-msg? ExceptionInfo #"reader failure"
                        (warnings-from {:analyzer-exception {:exception (ex-info "reader failure" {})}}))))

(deftest linter-errors-propagate-test
  (is (thrown-with-msg? ExceptionInfo #"linter failure"
                        (warnings-from {:lint-results [{:lint-error [{:exception (ex-info "linter failure" {})}]}]}))))
