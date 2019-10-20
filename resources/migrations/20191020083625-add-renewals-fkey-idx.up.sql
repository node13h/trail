CREATE INDEX "renewals_lease-id"
    ON renewals USING btree
    ("lease-id");
