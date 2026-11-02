(ns eastwood.test-runner-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]
   [eastwood.test-runner :as runner])
  (:import
   (java.nio.file Files)
   (java.nio.file.attribute FileAttribute)))

(defn- with-test-source [source f]
  (let [directory (.toFile (Files/createTempDirectory "eastwood-test-discovery" (make-array FileAttribute 0)))
        file      (io/file directory "example.clj")]
    (try
      (spit file source)
      (f directory)
      (finally
        (.delete file)
        (.delete directory)))))

(deftest discover-test-namespace-test
  (with-test-source "(ns example-test)"
    (fn [directory]
      (is (= '[example-test] (runner/find-test-namespaces [directory]))))))

(deftest malformed-test-namespace-test
  (with-test-source "(ns example-test"
    (fn [directory]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Cannot read test namespace"
                            (runner/find-test-namespaces [directory]))))))

(deftest missing-test-namespace-test
  (with-test-source "(def x 1)"
    (fn [directory]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Cannot read test namespace"
                            (runner/find-test-namespaces [directory]))))))
