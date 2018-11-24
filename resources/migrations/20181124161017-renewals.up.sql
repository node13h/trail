CREATE TABLE IF NOT EXISTS renewals
(
    "lease-id" uuid NOT NULL REFERENCES leases (id) ON DELETE CASCADE,
    "at-date" timestamp with time zone NOT NULL,
    CONSTRAINT renewals_pkey PRIMARY KEY ("lease-id", "at-date")
)
--;;
CREATE INDEX "renewals_at-date"
    ON renewals USING btree
    ("at-date");
