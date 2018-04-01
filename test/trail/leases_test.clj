(ns trail.leases-test
  (:require [midje.sweet :refer :all]
            [trail.fixtures :refer :all]
            [trail.leases :as tl]
            [clj-time.core :as t]))

(facts "about `this-lease?`"
       (fact "matching returns true"
             (tl/this-lease? a1 (:ip a1) (:start-date a1)))
       (fact "non-matching IP returns false"
             (tl/this-lease? a1 "0.0.0.0" (:start-date a1)) => false)
       (fact "non-matching date returns false"
             (tl/this-lease? a1 (:ip a1) (t/date-time 1999)) => false)
       (fact "can match different time zones"
             (tl/this-lease? a1 (:ip a1) (t/to-time-zone (:start-date a1) (t/time-zone-for-id "Europe/Vilnius"))) => true))

(facts "about `add`"
       (fact "can add to empty coll"
             (tl/add [] a1) => [a1])
       (fact "can add to non-empty coll"
             (tl/add [a1] b1) => [a1 b1])
       (fact "mergeable will merge"
             (tl/add [b1] b2) => [b-aggregated]))

(facts "about `aggregates` lists"
       (fact "empty input will produce empty output"
             (tl/aggregates (list)) => (list))
       (fact "can add single"
             (tl/aggregates (list a1)) => (list a1))
       (fact "can add multiple and the order is reversed"
             (tl/aggregates (list a1 b1)) => (list b1 a1))
       (fact "adding multiple mergeable will merge to single"
             (tl/aggregates (list a1 a2 a3)) => (list a-aggregated))
       (fact "will not merge when there is a gap"
             (tl/aggregates (list a1 a3)) => (list a3 a1))
       (fact "can add mixed"
             (tl/aggregates (list a1 a2 a3 b1)) => (list b1 a-aggregated))
       (fact "duplicates will merge together"
             (tl/aggregates (list a1 a1)) => (list a1))
       (fact "adding newer overlapping with different MAC will truncate existing"
             (tl/aggregates (list b1 c1)) => (list c1 b1-truncated))
       (fact "adding newer non-overlapping with different MAC will not truncate existing"
             (tl/aggregates (list b1 c2)) => (list c2 b1)))

(facts "about `aggregates` vectors"
       (fact "empty input will produce empty output"
             (tl/aggregates []) => [])
       (fact "can add single"
             (tl/aggregates [a1]) => [a1])
       (fact "can add multiple and the order is preserved"
             (tl/aggregates [a1 b1]) => [a1 b1])
       (fact "adding multiple mergeable will merge to single"
             (tl/aggregates [a1 a2 a3]) => [a-aggregated])
       (fact "will not merge when there is a gap"
             (tl/aggregates [a1 a3]) => [a1 a3])
       (fact "can add mixed"
             (tl/aggregates [a1 a2 a3 b1]) => [a-aggregated b1])
       (fact "duplicates will merge together"
             (tl/aggregates [a1 a1]) => [a1])
       (fact "adding newer overlapping with different MAC will truncate existing"
             (tl/aggregates [b1 c1]) => [b1-truncated c1])
       (fact "adding newer non-overlapping with different MAC will not truncate existing"
             (tl/aggregates [b1 c2]) => [b1 c2]))

(facts "about `filter-ip`"
       (fact "empty input will produce empty output"
             (tl/filter-ip [] "") => [])
       (fact "can match in single element collection"
             (tl/filter-ip [a1] "192.168.0.2") => [a1])
       (fact "all non-matching will produce empty output"
             (tl/filter-ip [b1 b2] "192.168.0.2") => [])
       (fact "can match multiple"
             (tl/filter-ip [a2 a3 b1 c1] "192.168.0.2") => [a2 a3]))

(facts "about `filter-mac`"
       (fact "empty input will produce empty output"
             (tl/filter-mac [] "") => [])
       (fact "can match in single element collection"
             (tl/filter-mac [a1] "aa:aa:aa:aa:aa:aa") => [a1])
       (fact "all non-matching will produce empty output"
             (tl/filter-mac [b1 b2] "aa:aa:aa:aa:aa:aa") => [])
       (fact "can match multiple"
             (tl/filter-mac [a2 a3 b1 c1] "aa:aa:aa:aa:aa:aa") => [a2 a3]))

(facts "about `filter-from`"
       (fact "empty input will produce empty output"
             (tl/filter-from [] before-all) => [])
       (fact "after will match"
             (tl/filter-from [a1] before-all) => [a1])
       (fact "equal will match"
             (tl/filter-from [a1] start-a1) => [a1])
       (fact "during will match"
             (tl/filter-from [a1] during-a1) => [a1])
       (fact "before will not match"
             (tl/filter-from [a1] after-a1) => [])
       (fact "all before will produce empty output"
             (tl/filter-from [a1 b1] after-all) => [])
       (fact "can match multiple"
             (tl/filter-from [a1 a2 a3] after-a1) => [a2 a3]))

(facts "about `filter-to`"
       (fact "empty input will produce empty output"
             (tl/filter-to [] after-all) => [])
       (fact "before will match"
             (tl/filter-to [a1] after-all) => [a1])
       (fact "equal will match"
             (tl/filter-to [a1] start-a1) => [a1])
       (fact "during will match"
             (tl/filter-to [a1] during-a1) => [a1])
       (fact "after will not match"
             (tl/filter-to [a1] before-a1) => [])
       (fact "all after will produce empty output"
             (tl/filter-to [a1 b1] before-all) => [])
       (fact "can match multiple"
             (tl/filter-to [a1 a2 a3] just-after-a1) => [a1 a2]))

(facts "about `release-matching`"
       (fact "empty input will produce empty output"
             (tl/release-matching [] "0.0.0.0" after-all) => [])
       (fact "matching will be released"
             (tl/release-matching [a1 a2 b1] "192.168.0.2" during-a1-and-a2) => [a1-truncated a2-truncated b1])
       (fact "non-matching IP will have no effect"
             (tl/release-matching [a1 a2 b1] "192.168.0.4" during-a1) => [a1 a2 b1])
       (fact "non-matching date will have no effect"
             (tl/release-matching [a1 a2 b1] "192.168.0.2" after-all) => [a1 a2 b1]))
