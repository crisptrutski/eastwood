(ns eastwood.parameter-tags-test
  (:require
   [clojure.test :refer [deftest is]]
   [eastwood.copieddeps.dep2.clojure.tools.analyzer.jvm :as analyzer]
   [eastwood.copieddeps.dep2.clojure.tools.analyzer.jvm.utils :as utils])
  (:import
   (java.nio Buffer ByteBuffer)))

(deftest covariant-bridge-selection-test
  (let [methods (filter #(and (= 'position (:name %))
                              (= [Integer/TYPE] (mapv utils/maybe-class (:parameter-types %))))
                        (utils/members ByteBuffer))]
    (is (= #{Buffer ByteBuffer} (set (map (comp utils/maybe-class :return-type) methods))))
    (doseq [candidates [methods (reverse methods)]]
      (let [resolved (utils/resolve-hinted-method candidates '[int])]
        (is (= ByteBuffer (utils/maybe-class (:return-type resolved))))
        (is (not (contains? (:flags resolved) :bridge)))))))

(deftest covariant-call-return-type-test
  (let [form '(^[int] java.nio.ByteBuffer/.position (java.nio.ByteBuffer/allocate 1) 0)
        ast  (analyzer/analyze form)]
    (is (= :instance-call (:op ast)))
    (is (= ByteBuffer (utils/maybe-class (:tag ast))))
    (is (instance? ByteBuffer (:result (analyzer/analyze+eval form))))))

(deftest ambiguous-parameter-tags-test
  (let [methods (utils/instance-methods ByteBuffer 'get 1)]
    (is (nil? (utils/resolve-hinted-method methods '[_])))))
