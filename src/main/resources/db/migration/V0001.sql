CREATE TYPE public.securityclassification AS ENUM (
    'PUBLIC',
    'PRIVATE',
    'RESTRICTED'
);

CREATE TABLE public.case_data (
                                id bigserial primary key,
                                created_date timestamp without time zone DEFAULT now() NOT NULL,
                                last_modified timestamp without time zone,
                                jurisdiction character varying(255) NOT NULL,
                                case_type_id character varying(255) NOT NULL,
                                state character varying(255) NOT NULL,
                                data jsonb NOT NULL,
                                reference bigint NOT NULL unique,
                                security_classification public.securityclassification NOT NULL,
                                version integer DEFAULT 1 not null,
                                last_state_modified_date timestamp without time zone,
                                supplementary_data jsonb
);


--
-- Name: case_event; Type: TABLE; Schema: public; Owner: ccd
--

CREATE TABLE public.case_event (
                                 id bigserial primary key,
                                 created_date timestamp without time zone DEFAULT now() NOT NULL,
                                 event_id character varying(70) NOT NULL,
                                 summary character varying(1024),
                                 description character varying(65536),
                                 user_id character varying(64) NOT NULL,
                                 case_data_id bigint NOT NULL references case_data(id),
                                 case_type_id character varying(255) NOT NULL,
                                 case_type_version integer NOT NULL,
                                 state_id character varying(255) NOT NULL,
                                 data jsonb NOT NULL,
                                 user_first_name character varying(255) DEFAULT NULL::character varying NOT NULL,
                                 user_last_name character varying(255) DEFAULT NULL::character varying NOT NULL,
                                 event_name character varying(30) DEFAULT NULL::character varying NOT NULL,
                                 state_name character varying(255) DEFAULT ''::character varying NOT NULL
);


