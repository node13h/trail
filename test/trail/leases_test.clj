(ns trail.leases-test
  (:require [midje.sweet :refer :all]
            [trail.leases :as tl]
            [clj-time.core :as t :refer [date-time]]
            [trail.fixtures :refer [ip]]))

(def a-lease
  {:ip (ip "192.168.0.1")
   :mac "aa:aa:aa:aa:aa:aa"
   :start-date (date-time 2000 1 1 0 0 0)
   :duration 12345
   :data {:key "val"}})

(def a-zero-duration-lease
  {:ip (ip "192.168.0.1")
   :mac "aa:aa:aa:aa:aa:aa"
   :start-date (date-time 2000 1 1 0 0 0)
   :duration 0
   :data {:key "val"}})

(def b-lease
  {:ip (ip "192.168.0.2")
   :mac "bb:bb:bb:bb:bb:bb"
   :start-date (date-time 2001 2 3 4 5 6)
   :duration 100
   :data {:key "val"}})

(def c1-lease
  {:ip (ip "192.168.0.3")
   :mac "cc:cc:cc:cc:cc:cc"
   :start-date (date-time 1999 1 1 0 0 0)
   :duration 50
   :data {:key "val"}})

(def c2-lease
  {:ip (ip "192.168.0.3")
   :mac "cc:cc:cc:cc:cc:cc"
   :start-date (date-time 2000 1 1 0 0 0)
   :duration 50
   :data {:key "val"}})

