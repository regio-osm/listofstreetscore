-- Alle Tabellen zum Aufbau der Straßenlisten-DB-Anwendung
--
-- V1.8, 21.11.2013, Dietmar Seifert
--      neue Tabelle history_evaluation_overview ergänzt, identisch mit normaler evaluation_overview
--         bisher war geplant, diese auch dort zu speichern, aber das soll nicht miteinander vermischt werden
--         jetzt ist auch das erste Update der fullhistory Auswertung geplant
--
-- V1.7, 23.02.2013, Dietmar Seifert
--      in Tabelle evaluation Index ergaenzt auf municipality_id zum deutlich schnelleren Zugriff bei Queries
-      in Tabelle municipality wird hierarchy entfernt (war nicht in Nutzung) und neue Spalte ergaenzt osm_hierarchy_level
--
-- V1.6, 06.11.2012, Dietmar Seifert
--      in Tabelle officialkeys etliche Felder ergänzt, die in der Inputdatei
--      vorhanden sind und Einwohnerzahl und Stadt-/Gemeindeklassifizerung haben
--         alter table officialkeys add column flaechekm2 real;
--         alter table officialkeys add column stand text;
--         alter table officialkeys add column bevoelkerungszahl integer;
--         alter table officialkeys add column gliederungstadtland smallint;
--
-- V1.5, 22.09.2012, Dietmar Seifert
--      in Tabelle evaluation Spalte osmdb_tstamp ergaenzt, um jetzt je Auswertung der OSM-DB Stand zu speichern
--
-- V1.4, 17.09.2012, Dietmar Seifert
--   Reihenfolge einiger neuerer Spalten korrigiert, Fehler aufgefalle während Transfer von nb11 nach regio-osm.de
--
-- V1.3, 15.08.2012, Dietmar Seifert
--   in table officialkeys add column name_unique
--
-- V1.2, 13.08.2012, Dietmar Seifert
--   pgsql-Funktion für korrekte deutsche Umlautsortierung (correctorder)
--   in table add column   sourcelist_filedate for storing external file/wiki timestamp (in UTC-format)
--
-- V1.1, 05.02.2012, Dietmar Seifert
--   in municipality bisherige column municipality_hierarchy umgenannt in osm_hierarchy
--   und zuzaetzlich hierarchy, das in Deutschland durch die Tabelle des statistischen Bundesamtes gesetzt wird
--   bisherige column municipality_administrationid wird abgekuerzt in ags
--
-- V1.0, 18.06.2011, Dietmar Seifert


-- Table: country

DROP TABLE country;

