(ns trail.api.core
  (:require [trail.leases :as tl]
            [clj-time.format :as tf]
            [clj-time.core :as t]))

(defn dt-formatter
  "dt formatter for the API"
  [tz]
  (tf/formatter "yyyy-MM-dd HH:mm:ss" (t/time-zone-for-id tz)))

(defn parsed-dt
  "Convert a string to dt"
  [s tz]
  (tf/parse (dt-formatter tz) s))

(defn formatted-dt
  "Convert dt to a string"
  [dt tz]
  (tf/unparse (dt-formatter tz) dt))

(def dt-keys #{:from-date :to-date :start-date :end-date})
(def uuid-keys #{:id})
(def ip-keys #{:ip})

(defn kv-parser
  "Parse values"
  [k v tz]
  (cond
    (and (some? v) (contains? dt-keys k)) (parsed-dt v tz)
    (and (some? v) (contains? uuid-keys k)) (java.util.UUID/fromString v)
    (and (some? v) (contains? ip-keys k)) (java.net.InetAddress/getByName v)
    :else v))

(defn kv-formatter
  "Format values"
  [k v tz]
  (cond
    (and (some? v) (contains? dt-keys k)) (formatted-dt v tz)
    (and (some? v) (contains? uuid-keys k)) (str v)
    (and (some? v) (contains? ip-keys k)) (.getHostAddress v)
    :else v))

(defn parsed-map
  "Parse map"
  [m tz]
  (-> #(assoc %1 %2 (kv-parser %2 %3 tz))
      (reduce-kv (empty m) m)))

(defn formatted-map
  "Format map"
  [m tz]
  (-> #(assoc %1 %2 (kv-formatter %2 %3 tz))
      (reduce-kv (empty m) m)))

(defn parsed-maps
  "Parse collection of maps"
  [coll tz]
  (map #(parsed-map %1 tz) coll))

(defn formatted-maps
  "Format collection of maps"
  [coll tz]
  (map #(formatted-map %1 tz) coll))

(defn add
  "Add multiple leases using add-f"
  [add-f leases tz]
  (let [leases (parsed-maps leases tz)]
    (doall (map add-f leases))))

(defn leases
  "Aggregate and format a list of leases produced by selection-f"
  [selection-f filters tz]
  (let [filters (parsed-map filters tz)]
    (-> filters
        selection-f
        (formatted-maps tz))))

(defn release
  "Release leases"
  [release-f ip end-date tz]
  (let [end-date (parsed-dt end-date tz)]
    (release-f {:ip ip :end-date end-date})))

(defn trim
  "Use trim-f to delete all leases ending before the to-date"
  [trim-f to-date tz]
  (let [to-date (parsed-dt to-date tz)]
    (trim-f {:to-date to-date})))

(defn trim-renewals
  "Use trim-renewals-f to delete all renewals before the to-date"
  [trim-renewals-f to-date tz]
  (let [to-date (parsed-dt to-date tz)]
    (trim-renewals-f {:to-date to-date})))

(defn trim-releases
  "Use trim-releases-f to delete all releases before the to-date"
  [trim-releases-f to-date tz]
  (let [to-date (parsed-dt to-date tz)]
    (trim-releases-f {:to-date to-date})))
