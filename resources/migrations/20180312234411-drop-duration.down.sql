ALTER TABLE leases
ADD COLUMN duration integer
--;;
UPDATE leases set duration = EXTRACT(EPOCH FROM "end-date" - "start-date")
--;;
ALTER TABLE leases
ALTER COLUMN duration SET NOT NULL
