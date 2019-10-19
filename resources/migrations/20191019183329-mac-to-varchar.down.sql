DROP INDEX leases_mac;
--;;
ALTER TABLE leases ALTER COLUMN mac TYPE macaddr USING mac::macaddr
--;;
CREATE INDEX mac
    ON leases USING btree
    (mac);
