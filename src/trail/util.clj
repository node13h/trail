(ns trail.util
  (:require [clojure.java.io :as io]))

(defn pom-properties
  []
  (try
    (with-open [reader (-> "META-INF/maven/trail/trail/pom.properties"
                           io/resource
                           io/reader)]
      (doto (java.util.Properties.)
        (.load reader)))
    (catch java.io.FileNotFoundException e)))

(def version (get (pom-properties) "version"))
