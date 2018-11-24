(ns trail.leases-test
  (:require [midje.sweet :refer :all]
            [trail.leases :as tl]
            [clj-time.core :as t :refer [date-time]]))

(def a-lease
  {:ip "192.168.0.1"
   :mac "aa:aa:aa:aa:aa:aa"
   :start-date (date-time 2000 1 1 0 0 0)
   :duration 12345
   :data {:key "val"}})

(def a-zero-duration-lease
  {:ip "192.168.0.1"
   :mac "aa:aa:aa:aa:aa:aa"
   :start-date (date-time 2000 1 1 0 0 0)
   :duration 0
   :data {:key "val"}})

(def b-lease
  {:ip "192.168.0.2"
   :mac "bb:bb:bb:bb:bb:bb"
   :start-date (date-time 2001 2 3 4 5 6)
   :duration 100
   :data {:key "val"}})

(def c1-lease
  {:ip "192.168.0.3"
   :mac "cc:cc:cc:cc:cc:cc"
   :start-date (date-time 1999 1 1 0 0 0)
   :duration 50
   :data {:key "val"}})

(def c2-lease
  {:ip "192.168.0.3"
   :mac "cc:cc:cc:cc:cc:cc"
   :start-date (date-time 2000 1 1 0 0 0)
   :duration 50
   :data {:key "val"}})

(def c3-lease
  {:ip "192.168.0.3"
   :mac "cc:cc:cc:cc:cc:cc"
   :start-date (date-time 2001 1 1 0 0 0)
   :duration 50
   :data {:key "val"}})


(facts "about `end-date`"
       (fact "calculates end date"
             (tl/end-date a-lease) => (date-time 2000 1 1 3 25 45))
       (fact "zero duration is handled properly"
             (tl/end-date a-zero-duration-lease) => (date-time 2000 1 1 0 0 0)))

(facts "about `start-epoch`"
       (fact "returns start-date in UNIX epoch"
             (tl/start-epoch a-lease) => 946684800))

(facts "about `interval-seconds`"
       (fact "returns number of seconds between two dates"
             (tl/interval-seconds (date-time 2000 1 1 0 0 0) (date-time 2000 1 1 4 15 28)) => 15328))

(facts "about `duration-span`"
       (fact "returns number of seconds between lease1 start and lease2 end"
             (tl/duration-span a-lease b-lease) => 34488406))

(facts "about `sorted`"
       (fact "empty input returns empty list"
             (tl/sorted []) => (list))
       (fact "single-element collection as the single-element list"
             (tl/sorted [a-lease]) => (list a-lease))
       (fact "collection is sorted by IP first"
             (tl/sorted [c1-lease a-lease]) => (list a-lease c1-lease))
       (fact "mixed collection is sorted by IP, then by start-date"
             (tl/sorted [c3-lease a-lease b-lease c1-lease c2-lease]) => (list a-lease b-lease c1-lease c2-lease c3-lease)))

(facts "about `adjust-start-date`"
       (let [lease {:ip "192.168.0.1"
                    :mac "aa:aa:aa:aa:aa:aa"
                    :start-date (date-time 2000 1 1 0 0 0)
                    :duration 60
                    :data {:key "val"}}
             new-start-date (date-time 2000 1 1 0 0 15)
             adjusted-lease {:ip "192.168.0.1"
                             :mac "aa:aa:aa:aa:aa:aa"
                             :start-date (date-time 2000 1 1 0 0 15)
                             :duration 45
                             :data {:key "val"}}]
         (fact "offsetting works correctly"
               (tl/adjust-start-date lease new-start-date) => adjusted-lease)))

(facts "about `truncated`"
       (let [lease {:ip "192.168.0.1"
                    :mac "aa:aa:aa:aa:aa:aa"
                    :start-date (date-time 2000 1 1 0 0 0)
                    :duration 100
                    :data {:key "val"}}
             truncated-lease {:ip "192.168.0.1"
                              :mac "aa:aa:aa:aa:aa:aa"
                              :start-date (date-time 2000 1 1 0 0 0)
                              :duration 60
                              :data {:key "val"}}]
         (fact "same lease is returned if at-date is higher than lease end-date"
               (tl/truncated lease (date-time 2001 1 1 0 2 0)) => lease)
         (fact "lease is truncated"
               (tl/truncated lease (date-time 2000 1 1 0 1 0)) => truncated-lease)))

(facts "about `same-lease?`"
       (let [lease1 {:ip "192.168.0.1"
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 100
                     :data {:key "val"}}
             lease2 {:ip "192.168.0.1"
                     :mac "bb:bb:bb:bb:bb:bb"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 100
                     :data {:key "val"}}
             lease3 {:ip "192.168.0.1"
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 500
                     :data {:key "val"}}
             lease4 {:ip "192.168.0.2"
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 100
                     :data {:key "val"}}
             lease5 {:ip "192.168.0.1"
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 0 1)
                     :duration 100
                     :data {:key "val"}}]
         (fact "different MAC addresses does not matter"
               (tl/same-lease? lease1 lease2) => truthy)
         (fact "different durations do not matter"
               (tl/same-lease? lease1 lease3) => truthy)
         (fact "different IP addresses do matter"
               (tl/same-lease? lease1 lease4) => falsey)
         (fact "different start dates do matter"
               (tl/same-lease? lease1 lease5) => falsey)))
