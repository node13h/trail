(ns trail.api.public
  (:require [trail.api.core :as tac]
            [schema.core :as s]
            [ring.swagger.json-schema :as rjs]
            [trail.state.core :as ts]))


(s/defschema InputLease
  (rjs/field
   {:ip s/Str
    :mac s/Str
    :start-date s/Str
    :duration s/Int
    (s/optional-key :data) {s/Any s/Any}}
   {:example {:ip "192.168.0.1"
              :mac "00:de:ad:be:ef:00"
              :start-date "2001-01-01 12:45:01"
              :duration 3600
              :data {}}}))

(s/defschema Lease
  (rjs/field
   {:id s/Str
    :ip s/Str
    :mac s/Str
    :start-date s/Str
    :duration s/Int
    (s/optional-key :data) {s/Any s/Any}}
   {:example {:id "123e4567-e89b-12d3-a456-426655440000"
              :ip "192.168.0.1"
              :mac "00:de:ad:be:ef:00"
              :start-date "2001-01-01 12:45:01"
              :duration 3600
              :data {}}}))

(defn leases
  "Return a list of aggregated leases filtered by the optional filters"
  [filters tz]
  (tac/leases ts/selection filters tz))

(defn add!
  "Add multiple leases to the store"
  [leases tz]
  (tac/add ts/add! leases tz))

(defn release!
  "Release leases"
  [ip end-date tz]
  (tac/release ts/release! ip end-date tz))

(defn trim!
  "Delete all leases ending before the to-date"
  [to-date tz]
  (tac/trim ts/trim! to-date tz))

(defn trim-renewals!
  "Delete all renewals before the to-date"
  [to-date tz]
  (tac/trim-renewals ts/trim-renewals! to-date tz))
