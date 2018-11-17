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

-- :name first-slice-after :? :1
-- :doc Return first offset following the specified one
SELECT "offset"
FROM slices
WHERE "lease-id" = :lease-id
      AND "offset" > :offset
ORDER BY "offset" ASC
LIMIT 1

-- :name delete-slice! :! :n
-- :doc Delete single slice
DELETE FROM slices
WHERE "lease-id" = :lease-id
      AND "offset" = :offset

-- :name add-slice! :! :n
-- :doc Add slice
INSERT INTO slices ("lease-id", "offset")
VALUES (:lease-id, :offset)
ON CONFLICT ON CONSTRAINT "slices_pkey" DO NOTHING

-- :name move-slices! :! :n
-- :doc Move slices from one lease to another adjusting the offset
UPDATE slices
SET "offset" = "offset" + :delta, "lease-id" = :to-lease-id
WHERE "lease-id" = :lease-id
/*~
(when (some? (:from params)) "AND \"offset\" >= :from")
~*/
/*~
(when (some? (:to params)) "AND \"offset\" <= :to")
~*/

-- :name truncate-lease! :<! :*
-- :doc Truncate the duration of lease
UPDATE leases SET "end-date" = :end-date
WHERE id = :id
RETURNING id


-- :name trim! :! :n
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
VALUES (:ip::inet, :end-date::timestamp with time zone)
ON CONFLICT ON CONSTRAINT "releases_pkey" DO NOTHING
