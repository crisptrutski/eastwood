(ns clojure-1-12.discarded-result)

(defn parse-and-discard [s]
  (^[String] Long/parseLong s)
  :done)

(defn discard-method-value []
  ^[String] Long/parseLong
  :done)
