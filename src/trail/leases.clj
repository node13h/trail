(ns trail.leases
  (:require [clj-time.core :as t]
            [clj-time.coerce :as tc]))

(defn end-date
  "Return end date for the lease"
  [lease]
  (t/plus (:start-date lease) (t/seconds (:duration lease))))

(def start-date :start-date)

(defn start-epoch
  "Return start-date in UNIX epoch"
  [lease]
  (tc/to-epoch (:start-date lease)))

(def duration :duration)

(def ip :ip)

(def mac :mac)

(defn new-duration
  "Set new duration for the lease"
  [lease duration]
  (assoc lease :duration duration))

(defn new-start-date
  "Set new start-date for the lease"
  [lease start-date]
  (assoc lease :start-date start-date))

(defn this-lease?
  "Return true if ip and start-date match the lease"
  [lease ip start-date]
  (and (= (:ip lease) ip)
       (t/equal? (:start-date lease) start-date)))

(defn this-mac?
  "Return true if the mac matches the lease"
  [mac lease]
  (= (:mac lease) mac))

(defn this-ip?
  "Return ture if the ip matches the lease"
  [ip lease]
  (= (:ip lease) ip))

(defn same-ip?
  "Return true if both leases have the same IP address"
  [lease1 lease2]
  (= (:ip lease1) (:ip lease2)))

(defn same-mac?
  "Return true if both leases have the smae MAC address"
  [lease1 lease2]
  (= (:mac lease1) (:mac lease2)))

(defn interval-seconds
  "Calculate interval in seconds between two dates"
  [dt1 dt2]
  (t/in-seconds (t/interval dt1 dt2)))

(defn new-end-date
  "Set new duration for the lease based on the end-date"
  [lease end-date]
  (new-duration lease (interval-seconds (:start-date lease) end-date)))

(defn seconds-between
  "Calculate interval between two leases in seconds"
  [lease1 lease2]
  (interval-seconds (start-date lease1) (start-date lease2)))

(defn continuous?
  "Return true if leases overlap or adjacent"
  [lease1 lease2]
  (>= (duration lease1) (seconds-between lease1 lease2)))

(defn combined-duration
  "Return combined duration in seconds"
  [lease1 lease2]
  (+ (seconds-between lease1 lease2) (duration lease2)))

(def duration-span combined-duration)

(defn required-action
  "Calculate the merge action for two leases"
  [lease1 lease2]
  (if (same-ip? lease1 lease2)
    (if (continuous? lease1 lease2)
      (if (same-mac? lease1 lease2) :combine :truncate))))

(defn consolidate
  "Join two leases using best strategy possible"
  [lease1 lease2]
  (case (required-action lease1 lease2)
    :combine [(new-duration lease1 (combined-duration lease1 lease2))]
    :truncate [(new-duration lease1 (seconds-between lease1 lease2)) lease2]
    [lease1 lease2]))

(defn add
  "Insert a new lease into the collection attempting to merge with the head. Return sequence"
  [coll lease]
  (if (seq coll)
    (into (pop coll) (consolidate (peek coll) lease))
    (conj coll lease)))

(defn aggregates
  "Aggregate continuous leases"
  [coll]
  (if (seq coll)
    (reduce add (conj (empty coll) (first coll)) (rest coll))
    coll))

(defn before?
  "Return true if the lease was active before the specified date"
  [date lease]
  (t/before? (end-date lease) date))

(defn after?
  "Return true if the lease was active after the specified date"
  [date lease]
  (t/after? (start-date lease) date))

(def not-before? (complement before?))

(def not-after? (complement after?))

(defn active?
  "Return true if the lease was active during the specified date range"
  [from-date to-date lease]
  (and (not-before? from-date lease) (not-after? to-date lease)))

(defn sorted
  "Return a sequence of leases sorted by IP and start-date"
  [coll]
  (sort-by (juxt :ip start-epoch) coll))

(defn filter-ip
  "Return a sequence of leases for specified IP"
  [coll ip]
  (filter (partial this-ip? ip) coll))

(defn filter-mac
  "Return a sequence of leases for specified MAC"
  [coll mac]
  (filter (partial this-mac? mac) coll))

(defn filter-from
  "Return a sequence of leases active during or after the specified date"
  [coll date]
  (filter (partial not-before? date) coll))

(defn filter-to
  "Return a sequence of leases active during or before the specified date"
  [coll date]
  (filter (partial not-after? date) coll))

(defn remove-before
  "Return a sequence of leases active during or after the specified date"
  [coll date]
  (remove (partial before? date) coll))

(defn release-matching
  [coll ip end-date]
  (map #(if (and (this-ip? ip %1) (active? end-date end-date %1)) (new-end-date %1 end-date) %1) coll))
