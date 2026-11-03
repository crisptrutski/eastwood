(ns eastwood.test-runner
  (:require
   [clojure.java.io :as io]
   [clojure.test :as test]
   [eastwood.copieddeps.dep9.clojure.tools.namespace.file :as file]
   [eastwood.copieddeps.dep9.clojure.tools.namespace.find :as find]
   [eastwood.copieddeps.dep9.clojure.tools.namespace.parse :as parse])
  (:import
   (clojure.lang Compiler RT)))

(defn find-test-namespaces
  "Return sorted namespaces from the test directories.
  Throw when a source file has an invalid or missing namespace declaration."
  [directories]
  (sort
   (for [directory directories
         source (find/find-sources-in-dir (io/file directory))]
     (try
       (or (some-> (file/read-file-ns-decl source) parse/name-from-ns-decl)
           (throw (ex-info "Missing test namespace declaration" {:file (str source)})))
       (catch Exception e
         (throw (ex-info "Cannot read test namespace" {:file (str source)} e)))))))

(defn run
  "Run the given test namespaces, or discover namespaces under `:test-paths`.
  Search the test directory when neither option is supplied."
  [{:keys [namespaces test-paths]
    :or   {test-paths ["test"]}}]
  (let [namespaces (or namespaces (find-test-namespaces test-paths))]
    ;; Keep classes created during linting visible to tagged record readers.
    (with-bindings {Compiler/LOADER (RT/makeClassLoader)}
      (run! require namespaces)
      (let [{:keys [fail error]} (apply test/run-tests namespaces)]
        (shutdown-agents)
        (when (pos? (+ (long fail) (long error)))
          (System/exit 1))))))
