(ns clojure-1-12.deprecated-methods
  (:import
   (java.util Date)))

(defn direct-year [^Date date]
  (Date/.getYear date))

(defn mapped-years [dates]
  (mapv Date/.getYear dates))

(defn direct-date []
  (Date/new 120 0 1))

(defn date-builder []
  ^[int int int] Date/new)

(defn ambiguous-date-builder []
  Date/new)
