DROP INDEX mac;
--;;
ALTER TABLE leases ALTER COLUMN mac TYPE VARCHAR
--;;
CREATE INDEX leases_mac
    ON leases USING btree
    (mac);