(def c3-lease
  {:ip (ip "192.168.0.3")
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
       (let [lease {:ip (ip "192.168.0.1")
                    :mac "aa:aa:aa:aa:aa:aa"
                    :start-date (date-time 2000 1 1 0 0 0)
                    :duration 60
                    :data {:key "val"}}
             new-start-date (date-time 2000 1 1 0 0 15)
             adjusted-lease {:ip (ip "192.168.0.1")
                             :mac "aa:aa:aa:aa:aa:aa"
                             :start-date (date-time 2000 1 1 0 0 15)
                             :duration 45
                             :data {:key "val"}}]
         (fact "offsetting works correctly"
               (tl/adjust-start-date lease new-start-date) => adjusted-lease)))

(facts "about `truncated`"
       (let [lease {:ip (ip "192.168.0.1")
                    :mac "aa:aa:aa:aa:aa:aa"
                    :start-date (date-time 2000 1 1 0 0 0)
                    :duration 100
                    :data {:key "val"}}
             truncated-lease {:ip (ip "192.168.0.1")
                              :mac "aa:aa:aa:aa:aa:aa"
                              :start-date (date-time 2000 1 1 0 0 0)
                              :duration 60
                              :data {:key "val"}}]
         (fact "same lease is returned if at-date is higher than lease end-date"
               (tl/truncated lease (date-time 2001 1 1 0 2 0)) => lease)
         (fact "lease is truncated"
               (tl/truncated lease (date-time 2000 1 1 0 1 0)) => truncated-lease)))

(facts "about `same-lease?`"
       (let [lease1 {:ip (ip "192.168.0.1")
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 100
                     :data {:key "val"}}
             lease2 {:ip (ip "192.168.0.1")
                     :mac "bb:bb:bb:bb:bb:bb"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 100
                     :data {:key "val"}}
             lease3 {:ip (ip "192.168.0.1")
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 500
                     :data {:key "val"}}
             lease4 {:ip (ip "192.168.0.2")
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 100
                     :data {:key "val"}}
             lease5 {:ip (ip "192.168.0.1")
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

(facts "about `same-ip?`"
       (let [lease1 {:ip (ip "192.168.0.1")
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 100
                     :data {:key "val"}}
             lease2 {:ip (ip "192.168.0.1")
                     :mac "bb:bb:bb:bb:bb:bb"
                     :start-date (date-time 2000 1 1 0 1 0)
                     :duration 99
                     :data {:key2 "val2"}}
             lease3 {:ip (ip "192.168.0.2")
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 100
                     :data {:key "val"}}
             lease4 {:ip (ip "192.168.000.2")
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 100
                     :data {:key "val"}}
             lease5 {:ip (ip "192.168.0.02")  ;; these tests are probably no longer necessary
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 100
                     :data {:key "val"}}
             lease6 {:ip (ip "::")
                     :mac "cc:cc:cc:cc:cc:cc"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 100
                     :data {:key "val"}}
             lease7 {:ip (ip "0:0:0:0:0:0:0:0")
                     :mac "cc:cc:cc:cc:cc:cc"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 100
                     :data {:key "val"}}
             lease8 {:ip (ip "0:0:0:0:0:0:0:1")
                     :mac "cc:cc:cc:cc:cc:cc"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 100
                     :data {:key "val"}}]
         (fact "same IP addresses return true"
               (tl/same-ip? lease1 lease2) => truthy)
         (fact "different IP addresses return false"
               (tl/same-ip? lease1 lease3) => falsey
               (tl/same-ip? lease6 lease8) => falsey)
         (fact "different formats compare correctly"
               (tl/same-ip? lease3 lease4) => truthy
               (tl/same-ip? lease3 lease5) => truthy
               (tl/same-ip? lease6 lease7) => truthy)))

(facts "about `same-mac?`"
       (let [lease1 {:ip (ip "192.168.0.1")
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 100
                     :data {:key "val"}}
             lease2 {:ip (ip "192.168.0.2")
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 1 0)
                     :duration 99
                     :data {:key2 "val2"}}
             lease3 {:ip (ip "192.168.0.1")
                     :mac "bb:bb:bb:bb:bb:bb"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 100
                     :data {:key "val"}}]
         (fact "same MAC addresses return true"
               (tl/same-mac? lease1 lease2) => truthy)
         (fact "different MAC addresses return false"
               (tl/same-mac? lease1 lease3) => falsey)))

(facts "about `active?`"
       (let [lease {:ip (ip "192.168.0.1")
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 1 0)
                     :duration 60
                     :data {:key "val"}}]
         (fact "within range returns true"
               (tl/active? (date-time 2000 1 1 0 0 0) (date-time 2000 1 1 0 2 0) lease) => truthy)
         (fact "outside range returns false"
               (tl/active? (date-time 2000 1 1 0 0 0) (date-time 2000 1 1 0 0 59) lease) => falsey)
         (fact "ending at range start returns true"
               (tl/active? (date-time 2000 1 1 0 2 0) (date-time 2000 1 1 0 3 0) lease) => truthy)
         (fact "starting at range end returns true"
               (tl/active? (date-time 2000 1 1 0 0 0) (date-time 2000 1 1 0 1 0) lease) => truthy)))

(facts "about `no-gap?`"
       (let [lease1 {:ip (ip "192.168.0.1")
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 1 0)
                     :duration 60
                     :data {:key "val"}}
             lease2 {:ip (ip "192.168.0.2")
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 2 0)
                     :duration 60
                     :data {:key "val"}}
             lease3 {:ip (ip "192.168.0.1")
                     :mac "bb:bb:bb:bb:bb:bb"
                     :start-date (date-time 2000 1 1 0 2 30)
                     :duration 60
                     :data {:key "val"}}
             lease4 {:ip (ip "192.168.0.2")
                     :mac "bb:bb:bb:bb:bb:bb"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 240
                     :data {:key "val"}}]
         (fact "adjacent return true"
               (tl/no-gap? lease1 lease2) => truthy)
         (fact "reverse adjacent return true"
               (tl/no-gap? lease2 lease1) => truthy)
         (fact "overlapping return true"
               (tl/no-gap? lease2 lease3) => truthy)
         (fact "reverse overlapping return true"
               (tl/no-gap? lease3 lease2) => truthy)
         (fact "disjoint return false"
               (tl/no-gap? lease1 lease3) => falsey)
         (fact "reverse disjoint return false"
               (tl/no-gap? lease3 lease1) => falsey)
         (fact "contained returns true"
               (tl/no-gap? lease1 lease4) => truthy)
         (fact "reverse contained returns true"
               (tl/no-gap? lease4 lease1) => truthy)))

(facts "about `union`"
       (let [lease1 {:ip (ip "192.168.0.1")
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 1 0)
                     :duration 60
                     :data {:key1 "val1"}}
             lease2 {:ip (ip "192.168.0.1")
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 2 0)
                     :duration 60
                     :data {:key2 "val2"}}
             lease3 {:ip (ip "192.168.0.1")
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 2 30)
                     :duration 60
                     :data {:key3 "val3"}}
             lease4 {:ip (ip "192.168.0.1")
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 0 0)
                     :duration 240
                     :data {:key4 "val4"}}
             lease5 {:ip (ip "192.168.0.2")
                     :mac "aa:aa:aa:aa:aa:aa"
                     :start-date (date-time 2000 1 1 0 2 0)
                     :duration 60
                     :data {:key5 "val5"}}
             lease6 {:ip (ip "192.168.0.1")
                     :mac "bb:bb:bb:bb:bb:bb"
                     :start-date (date-time 2000 1 1 0 2 0)
                     :duration 60
                     :data {:key6 "val6"}}]
         (fact "adjacent are merged with metadata from a more recent lease"
               (tl/union lease1 lease2) => [{:ip (ip "192.168.0.1")
                                             :mac "aa:aa:aa:aa:aa:aa"
                                             :start-date (date-time 2000 1 1 0 1 0)
                                             :duration 120
                                             :data {:key2 "val2"}}])
         (fact "reverse adjacent are merged with metadata a more recent lease"
               (tl/union lease2 lease1) => [{:ip (ip "192.168.0.1")
                                             :mac "aa:aa:aa:aa:aa:aa"
                                             :start-date (date-time 2000 1 1 0 1 0)
                                             :duration 120
                                             :data {:key2 "val2"}}])
         (fact "overlapping are merged with metadata from a more recent lease"
               (tl/union lease2 lease3) => [{:ip (ip "192.168.0.1")
                                             :mac "aa:aa:aa:aa:aa:aa"
                                             :start-date (date-time 2000 1 1 0 2 0)
                                             :duration 90
                                             :data {:key3 "val3"}}])
         (fact "reverse overlapping are merged with metadata from a more recent lease"
               (tl/union lease3 lease2) => [{:ip (ip "192.168.0.1")
                                             :mac "aa:aa:aa:aa:aa:aa"
                                             :start-date (date-time 2000 1 1 0 2 0)
                                             :duration 90
                                             :data {:key3 "val3"}}])
         (fact "disjoint are not merged"
               (tl/union lease1 lease3) => [lease1 lease3])
         (fact "reverse disjoint are not merged"
               (tl/union lease3 lease1) => [lease3 lease1])
         (fact "contained are merged with metadata from a more recent lease"
               (tl/union lease1 lease4) => [{:ip (ip "192.168.0.1")
                                             :mac "aa:aa:aa:aa:aa:aa"
                                             :start-date (date-time 2000 1 1 0 0 0)
                                             :duration 240
                                             :data {:key4 "val4"}}])
         (fact "reverse contained are merged with metadata from a more recent lease"
               (tl/union lease4 lease1) => [{:ip (ip "192.168.0.1")
                                             :mac "aa:aa:aa:aa:aa:aa"
                                             :start-date (date-time 2000 1 1 0 0 0)
                                             :duration 240
                                             :data {:key4 "val4"}}])
         (fact "adjacent with different IP addresses are not merged"
               (tl/union lease1 lease5) => [{:ip (ip "192.168.0.1")
                                             :mac "aa:aa:aa:aa:aa:aa"
                                             :start-date (date-time 2000 1 1 0 1 0)
                                             :duration 60
                                             :data {:key1 "val1"}}
                                            {:ip (ip "192.168.0.2")
                                             :mac "aa:aa:aa:aa:aa:aa"
                                             :start-date (date-time 2000 1 1 0 2 0)
                                             :duration 60
                                             :data {:key5 "val5"}}])
         (fact "adjacent with different MAC addresses are not merged"
               (tl/union lease1 lease6) => [{:ip (ip "192.168.0.1")
                                             :mac "aa:aa:aa:aa:aa:aa"
                                             :start-date (date-time 2000 1 1 0 1 0)
                                             :duration 60
                                             :data {:key1 "val1"}}
                                            {:ip (ip "192.168.0.1")
                                             :mac "bb:bb:bb:bb:bb:bb"
                                             :start-date (date-time 2000 1 1 0 2 0)
                                             :duration 60
                                             :data {:key6 "val6"}}])))

