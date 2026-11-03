(ns clojure-1-12.qualified-instance
  (:import
   (java.util ArrayList Collection)))

(defn string-length [s]
  (String/.length s))

(defn remove-object [^ArrayList items]
  (Collection/.remove items (int 0)))

(defn inherited-object-class [value]
  (String/.getClass value))

(defn add-inherited [items]
  (ArrayList/.add items 1))

(defn loop-string-length [value]
  (loop [s "a" n 1]
    (if (pos? n)
      (recur value 0)
      (String/.length s))))

(defn hinted-loop-string-length [value]
  (loop [s "a" n 1]
    (if (pos? n)
      (recur value 0)
      (^[] String/.length s))))
