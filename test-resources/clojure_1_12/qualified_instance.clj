(ns clojure-1-12.qualified-instance
  (:import
   (java.util ArrayList Collection)))

(defn string-length [s]
  (String/.length s))

(defn remove-object [^ArrayList items]
  (Collection/.remove items (int 0)))
