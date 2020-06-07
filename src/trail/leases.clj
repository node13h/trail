(ns trail.leases
  (:require [clj-time.core :as t]
            [clj-time.coerce :as tc]))

(defn end-date
  "Return end date for the lease"
  [lease]
  (t/plus (:start-date lease) (t/seconds (:duration lease))))

(defn start-epoch
  "Return start-date in UNIX epoch"
  [lease]
  (tc/to-epoch (:start-date lease)))

(defn interval-seconds
  "Calculate interval in seconds between two dates"
  [dt1 dt2]
  (t/in-seconds (t/interval dt1 dt2)))

(defn same-ip?
  "Return true if both leases have the same IP address"
  [lease1 lease2]
  (.equals (:ip lease1) (:ip lease2)))

(defn same-mac?
  "Return true if both leases have the smae MAC address"
  [lease1 lease2]
  (= (:mac lease1) (:mac lease2)))

(defn sorted
  "Return a sequence of leases sorted by IP and start-date"
  [coll]
  (sort-by (juxt #(.getHostAddress (:ip %)) start-epoch) coll))

(defn truncated
  "Truncate a lease at the specified date"
  [lease at-date]
  (let [new-duration (interval-seconds (:start-date lease) at-date)]
    (if (> (:duration lease) new-duration)
      (assoc lease :duration new-duration)
      lease)))

(defn adjust-start-date
  "Change the start date for the lease keeping the end date the same"
  [lease start-date]
  (->> {:start-date start-date
        :duration (- (:duration lease) (interval-seconds (:start-date lease) start-date))}
       (merge lease)))

(defn same-lease?
  "Return true if start-date and ip addresses of both leases are the same"
  [lease1 lease2]
  (and
   (t/equal? (:start-date lease1) (:start-date lease2))
   (same-ip? lease1 lease2)))

(defn active?
  "Return true if the lease was active during the specified date range"
  [from-date to-date lease]
  (let [start-date (:start-date lease)
        end-date (end-date lease)]
    (and (or (t/before? start-date to-date) (t/equal? start-date to-date))
         (or (t/after? end-date from-date) (t/equal? end-date from-date)))))

(defn no-gap?
  "Return true if there is no gap between two leases"
  [lease1 lease2]
  (active? (:start-date lease1) (end-date lease1) lease2))

(defn union
  "Merge if leases overlap, otherwise return as-is. Metadata is taken from lease2"
  [lease1 lease2]
  (if (every? true? ((juxt same-mac? same-ip? no-gap?) lease1 lease2))
    (let [lease (if (t/after? (end-date lease1) (end-date lease2)) lease1 lease2)
          earliest (t/earliest (:start-date lease1) (:start-date lease2))
          latest (t/latest (end-date lease1) (end-date lease2))
          duration (interval-seconds earliest latest)]
      [(assoc lease :start-date earliest :duration duration)])
      [lease1 lease2]))

(defn append
  "Replace head of the collection with the items returned by (f head val)"
  [f coll val]
  (if (seq coll)
    (into (pop coll) (f (peek coll) val))
    (conj coll val)))

(defn fused
  "Aggregate overlapping leases"
  [sorted-coll]
  (if (seq sorted-coll)
    (reduce (partial append union) (conj (empty sorted-coll) (first sorted-coll)) (rest sorted-coll))
    sorted-coll))
