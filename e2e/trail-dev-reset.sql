--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.18
-- Dumped by pg_dump version 9.6.18

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

ALTER TABLE ONLY public.renewals DROP CONSTRAINT "renewals_lease-id_fkey";
DROP INDEX public."renewals_at-date";
DROP INDEX public."releases_end-date";
DROP INDEX public."leases_start-date";
DROP INDEX public.leases_mac;
DROP INDEX public.leases_ip;
DROP INDEX public."leases_end-date";
ALTER TABLE ONLY public.schema_migrations DROP CONSTRAINT schema_migrations_id_key;
ALTER TABLE ONLY public.renewals DROP CONSTRAINT renewals_pkey;
ALTER TABLE ONLY public.releases DROP CONSTRAINT releases_pkey;
ALTER TABLE ONLY public.leases DROP CONSTRAINT leases_pkey;
ALTER TABLE ONLY public.leases DROP CONSTRAINT "leases_ip-start-date";
DROP TABLE public.schema_migrations;
DROP TABLE public.renewals;
DROP TABLE public.releases;
DROP TABLE public.leases;
DROP EXTENSION "uuid-ossp";
DROP EXTENSION plpgsql;
DROP SCHEMA public;
--
-- Name: DATABASE postgres; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON DATABASE postgres IS 'default administrative connection database';


--
-- Name: public; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA public;


ALTER SCHEMA public OWNER TO postgres;

--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA public IS 'standard public schema';


--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;


--
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: leases; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.leases (
    id uuid DEFAULT public.uuid_generate_v1() NOT NULL,
    ip inet NOT NULL,
    mac character varying NOT NULL,
    "start-date" timestamp with time zone NOT NULL,
    data jsonb DEFAULT '{}'::jsonb NOT NULL,
    "end-date" timestamp with time zone NOT NULL
);


ALTER TABLE public.leases OWNER TO postgres;

--
-- Name: releases; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.releases (
    ip inet NOT NULL,
    "end-date" timestamp with time zone NOT NULL
);


ALTER TABLE public.releases OWNER TO postgres;

--
-- Name: renewals; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.renewals (
    "lease-id" uuid NOT NULL,
    "at-date" timestamp with time zone NOT NULL
);


ALTER TABLE public.renewals OWNER TO postgres;

--
-- Name: schema_migrations; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.schema_migrations (
    id bigint NOT NULL,
    applied timestamp without time zone,
    description character varying(1024)
);


ALTER TABLE public.schema_migrations OWNER TO postgres;

--
-- Data for Name: leases; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.leases (id, ip, mac, "start-date", data, "end-date") FROM stdin;
\.


--
-- Data for Name: releases; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.releases (ip, "end-date") FROM stdin;
\.


--
-- Data for Name: renewals; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.renewals ("lease-id", "at-date") FROM stdin;
\.


--
-- Data for Name: schema_migrations; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.schema_migrations (id, applied, description) FROM stdin;
20180211010133	2020-06-19 08:55:11.405	leases
20180312074308	2020-06-19 08:55:11.435	end-date
20180312234411	2020-06-19 08:55:11.447	drop-duration
20181103115018	2020-06-19 08:55:11.463	slices
20181105171028	2020-06-19 08:55:11.478	slices-offset-index
20181105194459	2020-06-19 08:55:11.496	releases
20181118231459	2020-06-19 08:55:11.513	releases-end-date-idx
20181124161017	2020-06-19 08:55:11.532	renewals
20181124173208	2020-06-19 08:55:11.548	drop-slices
20191019183329	2020-06-19 08:55:11.57	mac-to-varchar
20191019235341	2020-06-19 08:55:11.582	rename-indexes
\.


--
-- Name: leases leases_ip-start-date; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.leases
    ADD CONSTRAINT "leases_ip-start-date" UNIQUE (ip, "start-date");


--
-- Name: leases leases_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.leases
    ADD CONSTRAINT leases_pkey PRIMARY KEY (id);


--
-- Name: releases releases_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.releases
    ADD CONSTRAINT releases_pkey PRIMARY KEY (ip, "end-date");


--
-- Name: renewals renewals_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.renewals
    ADD CONSTRAINT renewals_pkey PRIMARY KEY ("lease-id", "at-date");


--
-- Name: schema_migrations schema_migrations_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.schema_migrations
    ADD CONSTRAINT schema_migrations_id_key UNIQUE (id);


--
-- Name: leases_end-date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "leases_end-date" ON public.leases USING btree ("end-date");


--
-- Name: leases_ip; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX leases_ip ON public.leases USING btree (ip);


--
-- Name: leases_mac; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX leases_mac ON public.leases USING btree (mac);


--
-- Name: leases_start-date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "leases_start-date" ON public.leases USING btree ("start-date");


--
-- Name: releases_end-date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "releases_end-date" ON public.releases USING btree ("end-date");


--
-- Name: renewals_at-date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX "renewals_at-date" ON public.renewals USING btree ("at-date");


--
-- Name: renewals renewals_lease-id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.renewals
    ADD CONSTRAINT "renewals_lease-id_fkey" FOREIGN KEY ("lease-id") REFERENCES public.leases(id) ON DELETE CASCADE;


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--

