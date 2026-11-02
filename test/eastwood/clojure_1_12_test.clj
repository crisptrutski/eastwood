(ns eastwood.clojure-1-12-test
  (:require
   [clojure.test :refer [deftest is]]
   [eastwood.lint :as lint]))

(defn- lint-fixture [namespace-sym]
  (lint/with-memoization-bindings
    (lint/lint {:source-paths        ["test-resources"]
                :test-paths          []
                :namespaces          [namespace-sym]
                :rethrow-exceptions? true})))

(deftest qualified-methods-test
  (let [{:keys [err warnings]} (lint-fixture 'clojure-1-12.qualified-methods)]
    (is (nil? err) (pr-str err))
    (is (empty? warnings) (pr-str (mapv :kind warnings)))
    (when-not err
      (is (= "ist" ((requiring-resolve 'clojure-1-12.qualified-methods/fold-case) "İſT")))
      (is (= "\uD801\uDC28" ((requiring-resolve 'clojure-1-12.qualified-methods/fold-case) "\uD801\uDC00")))
      (is (= (class (byte-array 0)) ((requiring-resolve 'clojure-1-12.qualified-methods/byte-array-class))))
      (is (= (class (make-array Object 0 0)) ((requiring-resolve 'clojure-1-12.qualified-methods/object-array-class))))
      (is (= [1 2] ((requiring-resolve 'clojure-1-12.qualified-methods/absolute-longs) [-1 -2])))
      (is (= "" (str ((requiring-resolve 'clojure-1-12.qualified-methods/empty-builder)))))
      (is (= [12 34] ((requiring-resolve 'clojure-1-12.qualified-methods/parse-longs) ["12" "34"])))
      (is (= [1 2] ((requiring-resolve 'clojure-1-12.qualified-methods/lengths) ["a" "ab"])))
      (is (= ["a" "ab"] (mapv str ((requiring-resolve 'clojure-1-12.qualified-methods/builders) ["a" "ab"])))))))

(deftest discarded-qualified-method-result-test
  (let [{:keys [err warnings]} (lint-fixture 'clojure-1-12.discarded-result)]
    (is (nil? err) (pr-str err))
    (is (= [:unused-ret-vals :unused-ret-vals] (mapv (comp :linter :warn-data) warnings)))
    (is (= {:static-call 1, :method-value 1} (frequencies (map (comp :kind :warn-data) warnings))))))

(deftest deprecated-qualified-methods-test
  (let [{:keys [err warnings]} (lint-fixture 'clojure-1-12.deprecated-methods)
        diagnostics (mapv :warn-data warnings)
        deprecations (filter #(= :deprecations (:linter %)) diagnostics)]
    (is (nil? err) (pr-str err))
    (is (= {:deprecations 4, :reflection 1} (frequencies (map :linter diagnostics))))
    (is (= {:instance-call 1, :new 1, :method-value 2}
           (frequencies (map :kind deprecations))))))
