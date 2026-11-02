(ns clojure-1-12.qualified-methods)

(set! *warn-on-reflection* true)

(defn fold-case
  "Fold each code point to upper case, then lower case."
  ^String [^String s]
  (-> (String/.codePoints s)
      (.map ^[int] Character/toUpperCase)
      (.map ^[int] Character/toLowerCase)
      (.collect ^[] StringBuilder/new
                ^[int] StringBuilder/.appendCodePoint
                ^[StringBuffer] StringBuilder/.append)
      str))

(defn parse-longs [strings]
  (mapv ^[String] Long/parseLong strings))

(defn lengths [strings]
  (mapv String/.length strings))

(defn builders [strings]
  (mapv ^[String] StringBuilder/new strings))

(defn byte-array-class []
  byte/1)

(defn object-array-class []
  Object/2)

(defn absolute-longs [values]
  (mapv ^[long] Math/abs values))

(defn empty-builder []
  (^[] StringBuilder/new))