(facts "about `append`"
       (fact "can append to an empty vector"
             (tl/append #([(+ %1 %2)]) [] 1) => [1])
       (fact "can append to an empty list"
             (tl/append #([(+ %1 %2)]) (list) 1) => (list 1))
       (fact "can append to an existing vector"
             (tl/append #(if (odd? %1) [(+ %1 %2)] [%2 %1]) [1 2 3] 1) => [1 2 4]
             (tl/append #(if (odd? %1) [(+ %1 %2)] [%2 %1]) [1 2 4] 1) => [1 2 1 4])
       (fact "can append to an existing list"
             (tl/append #(if (odd? %1) [(+ %1 %2)] [%2 %1]) (list 1 2 3) 1) => (list 2 2 3)
             (tl/append #(if (odd? %1) [(+ %1 %2)] [%2 %1]) (list 2 3 4) 1) => (list 2 1 3 4)))

(facts "about `fused`"
       (let [coll [{:ip (ip "192.168.0.1")
                    :mac "aa:aa:aa:aa:aa:aa"
                    :start-date (date-time 2000 1 1 0 0 0)
                    :duration 120
                    :data {:key1 "val1"}}
                   {:ip (ip "192.168.0.1")
                    :mac "aa:aa:aa:aa:aa:aa"
                    :start-date (date-time 2000 1 1 0 1 0)
                    :duration 60
                    :data {:key2 "val2"}}
                   {:ip (ip "192.168.0.1")
                    :mac "aa:aa:aa:aa:aa:aa"
                    :start-date (date-time 2000 1 1 0 1 30)
                    :duration 60
                    :data {:key3 "val3"}}
                   {:ip (ip "192.168.0.1")
                    :mac "aa:aa:aa:aa:aa:aa"
                    :start-date (date-time 2000 1 1 0 2 59)
                    :duration 60
                    :data {:key4 "val4"}}
                   {:ip (ip "192.168.0.1")
                    :mac "aa:aa:aa:aa:aa:aa"
                    :start-date (date-time 2000 1 1 0 3 00)
                    :duration 100
                    :data {:key5 "val5"}}
                   {:ip (ip "192.168.0.1")
                    :mac "aa:aa:aa:aa:aa:aa"
                    :start-date (date-time 2000 1 1 0 5 00)
                    :duration 60
                    :data {:key6 "val6"}}
                   {:ip (ip "192.168.0.2")
                    :mac "bb:bb:bb:bb:bb:bb"
                    :start-date (date-time 2000 1 1 0 0 0)
                    :duration 90
                    :data {:key1 "val1"}}
                   {:ip (ip "192.168.0.2")
                    :mac "bb:bb:bb:bb:bb:bb"
                    :start-date (date-time 2000 1 1 0 0 0)
                    :duration 100
                    :data {:key2 "val2"}}
                   {:ip (ip "192.168.0.3")
                    :mac "aa:aa:aa:aa:aa:aa"
                    :start-date (date-time 2000 1 1 0 10 0)
                    :duration 60
                    :data {:key1 "val1"}}]]
         (fact "overlapping leases are fused together"
               (tl/fused coll) => [{:ip (ip "192.168.0.1")
                                    :mac "aa:aa:aa:aa:aa:aa"
                                    :start-date (date-time 2000 1 1 0 0 0)
                                    :duration 150
                                    :data {:key3 "val3"}}
                                   {:ip (ip "192.168.0.1")
                                    :mac "aa:aa:aa:aa:aa:aa"
                                    :start-date (date-time 2000 1 1 0 2 59)
                                    :duration 101
                                    :data {:key5 "val5"}}
                                   {:ip (ip "192.168.0.1")
                                    :mac "aa:aa:aa:aa:aa:aa"
                                    :start-date (date-time 2000 1 1 0 5 00)
                                    :duration 60
                                    :data {:key6 "val6"}}
                                   {:ip (ip "192.168.0.2")
                                    :mac "bb:bb:bb:bb:bb:bb"
                                    :start-date (date-time 2000 1 1 0 0 0)
                                    :duration 100
                                    :data {:key2 "val2"}}
                                   {:ip (ip "192.168.0.3")
                                    :mac "aa:aa:aa:aa:aa:aa"
                                    :start-date (date-time 2000 1 1 0 10 0)
                                    :duration 60
                                    :data {:key1 "val1"}}])))
