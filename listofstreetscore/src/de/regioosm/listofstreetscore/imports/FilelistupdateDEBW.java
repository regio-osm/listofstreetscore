package de.regioosm.listofstreetscore.imports;
/*
 	OSM Straßenlistenverwaltung zum Abgleich offieller Straßenlisten mit den erfassten Straßen in OSM
    Copyright (C) 2014  Dietmar Seifert   Mail-contact: strassenliste@diesei.de

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.regioosm.listofstreetscore.util.Officialstreetlist_Filereader;
import de.regioosm.listofstreetscore.util.Updater_central_streetlist;

/**
 * Import central official street list for country Germany, state Baden-Württemberg
 * into listofstreets evaluation DB.
 * Prior to 2016-02, the list was imported into evaluation Wiki for interactive manipulation
 * through OSM users, but after 2016-02, the list will be imported directly into DB and
 * OSM users can manipulate street lists through website on evaluation pages.
 * 
 * This class will be run directly on evaluation server on command line.
 * Some file path and names must be configured in source code directly, sorry.
 * 
 * @author Dietmar Seifert
 * @version 3.0
 *
 */
public class FilelistupdateDEBW {

	public static void main(String args[]) {

		Officialstreetlist_Filereader streetlist = new Officialstreetlist_Filereader();
		Updater_central_streetlist updater = new Updater_central_streetlist();
		
			// File format
			//lageschluessel;lagebezeichnung;gemeindeschluessel;gemeinde
			//00020;Aachener Straße;08111000;Stuttgart
			//00040;Aalstraße;08111000;Stuttgart
		streetlist.setInputfile_column_municipalityname(3);
		streetlist.setInputfile_column_id(2);
		streetlist.setInputfile_column_streetname(1);
		streetlist.setInputfile_header_lastline("lageschluessel;lagebezeichnung;gemeindeschluessel;gemeinde");
		streetlist.setInputfile_min_columnnumber_for_active_lines(4);
		streetlist.setInputfile_columnsepartor(";");

		streetlist.setMunicipalities_identify_adminhierarchy("%Baden-W%");

		streetlist.setInputdirectory("/home/openstreetmap/NASI/OSMshare/Projekte-Zusammenarbeiten/Straßenlisten-ab-2012-11/Deutschland/BadenWürttemberg/zentraleListe/20160223");
		streetlist.setOutput_municipality_directory(streetlist.getInputdirectory() + "/" + "20160223_neu-Wiki-Update201601");
		streetlist.setOutput_wiki_directory(streetlist.getInputdirectory() + "/" + "20160223_bisher-Wiki-Update201601");
		streetlist.addFile("OD_Strassenschluessel_20160104.csv");

		//streetlist.addWiki_ImporterIgnore("anystreetlistwikiuserinstate_BaWue");

		streetlist.setMunicipality_country("Bundesrepublik Deutschland");
		streetlist.setOfficialsource_copyrighttext("Datenquelle: LGL, www.lgl-bw.de");
		streetlist.setOfficialsource_useagetext("<a href=\"http://creativecommons.org/licenses/by/3.0/deed.de\">Lizenz CC BY 3.0");
		streetlist.setOfficialsource_url("https://www.lgl-bw.de/lgl-internet/opencms/de/07_Produkte_und_Dienstleistungen/Open_Data_Initiative/index.html");
		streetlist.setOfficialsource_filedate("2016-02-23");
		streetlist.setOfficialsource_contentdate("2016-01-04");

		
		//streetlist.setWiki_pageupdatesummary("Import zentrales Straßenverzeichnis (ZIP-Archiv mit CSV-Datei (UTF-8), Stand ALKIS Migration 02.01.2014) vom Baden-Württembergischen Landesamt für Geoinformation und Landesentwicklung (LGL)");
		//streetlist.setWiki_passwordstate("public");
		updater.setDestination("db");
		//updater.setJustsimulateimport(true);
		updater.update(streetlist);
	}
}
