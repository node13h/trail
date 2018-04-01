(ns trail.state.core
  (:require [trail.state.sql :as tss]))

(def sorted-selection tss/sorted-selection)
(def add! tss/add!)
(def release! tss/release!)
(def trim! tss/trim!)
