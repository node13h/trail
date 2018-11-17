CREATE TABLE IF NOT EXISTS releases
(
    ip inet NOT NULL,
    "end-date" timestamp with time zone NOT NULL,
    CONSTRAINT releases_pkey PRIMARY KEY (ip, "end-date")
);
