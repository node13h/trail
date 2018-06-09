-- :name sorted-selection :? :*
-- :doc Return a sorted list of leases filtered by the optional arguments.
/* :require [clojure.string :refer [join]] */
SELECT id, ip, mac, "start-date", EXTRACT(EPOCH FROM "end-date" - "start-date") as duration, data
FROM leases
/*~
(let [{:keys [ip mac from-date to-date]} params]
  (-> (cond-> []
        (some? ip) (conj "ip = :ip::inet")
        (some? mac) (conj "mac = :mac::macaddr")
        (some? from-date) (conj "\"end-date\" >= :from-date")
        (some? to-date) (conj "\"start-date\" <= :to-date"))
      seq
      (#(when-let [pred %1]
          (->> pred
               (interpose "AND")
               (cons "WHERE")
               (join " "))))))
~*/
ORDER BY "start-date" ASC


-- :name add! :<! :1
-- :doc Add a lease to the store
INSERT INTO leases (ip, mac, "start-date", "end-date", data)
VALUES (:ip::inet, :mac::macaddr, :start-date, :start-date::timestamp with time zone + make_interval(secs => :duration), :data)
ON CONFLICT ON CONSTRAINT "ip-start-date" DO UPDATE
   SET (mac, "end-date", data) = (:mac::macaddr, :start-date::timestamp with time zone + make_interval(secs => :duration), :data)
RETURNING id


-- :name release! :<! :*
-- :doc Truncate the duration of the matching leases
UPDATE leases SET "end-date" = :end-date
WHERE ip = :ip::inet
      AND "end-date" > :end-date
      AND "start-date" <= :end-date
RETURNING id


-- :name trim! :! :n
-- :doc Delete all leases ending before the to-date
DELETE FROM leases
WHERE "end-date" < :to-date


-- :name delete! :! :n
-- :doc Delete lease(s)
DELETE FROM leases
WHERE id IN (:v*:ids)
