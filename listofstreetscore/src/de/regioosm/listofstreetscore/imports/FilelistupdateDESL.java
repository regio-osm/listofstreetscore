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
 * Import central official street list for country Germany, state Saarland
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
public class FilelistupdateDESL {


	public static void main(String args[]) {

		Officialstreetlist_Filereader streetlist = new Officialstreetlist_Filereader();
		Updater_central_streetlist updater = new Updater_central_streetlist();
		
			// File format
			// (((kein Header vorhanden))), siehe liesmich Tabulator

		// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!   ACHTUNG: Spalten 1 bis 4 zusammengelegt, damit AGS in einer Spalte vorliegt !!!!!!!!!!!!!!!!!!
		
		//10041100	110	00102	Abtsdell	 	Saarbrücken, Landeshauptstadt	Alt-Saarbrücken	66117	041100110	041100116	00102116117	001021100116117	116	Bellevue	Alt-Saarbrücken / Bellevue	 	0	0
		streetlist.setInputfile_column_municipalityname(5);
		streetlist.setInputfile_column_id(0);
		streetlist.setInputfile_column_suburbname(6);
		streetlist.setInputfile_column_streetname(3);
		//streetlist.setInputfile_header_lastline("lageschluessel;lagebezeichnung;gemeindeschluessel;gemeinde");
		streetlist.setInputfile_min_columnnumber_for_active_lines(5);
		//streetlist.setInputfile_column_filter(5);
		//streetlist.setInputfile_column_filtercontentmustbe("^$");
		streetlist.setInputfile_columnsepartor("\t");
		
		streetlist.setMunicipalities_identify_adminhierarchy("%Saarland%");

		streetlist.setInputdirectory("/home/openstreetmap/NASI/OSMshare/Projekte-Zusammenarbeiten/Straßenlisten-ab-2012-11/Deutschland/Saarland-zentraleListe");
		//streetlist.setOutput_municipality_directory(streetlist.getInputdirectory() + "/" + "20140310_neu-Wiki-Update201403");
		//streetlist.setOutput_wiki_directory(streetlist.getInputdirectory() + "/" + "20140310_Stand-Wiki_vor_Update201403");
		streetlist.addFile("SAAR_STR_VERZ_20131122.csv");

		//streetlist.addWiki_ImporterIgnore("anystreetlistwikiuserinstate_BaWue");

		streetlist.setMunicipality_country("Bundesrepublik Deutschland");
		streetlist.setOfficialsource_copyrighttext("Landesamt für Zentrale Dienste, Saarbrücken");
		streetlist.setOfficialsource_useagetext("Zum Datenabgleich mit OSM-Daten bereitgestellt vom Landesamt für Zentrale Dienste, Saarbrücken per E-Mail am 10.03.2014 nach Anfrage von Dietmar Seifert\n\nDatenstand: 22.11.2013");
		streetlist.setOfficialsource_contentdate("2013-11-22");
		streetlist.setOfficialsource_filedate("2014-03-10");
		//streetlist.setWiki_pageupdatesummary("Import zentrales Straßenverzeichnis Saarland des Landesamt für Zentrale Dienste, Saarbrücken");
		//streetlist.setWiki_passwordstate("public");
		
		updater.setDestination("db");
		updater.update(streetlist);
	}
}
