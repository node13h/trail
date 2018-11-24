-- :name selection :? :*
-- :doc Return a list of leases filtered by the optional arguments.
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
/*~
(when (true? (:lock params)) "FOR UPDATE")
~*/

-- :name add-or-update-lease! :<! :1
-- :doc Add a lease to the store
INSERT INTO leases (ip, mac, "start-date", "end-date", data)
VALUES (:ip::inet, :mac::macaddr, :start-date, :start-date::timestamp with time zone + make_interval(secs => :duration), :data)
ON CONFLICT ON CONSTRAINT "ip-start-date" DO UPDATE
   SET (mac, "end-date", data) = (:mac::macaddr, :start-date::timestamp with time zone + make_interval(secs => :duration), :data)
RETURNING id

-- :name truncate-lease! :<! :*
-- :doc Truncate the duration of lease
UPDATE leases SET "end-date" = :end-date
WHERE id = :id
RETURNING id


-- :name trim-leases! :! :n
-- :doc Delete all leases ending before the to-date
DELETE FROM leases
WHERE "end-date" < :to-date


-- :name delete! :! :n
-- :doc Delete lease(s)
DELETE FROM leases
WHERE id IN (:v*:ids)


-- :name add-release! :! :n
-- :doc Add release record
INSERT INTO releases (ip, "end-date")
VALUES (:ip::inet, :end-date)
ON CONFLICT ON CONSTRAINT "releases_pkey" DO NOTHING


-- :name get-released :? :1
-- :doc Get release record
SELECT "end-date"
FROM releases
WHERE ip = :ip::inet
  AND "end-date" > :start-date
  AND "end-date" < :start-date::timestamp with time zone + make_interval(secs => :duration)
ORDER BY "end-date"
LIMIT 1

-- :name trim-releases! :! :n
-- :doc Delete all releases ending before the to-date
DELETE FROM releases
WHERE "end-date" < :to-date

-- :name add-renewal! :! :n
-- :doc Add renewal entry
INSERT INTO renewals ("lease-id", "at-date")
VALUES (:lease-id, :at-date)
ON CONFLICT ON CONSTRAINT "renewals_pkey" DO NOTHING

-- :name move-renewals! :! :n
-- :doc Move renewals from one lease to another
UPDATE renewals
SET "lease-id" = :to-lease-id
WHERE "lease-id" = :lease-id
/*~
(when (some? (:from-date params)) "AND \"at-date\" >= :from-date")
~*/
/*~
(when (some? (:to-date params)) "AND \"at-date\" <= :to-date")
~*/

-- :name delete-renewal! :! :n
-- :doc Delete single renewal
DELETE FROM renewals
WHERE "lease-id" = :lease-id
      AND "at-date" = :at-date

-- :name first-renewal-after :? :1
-- :doc Return first renewal following the specified date
SELECT "at-date"
FROM renewals
WHERE "lease-id" = :lease-id
      AND "at-date" > :after-date
ORDER BY "at-date" ASC
LIMIT 1

-- :name trim-renewals! :! :n
-- :doc Delete all renewals before the to-date
DELETE FROM renewals
WHERE "at-date" < :to-date
