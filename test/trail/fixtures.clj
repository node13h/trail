(ns trail.fixtures
  (:require [clj-time.core :as t]))

(defn leasegen
  [n]
  (for [x (range n)]
    {:ip "192.168.0.4"
     :mac "cc:cc:cc:cc:cc:cc"
     :start-date (t/plus (t/date-time 2000 1 1 0 0 0) (t/seconds x))
     :duration 100
     :data {}}))

(def before-all (t/date-time 1999 1 1))
(def after-all (t/date-time 2001 1 1))
(def distant-future (t/date-time 2002 1 1))
(def start-a1 (t/date-time 2000 1 1 0 0 0))
(def during-a1 (t/date-time 2000 1 1 0 0 50))
(def during-a1-and-a2 (t/date-time 2000 1 1 0 1 0))
(def before-a1 (t/date-time 1999 12 31 23 59 59))
(def just-after-a1 (t/date-time 2000 1 1 0 1 40))
(def after-a1 (t/date-time 2000 1 1 0 1 41))
(def just-after-a2 (t/date-time 2000 1 1 0 2 30))
(def start-b2 (t/date-time 2000 1 1 0 1 40))
(def during-b1 (t/date-time 2000 1 1 0 0 50))
(def after-b1 (t/date-time 2000 1 1 0 1 41))

;; A lease
(def a1
  {:ip "192.168.0.2"
   :mac "aa:aa:aa:aa:aa:aa"
   :start-date start-a1
   :duration 100
   :data {}})

;; A lease truncated to during-a1-and-a2
(def a1-truncated
  {:ip "192.168.0.2"
   :mac "aa:aa:aa:aa:aa:aa"
   :start-date start-a1
   :duration 60
   :data {}})

;; Overlapping continuation of the a1
(def a2
  {:ip "192.168.0.2"
   :mac "aa:aa:aa:aa:aa:aa"
   :start-date during-a1
   :duration 100
   :data {}})

;; a2 truncated to during-a1-and-a2
(def a2-truncated
  {:ip "192.168.0.2"
   :mac "aa:aa:aa:aa:aa:aa"
   :start-date during-a1
   :duration 10
   :data {}})

;; Adjacent continuation of the a2
(def a3
  {:ip "192.168.0.2"
   :mac "aa:aa:aa:aa:aa:aa"
   :start-date just-after-a2
   :duration 100
   :data {}})

;; a1, a2 and a3 combined
(def a-aggregated
  {:ip "192.168.0.2"
   :mac "aa:aa:aa:aa:aa:aa"
   :start-date start-a1
   :duration 250
   :data {}})

;; Same start date as a1, but different IP
(def b1
  {:ip "192.168.0.3"
   :mac "bb:bb:bb:bb:bb:bb"
   :start-date start-a1
   :duration 100
   :data {}})

;; Adjacent continuation of b1
(def b2
  {:ip "192.168.0.3"
   :mac "bb:bb:bb:bb:bb:bb"
   :start-date start-b2
   :duration 100
   :data {}})

;; Overlaps with c1
(def b1-truncated
  {:ip "192.168.0.3"
   :mac "bb:bb:bb:bb:bb:bb"
   :start-date start-a1
   :duration 50
   :data {}})

;; b1 and b2 combined
(def b-aggregated
  {:ip "192.168.0.3"
   :mac "bb:bb:bb:bb:bb:bb"
   :start-date start-a1
   :duration 200
   :data {}})

;; Overlaps with b1. See b1-truncated
(def c1
  {:ip "192.168.0.3"
   :mac "cc:cc:cc:cc:cc:cc"
   :start-date during-b1
   :duration 100
   :data {}})

;; Doesn't overlap with with b1. See b1-truncated
(def c2
  {:ip "192.168.0.3"
   :mac "cc:cc:cc:cc:cc:cc"
   :start-date after-b1
   :duration 100
   :data {}})

;; Updated version of the c1, MAC updates are also allowed
(def c1-updated
  {:ip "192.168.0.3"
   :mac "000000000000"
   :start-date during-b1
   :duration 500
   :data {:a 1}})
