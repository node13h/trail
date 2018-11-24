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

(defn seconds-between
  "Calculate interval between two leases in seconds"
  [lease1 lease2]
  (interval-seconds (:start-date lease1) (:start-date lease2)))

(defn duration-span
  "Return combined duration in seconds"
  [lease1 lease2]
  (+ (seconds-between lease1 lease2) (:duration lease2)))

(defn sorted
  "Return a sequence of leases sorted by IP and start-date"
  [coll]
  (sort-by (juxt :ip start-epoch) coll))

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
   (= (:ip lease1) (:ip lease2))))
