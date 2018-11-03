CREATE TABLE IF NOT EXISTS slices
(
    "lease-id" uuid REFERENCES leases (id) ON DELETE CASCADE,
    "offset" integer NOT NULL
);
--;;
CREATE INDEX "lease-id"
    ON slices USING btree
    ("lease-id");
