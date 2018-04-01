ALTER TABLE leases
ADD COLUMN "end-date" timestamp with time zone
--;;
UPDATE leases set "end-date" = "start-date" + make_interval(secs => duration)
--;;
ALTER TABLE leases
ALTER COLUMN "end-date" SET NOT NULL
--;;
CREATE INDEX "end-date"
    ON leases USING btree
    ("end-date");
