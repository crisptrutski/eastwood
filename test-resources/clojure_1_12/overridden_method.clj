(ns clojure-1-12.overridden-method)

(defn unhinted-method-value []
  java.sql.Date/.getHours)

(defn hinted-method-value []
  ^[] java.sql.Date/.getHours)
