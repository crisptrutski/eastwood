(ns eastwood.clojure-1-12-test
  (:require
   [clojure.test :refer [deftest is]]
   [eastwood.lint :as lint])
  (:import
   (java.lang.reflect Member)
   (java.util ArrayList Collection LinkedList)))

(defn- lint-fixture
  "Return a fixture's warning data, or throw its analysis exception."
  [namespace-sym]
  (let [{:keys [err warnings]}
        (lint/with-memoization-bindings
          (lint/lint {:source-paths        ["test-resources"]
                      :test-paths          []
                      :namespaces          [namespace-sym]
                      :rethrow-exceptions? true}))]
    (when err
      (throw (:exception err)))
    (mapv :warn-data warnings)))

(defn- fixture-function [namespace-sym function-name]
  (var-get (ns-resolve namespace-sym function-name)))

(deftest static-method-values-test
  (let [namespace-sym 'clojure-1-12.qualified-methods]
    (is (empty? (lint-fixture namespace-sym)))
    (is (= [12 34] ((fixture-function namespace-sym 'parse-longs) ["12" "34"])))))

(deftest instance-method-values-test
  (let [namespace-sym 'clojure-1-12.qualified-methods]
    (is (empty? (lint-fixture namespace-sym)))
    (is (= [1 2] ((fixture-function namespace-sym 'lengths) ["a" "ab"])))))

(deftest constructor-method-values-test
  (let [namespace-sym 'clojure-1-12.qualified-methods]
    (is (empty? (lint-fixture namespace-sym)))
    (is (= ["a" "ab"] (mapv str ((fixture-function namespace-sym 'builders) ["a" "ab"]))))))

(deftest array-class-syntax-test
  (let [namespace-sym 'clojure-1-12.qualified-methods]
    (is (empty? (lint-fixture namespace-sym)))
    (is (= (class (byte-array 0)) ((fixture-function namespace-sym 'byte-array-class))))
    (is (= (class (make-array Object 0 0)) ((fixture-function namespace-sym 'object-array-class))))))

(deftest primitive-parameter-tags-test
  (let [namespace-sym 'clojure-1-12.qualified-methods]
    (is (empty? (lint-fixture namespace-sym)))
    (is (= [1 2] ((fixture-function namespace-sym 'absolute-longs) [-1 -2])))))

(deftest zero-argument-constructor-tags-test
  (let [namespace-sym 'clojure-1-12.qualified-methods]
    (is (empty? (lint-fixture namespace-sym)))
    (is (= "" (str ((fixture-function namespace-sym 'empty-builder)))))))

(deftest unicode-case-folding-test
  (let [namespace-sym 'clojure-1-12.qualified-methods]
    (is (empty? (lint-fixture namespace-sym)))
    (let [fold-case (fixture-function namespace-sym 'fold-case)]
      (is (= "ist" (fold-case "İſT")))
      (is (= "\uD801\uDC28" (fold-case "\uD801\uDC00"))))))

(defn- native-and-linted
  "Return a `clojure-1-12.qualified-instance` function as Clojure compiles it and as Eastwood evaluates it.
  Assert that linting the namespace reports no warnings."
  [function-name]
  (let [namespace-sym 'clojure-1-12.qualified-instance]
    (require namespace-sym :reload)
    (let [native   (fixture-function namespace-sym function-name)
          warnings (lint-fixture namespace-sym)
          linted   (fixture-function namespace-sym function-name)]
      (is (empty? warnings))
      [native linted])))

(deftest qualified-receiver-cast-test
  (let [[native-length linted-length] (native-and-linted 'string-length)]
    (is (= 3 (native-length "abc") (linted-length "abc")))
    (is (thrown? ClassCastException (native-length (StringBuilder. "abc"))))
    (is (thrown? ClassCastException (linted-length (StringBuilder. "abc"))))))

(defn- removal-result [remove-fn]
  (let [items  (ArrayList. ^Collection (vector "zero" (int 0)))
        result (remove-fn items)]
    {:result result, :remaining (vec items)}))

(deftest qualified-overload-selection-test
  (let [[native-remove linted-remove] (native-and-linted 'remove-object)]
    (is (= {:result true, :remaining ["zero"]}
           (removal-result native-remove)
           (removal-result linted-remove)))))

(deftest inherited-qualified-receiver-test
  (let [[native-class linted-class] (native-and-linted 'inherited-object-class)
        value                       (Object.)]
    (is (= Object (native-class value) (linted-class value)))))

(deftest inherited-method-qualifier-test
  (let [[native-add linted-add] (native-and-linted 'add-inherited)]
    (is (= true (native-add (ArrayList.)) (linted-add (ArrayList.))))
    (is (thrown? ClassCastException (native-add (LinkedList.))))
    (is (thrown? ClassCastException (linted-add (LinkedList.))))))

(deftest qualified-call-on-widened-loop-local-test
  (let [[native-length linted-length] (native-and-linted 'loop-string-length)]
    (is (= 4 (native-length "abcd") (linted-length "abcd")))
    (is (thrown? ClassCastException (native-length (StringBuilder. "abcd"))))
    (is (thrown? ClassCastException (linted-length (StringBuilder. "abcd"))))))

(deftest hinted-call-on-widened-loop-local-test
  (let [[native-length linted-length] (native-and-linted 'hinted-loop-string-length)]
    (is (= 4 (native-length "abcd") (linted-length "abcd")))
    (is (thrown? ClassCastException (native-length (StringBuilder. "abcd"))))
    (is (thrown? ClassCastException (linted-length (StringBuilder. "abcd"))))))

(deftest interface-static-method-test
  (let [e (is (thrown? Exception (lint-fixture 'clojure-1-12.interface-static)))]
    (is (re-find #"No matching method of found taking 0 args" (str (ex-message (ex-cause e)))))))

(deftest discarded-qualified-call-test
  (let [warnings (lint-fixture 'clojure-1-12.discarded-result)
        calls    (filter #(= :static-call (:kind %)) warnings)]
    (is (= [[:unused-ret-vals :static-call]] (mapv (juxt :linter :kind) calls)))))

(deftest discarded-method-value-test
  (let [warnings (lint-fixture 'clojure-1-12.discarded-result)
        values   (filter #(= :method-value (:kind %)) warnings)]
    (is (= [[:unused-ret-vals :method-value]] (mapv (juxt :linter :kind) values)))))

(deftest deprecated-qualified-calls-test
  (let [warnings (lint-fixture 'clojure-1-12.deprecated-methods)
        calls    (filter #(#{:instance-call :new} (:kind %)) warnings)]
    (is (= {[:deprecations :instance-call] 1, [:deprecations :new] 1}
           (frequencies (map (juxt :linter :kind) calls))))))

(deftest deprecated-method-values-test
  (let [warnings (lint-fixture 'clojure-1-12.deprecated-methods)
        values   (filter #(= :method-value (:kind %)) warnings)]
    (is (= [:deprecations :deprecations :deprecations] (mapv :linter values)))
    (is (= #{"getYear" "java.util.Date" "parse"}
           (set (map (fn [warning] (.getName ^Member (first (:var warning)))) values))))))

(deftest ambiguous-method-values-test
  (let [warnings (lint-fixture 'clojure-1-12.deprecated-methods)]
    (is (= {:deprecations 5, :reflection 1} (frequencies (map :linter warnings))))))

(deftest deprecated-overridden-method-value-test
  (let [warnings (lint-fixture 'clojure-1-12.overridden-method)]
    (is (= [[:deprecations :method-value] [:deprecations :method-value]]
           (mapv (juxt :linter :kind) warnings)))
    (is (= #{java.sql.Date}
           (set (map (fn [warning] (.getDeclaringClass ^Member (first (:var warning)))) warnings))))))

(deftest ordinary-inferred-call-reflection-test
  (let [warnings (lint-fixture 'clojure-1-12.inferred-instance)]
    (is (= {:reflection 1} (frequencies (map :linter warnings))))))
