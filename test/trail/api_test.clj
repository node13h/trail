(ns trail.api-test
  (:require [midje.sweet :refer :all]
            [clj-time.core :as t]
            [trail.api.core :as tac]))

(def utc-tz (t/time-zone-for-id "UTC"))
(def vilnius-tz (t/time-zone-for-id "Europe/Vilnius"))
(def utc-s "2000-01-01 20:01:15")
(def vilnius-s "2000-01-01 22:01:15")
(def utc-dt (t/from-time-zone (t/date-time 2000 01 01 20 1 15) utc-tz))
(def vilnius-dt (t/to-time-zone utc-dt vilnius-tz))
(def a1
  {:ip "192.168.0.2"
   :mac "aa:aa:aa:aa:aa:aa"
   :start-date utc-dt
   :duration 100
   :data {}})

;; Equivalent to a1, but with Europe/Vilnius time zone set for dt
(def vilnius-a1
  {:ip "192.168.0.2"
   :mac "aa:aa:aa:aa:aa:aa"
   :start-date vilnius-dt
   :duration 100
   :data {}})

(def b1
  {:ip "192.168.0.3"
   :mac "bb:bb:bb:bb:bb:bb"
   :start-date utc-dt
   :duration 100
   :data {}})

;; Equivalent to b1, but with Europe/Vilnius time zone set for dt
(def vilnius-b1
  {:ip "192.168.0.3"
   :mac "bb:bb:bb:bb:bb:bb"
   :start-date vilnius-dt
   :duration 100
   :data {}})

(def formatted-utc-a1
  {:ip "192.168.0.2"
   :mac "aa:aa:aa:aa:aa:aa"
   :start-date utc-s
   :duration 100
   :data {}})

(def formatted-vilnius-a1
  {:ip "192.168.0.2"
   :mac "aa:aa:aa:aa:aa:aa"
   :start-date vilnius-s
   :duration 100
   :data {}})

(def formatted-utc-b1
  {:ip "192.168.0.3"
   :mac "bb:bb:bb:bb:bb:bb"
   :start-date utc-s
   :duration 100
   :data {}})

(def formatted-vilnius-b1
  {:ip "192.168.0.3"
   :mac "bb:bb:bb:bb:bb:bb"
   :start-date vilnius-s
   :duration 100
   :data {}})

;; TODO More generic tests
(facts "about test samples"
       (fact "test samples for different time zones are equal"
             (t/equal? utc-dt vilnius-dt) => true))

(facts "about `parsed-dt`"
       (fact "can parse string to dt"
             (tac/parsed-dt utc-s "UTC") => utc-dt)
       (fact "can parse string using custom time zone"
             (tac/parsed-dt vilnius-s "Europe/Vilnius") => vilnius-dt))

(facts "about `formatted-dt`"
       (fact "can format dt to string"
             (tac/formatted-dt utc-dt "UTC") => utc-s)
       (fact "can format dt using custom time zone"
             (tac/formatted-dt utc-dt "Europe/Vilnius") => vilnius-s))

(facts "about `formatted-map`"
       (fact "can format lease"
             (tac/formatted-map a1 "UTC") => formatted-utc-a1)
       (fact "can format lease using custom time zone"
             (tac/formatted-map a1 "Europe/Vilnius") => formatted-vilnius-a1))

(facts "about `formatted-maps`"
       (fact "can format multiple leases"
             (tac/formatted-maps [a1 b1] "UTC") => [formatted-utc-a1 formatted-utc-b1])
       (fact "can format multiple leases using custom time zone"
             (tac/formatted-maps [a1 b1] "Europe/Vilnius") => [formatted-vilnius-a1 formatted-vilnius-b1]))

(facts "about `parsed-map`"
       (fact "can parse lease"
             (tac/parsed-map formatted-utc-a1 "UTC") => a1)
       (fact "can parse lease using custom time zone"
             (tac/parsed-map formatted-vilnius-a1 "Europe/Vilnius") => vilnius-a1))

(facts "about `parsed-maps`"
       (fact "can parse multiple leases"
             (tac/parsed-maps [formatted-utc-a1 formatted-utc-b1] "UTC") => [a1 b1])
       (fact "can parse multiple leases using custom time zone"
             (tac/parsed-maps [formatted-vilnius-a1 formatted-vilnius-b1] "Europe/Vilnius") => [vilnius-a1 vilnius-b1]))
