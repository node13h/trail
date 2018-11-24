CREATE TABLE IF NOT EXISTS slices
(
    "lease-id" uuid NOT NULL REFERENCES leases (id) ON DELETE CASCADE,
    "offset" integer NOT NULL,
    CONSTRAINT slices_pkey PRIMARY KEY ("lease-id", "offset")
);
