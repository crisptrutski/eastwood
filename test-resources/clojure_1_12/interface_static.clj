(ns clojure-1-12.interface-static
  (:import
   (java.util.concurrent ConcurrentSkipListSet)))

;; Clojure rejects this call because classes do not inherit static methods from interfaces.
(defn empty-set []
  (ConcurrentSkipListSet/of))
