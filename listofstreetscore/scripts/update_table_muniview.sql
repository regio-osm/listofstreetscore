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
    AND m.country <> 'Neu-Meck-Vorp'
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

