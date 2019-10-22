(ns trail.util
  (:require [clojure.java.io :as io]))

(def version (clojure.string/trim-newline (slurp (io/resource "VERSION"))))
