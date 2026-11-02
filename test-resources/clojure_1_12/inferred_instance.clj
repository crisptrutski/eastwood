(ns clojure-1-12.inferred-instance)

(defn file-exists []
  (let [make-file (fn [] (java.io.File. "."))]
    (.exists (make-file))))
