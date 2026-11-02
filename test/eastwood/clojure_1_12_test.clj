(ns eastwood.clojure-1-12-test
  (:require
   [clojure.test :refer [deftest is]]
   [eastwood.lint :as lint])
  (:import
   (java.util ArrayList)))

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

(defn- removal-result [remove-fn]
  (let [items (ArrayList. ["zero" (int 0)])
        result (remove-fn items)]
    {:result result, :remaining (vec items)}))

(deftest qualified-instance-semantics-test
  (require 'clojure-1-12.qualified-instance :reload)
  (let [native-length (var-get (requiring-resolve 'clojure-1-12.qualified-instance/string-length))
        native-remove (var-get (requiring-resolve 'clojure-1-12.qualified-instance/remove-object))
        {:keys [err warnings]} (lint-fixture 'clojure-1-12.qualified-instance)
        linted-length (var-get (requiring-resolve 'clojure-1-12.qualified-instance/string-length))
        linted-remove (var-get (requiring-resolve 'clojure-1-12.qualified-instance/remove-object))]
    (is (nil? err) (pr-str err))
    (is (empty? warnings) (pr-str (mapv :kind warnings)))
    (is (= 3 (native-length "abc") (linted-length "abc")))
    (is (thrown? ClassCastException (native-length (StringBuilder. "abc"))))
    (is (thrown? ClassCastException (linted-length (StringBuilder. "abc"))))
    (is (= {:result true, :remaining ["zero"]}
           (removal-result native-remove)
           (removal-result linted-remove)))))
