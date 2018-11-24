(ns trail.state.core
  (:require [trail.state.sql :as tss]
            [trail.leases :as tl]))

(def selection tss/selection)
(def release! tss/release!)
(def trim! tss/trim!)
(def trim-renewals! tss/trim-renewals!)
(def add! tss/add!)
