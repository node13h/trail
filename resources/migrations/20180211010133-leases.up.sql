CREATE TABLE IF NOT EXISTS leases
(
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    ip inet NOT NULL,
    mac macaddr NOT NULL,
    "start-date" timestamp with time zone NOT NULL,
    duration integer NOT NULL,
    data jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT leases_pkey PRIMARY KEY (id),
    CONSTRAINT "ip-start-date" UNIQUE (ip, "start-date")
);
--;;
CREATE INDEX ip
    ON leases USING btree
    (ip);
--;;
CREATE INDEX mac
    ON leases USING btree
    (mac);
--;;
CREATE INDEX "start-date"
    ON leases USING btree
    ("start-date");