CREATE TABLE country
(
  id                              bigserial      NOT NULL,
  country                         text           NOT NULL,
  abbreviation                    text,                         -- ISO-3166 2 Character Code uppercase letter, see http://de.wikipedia.org/wiki/ISO-3166-1-Kodierliste
  osmrelation_administration_key  text,
  osmobjects_administration_key   text,
  CONSTRAINT pk_country           PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
--ALTER TABLE country OWNER TO strassenuser;
INSERT INTO country (country) values('Bundesrepublik Deutschland');


-- Table: municipality

DROP TABLE municipality;

CREATE TABLE municipality
(
  id                              bigserial      NOT NULL,
  country_id                      bigint         NOT NULL,
  name                            name           NOT NULL,
  officialkeys_id                 text,
  municipality_level              int,
  osm_hierarchy                   text,
  osm_hierarchy_level             int,
  osm_relation_id                 text           NOT NULL,
  sourcelist_passwordstatus       text,
  polygon                         geometry,
  polygon_state                   text, 
  hierarchy                       text,
  sourcelist_url                  text,                          -- depricated from 2016-02-23 on - contained the url to municipality article on listofstreets wiki - See new version officialsource_url
  sourcelist_text                 text,                          -- depricated from 2016-02-23 on - only a few records are set. Information about origin source of the lists - See new version officialsource_useagetext
  sourcelist_source               text,                          -- depricated from 2016-02-23 on - only a few records are set. Earlier entries for Florian-Lohoff and Sven-Anders source - See new version officialsource_copyrightext
  sourcelist_deliverydate         text,                          -- depricated from 2016-02-23 on - wasn't filled anythinbg - See new version officialsource_contentdate
  sourcelist_filedate             text,                          -- depricated from 2016-02-23 on - had listofstreets wiki last modified date. See new version officialsource_filedate
  officialsource_copyrighttext    text,                          -- new 2016-02-23 - copyright text from originator
  officialsource_useagetext       text,                          -- new 2016-02-23 - useage text, normally formal license text and/or link, or explicit useage for osm
  officialsource_url              text,                          -- new 2016-02-23 - url to street list itself or explaining page with link to street list
  officialsource_contentdate      date,                          -- new 2016-02-23 - date of content in street list 
  officialsource_filedate         date,                          -- new 2016-02-23 - date of the file, when it was public available or when it get from originator or its website
  
  parameters                      hstore,                        -- new 2016-02-09 add municipality specific parameters for evaluations as key-value pairs
                                                                 --   parameter: streetref=>osmkeyname, for example streetref=de:strassenschluessel
  CONSTRAINT pk_municipality      PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
--ALTER TABLE municipality OWNER TO strassenuser;


-- Table: street

DROP TABLE street;

CREATE TABLE street
(
  id                              bigserial      NOT NULL,
  country_id                      bigint         NOT NULL,
  municipality_id                 bigint         NOT NULL,
  name                            name           NOT NULL,
  municipality_addition           text,
  streetref                       text,                         -- new 2016-02-09 local based reference id of street
  point                           geometry,
  point_source                    text,
  point_state                     text,
  point_numbertriedgeocoding      int,
  evaluation_state                text,                         -- neu 11.11.2011 enthält Status der Straße, list|listandosm|osmonly|inactive
  evaluation_state_old            text,                         -- neu 11.11.2011 enthält vorherigen Status der Straße, listonly|listandosm|osmonly|inactive
  typed_osm_ids                   text[],                       -- neu 11.11.2011 enthält Liste der OSM-ids aller Straßenteile, komma-separated
  CONSTRAINT pk_street            PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
--ALTER TABLE street OWNER TO strassenuser;
create index street_municipalityidindex on street (municipality_id);


-- Table: streetoriginal

DROP TABLE streetoriginal;

CREATE TABLE streetoriginal
(
  id                              bigserial      NOT NULL,
  country_id                      bigint         NOT NULL,
  municipality_id                 bigint         NOT NULL,
  name                            text           NOT NULL,
  streetref                       text,                         -- new 2016-02-09 local based reference id of street
  storetimestamp                  timestamp without time zone,
  CONSTRAINT pk_streetoriginal    PRIMARY KEY (id)
)
WITH (OIDS=FALSE);


-- Table: streetcorrection

DROP TABLE streetcorrection;

CREATE TABLE streetcorrection
(
  id                              bigserial      NOT NULL,
  country_id                      bigint         NOT NULL,
  municipality_id                 bigint         NOT NULL,
  originalname                    text           NOT NULL,
  name                            text,
  correctiontype                  text,
  streetref                       text,                         -- new 2016-02-09 local based reference id of street
  storetimestamp                  timestamp without time zone,
  CONSTRAINT pk_streetcorrection  PRIMARY KEY (id)
)
WITH (OIDS=FALSE);


-- Table: odblstate

DROP TABLE odblstate;

CREATE TABLE odblstate
(
  typed_id                        text           NOT NULL,
  version                         text,
  decision                        text,
  userid                          bigint,
  datetime                        text,
  CONSTRAINT pk_odblstate         PRIMARY KEY (typed_id,version,userid)
)
WITH (OIDS=FALSE);
--ALTER TABLE odblstate OWNER TO strassenuser;
create index odblstate_useridindex on odblstate (userid);



-- Table: evaluation

DROP TABLE evaluation;

CREATE TABLE evaluation
(
  id                              bigserial      NOT NULL,
  country_id                      bigint         NOT NULL,
  municipality_id                 bigint         NOT NULL,
  evaluation_overview_id          int,
  number_liststreets              bigint         NOT NULL,
  number_osmstreets               bigint         NOT NULL,
  number_osmsinglestreets         bigint         NOT NULL,
  number_missingstreets           bigint         NOT NULL,
  tstamp                          timestamp without time zone NOT NULL,
  osmdb_tstamp                    timestamp without time zone,
  CONSTRAINT pk_evaluation        PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
 CREATE INDEX ON evaluation (municipality_id);   -- new 24.11.2013

-- Table: evaluation_overview

DROP TABLE evaluation_overview;

CREATE TABLE evaluation_overview
(
  id                              bigserial      NOT NULL,
  evaluation_first_id             bigint         NOT NULL,
  evaluation_last_id              bigint         NOT NULL,
  description                     text,
  evaluation_type                 text,
  evaluation_number               int,
  CONSTRAINT pk_evaluationoverview   PRIMARY KEY (id)
)
WITH (OIDS=FALSE);


                     String insert_evaluationstreet = "INSERT INTO evaluation_street (evaluation_id,street_id,osm_id,osm_type,osm_keyvalue) ";
-- Table: evaluation_street

DROP TABLE evaluation_street;

CREATE TABLE evaluation_street
(
  id                              bigserial      NOT NULL,
  evaluation_id                   bigint,
  street_id                       bigint,
  name                            text,							-- !!!!!!!!!!!!!!!! noch type name, am 1.12.2014 zu lange bei alter table evaluation_street alter column name set data type text;
  osm_id                          text,
  osm_type                        text,
  osm_keyvalue                    text,
  streetref                       text,                         -- new 2016-02-09 local based reference id of street
  osm_point_leftbottom            geometry,                     -- new 2016-02-16 left-bottom border position of bbox containing osm street(s)
  osm_point_righttop              geometry,                     -- new 2016-02-16 right-top border position of bbox containing osm street(s)
  CONSTRAINT pk_evaluationstreet  PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
 create index on evaluation_street ( evaluation_id);





-- Table: history_evaluation

DROP TABLE history_evaluation;

CREATE TABLE history_evaluation
(
  id                              bigserial      NOT NULL,
  country_id                      bigint         NOT NULL,
  municipality_id                 bigint         NOT NULL,
  history_evaluation_overview_id  int,
  history_timestamp               text,
  number_liststreets              bigint         NOT NULL,
  number_osmstreets               bigint         NOT NULL,
  number_osmsinglestreets         bigint         NOT NULL,
  number_missingstreets           bigint         NOT NULL,
  tstamp                          timestamp without time zone NOT NULL,
  osmdb_tstamp        timestamp without time zone,
  license char(1), 
  CONSTRAINT pk_history_evaluation PRIMARY KEY (id)
)
WITH (OIDS=FALSE);


-- Table: history_evaluation_overview

DROP TABLE history_evaluation_overview;

CREATE TABLE history_evaluation_overview
(
  id                              bigserial      NOT NULL,
  evaluation_first_id             bigint         NOT NULL,
  evaluation_last_id              bigint         NOT NULL,
  description                     text,
  evaluation_type                 text,
  evaluation_number               int,
  CONSTRAINT pk_historyevaluationoverview PRIMARY KEY (id)
)
WITH (OIDS=FALSE);



-- Table: osmusers

DROP TABLE osmusers;

CREATE TABLE osmusers
(
  id                              bigserial      NOT NULL,
  username                        text           NOT NULL,
  CONSTRAINT pk_osmusers PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
--ALTER TABLE osmusers OWNER TO strassenuser;


-- Table: officialkeys

DROP TABLE officialkeys;

CREATE TABLE officialkeys
(
  id                              bigserial      NOT NULL,
  name                            name           NOT NULL,
  ags                             text,
  rs                              text,
  level                           smallint,
  hierarchy                       text,
  name_unique                     name, 
  flaechekm2                      real, 
  stand                           text, 
  bevoelkerungszahl               integer, 
  gliederungstadtland             smallint,
  country_id                      bigint         NOT NULL,
  temp_import_state               text,                         -- temporary columns, which is only important during update of the official list
  CONSTRAINT pk_officialkeys      PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
--ALTER TABLE officielakeys OWNER TO strassenuser;



-- Table: messages

DROP TABLE messages;

CREATE TABLE messages
(
  id                              bigserial      NOT NULL,
  class                           integer        NOT NULL,
  messagenumber                   integer        NOT NULL,
  municipality_id                 bigint,
  message                         text,
  tstamp                          timestamp without time zone NOT NULL,
  CONSTRAINT pk_messages PRIMARY KEY (id)
)
WITH (OIDS=FALSE);



-- Table: export2shape

DROP TABLE export2shape;

CREATE TABLE export2shape
(
  gemndename                      text,
  orgakontxt                      text,
  str_soll                        bigint,
  str_osm                         bigint,
  str_fhlosm                      bigint,
  str_nurosm                      bigint,
  str_abdeck                      bigint,
  osm_rel_id                      text, 
  regschlssl                      text,
  polygnstat                      text, 
  country                         text,
  sourcelist                      text,
  stand                           timestamp without time zone,
  polygon                         geometry,
  id                              bigserial      NOT NULL,
  adminlevel                      int
)
WITH (OIDS=FALSE);


-- Table: boundaries2shape

DROP TABLE boundaries2shape;

CREATE TABLE boundaries2shape
(
  osm_id                          bigint,
  name                            text,
  admin_level                     int,
  polygon                         geometry,
  id                              bigserial      NOT NULL
)
WITH (OIDS=FALSE);


-- Table: street_lastccbysa
--   copy of table structure street. only necessary for fix last evalation with osm license ccbysa

DROP TABLE street_lastccbysa;

CREATE TABLE street_lastccbysa
(
  id                              bigserial      NOT NULL,
  country_id                      bigint         NOT NULL,
  municipality_id                 bigint         NOT NULL,
  name                            name           NOT NULL,
  municipality_addition           text,   
  point                           geometry,
  point_source                    text,
  point_state                     text,
  point_numbertriedgeocoding      int,
  evaluation_state                text,                         -- neu 11.11.2011 enthält Status der Straße, list|listandosm|osmonly|inactive
  evaluation_state_old            text,                         -- neu 11.11.2011 enthält vorherigen Status der Straße, listonly|listandosm|osmonly|inactive
  typed_osm_ids                   text[],                       -- neu 11.11.2011 enthält Liste der OSM-ids aller Straßenteile, komma-separated
  CONSTRAINT pk_street_lastccbysa PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
--ALTER TABLE street_lastccbysa OWNER TO strassenuser;


-- Table: muniview

DROP TABLE muniview;


CREATE TABLE muniview 
  AS SELECT
    name, country, e.id as e_id, number_liststreets, number_osmstreets, number_osmsinglestreets, 
    number_missingstreets, 100.0*number_osmstreets/number_liststreets as abdeck, 
    polygon_state, osm_relation_id, sourcelist_url, officialkeys_id, osmrelation_administration_key, 
    polygon, tstamp 
    FROM
    evaluation AS e, municipality AS m, country AS c 
    WHERE
    municipality_id = m.id AND number_liststreets > 0
    AND m.country_id = c.id
    AND evaluation_overview_id = (select max(id) from evaluation_overview);
ALTER TABLE muniview ADD COLUMN class int;
UPDATE muniview SET class = 0 WHERE abdeck < 0;
UPDATE muniview SET class = 1 WHERE abdeck >= 0 and abdeck < 25.0;
UPDATE muniview SET class = 2 WHERE abdeck >= 25.0 and abdeck < 50.0;
UPDATE muniview SET class = 3 WHERE abdeck >= 50.0 and abdeck < 75.0;
UPDATE muniview SET class = 4 WHERE abdeck >= 75.0 and abdeck < 85.0;
UPDATE muniview SET class = 5 WHERE abdeck >= 85.0 and abdeck < 95.0;
UPDATE muniview SET class = 6 WHERE abdeck >= 95.0 and abdeck < 100.0;
UPDATE muniview SET class = 7 WHERE abdeck >= 100.0;

   

--origin from http://postgresql.1045698.n5.nabble.com/german-sort-is-wrong-td5582836.html
CREATE OR REPLACE FUNCTION correctorder(text)
  RETURNS text AS
  $BODY$ SELECT REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(lower($1),'ß','ss'),'ä','ae'),'ö','oe'),'ü','ue'),'â','a'),'í','i'),'Í','I') $BODY$
  LANGUAGE sql VOLATILE
  COST 100;
ALTER FUNCTION correctorder(text) OWNER TO postgres; 
-- if necessary, build an index with
-- CREATE INDEX correctorder_idx ON street (correctorder(col1));

