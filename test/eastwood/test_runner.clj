(ns eastwood.test-runner
  (:require
   [clojure.java.io :as io]
   [clojure.test :as test]
   [eastwood.copieddeps.dep9.clojure.tools.namespace.find :as find])
  (:import
   (clojure.lang Compiler RT)))

(defn run
  "Run the given test namespaces, or all Eastwood tests when `:namespaces` is absent."
  [{:keys [namespaces]}]
  (let [namespaces (or namespaces (sort (find/find-namespaces-in-dir (io/file "test"))))]
    ;; Keep classes created during linting visible to tagged record readers.
    (with-bindings {Compiler/LOADER (RT/makeClassLoader)}
      (run! require namespaces)
      (let [{:keys [fail error]} (apply test/run-tests namespaces)]
        (shutdown-agents)
        (when (pos? (+ fail error))
          (System/exit 1))))))
