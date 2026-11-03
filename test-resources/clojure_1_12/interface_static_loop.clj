(ns clojure-1-12.interface-static-loop
  (:import
   (java.util.concurrent ConcurrentSkipListSet)))

;; Widening the loop local revalidates the call, which must keep the source class.
(defn pair-set [value]
  (loop [x "a" n 1]
    (if (pos? n)
      (recur value 0)
      (ConcurrentSkipListSet/of x x))))
