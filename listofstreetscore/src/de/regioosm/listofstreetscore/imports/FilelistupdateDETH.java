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
 * Import central official street list for country Germany, state Thüringen
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
public class FilelistupdateDETH {

	public static void main(String args[]) {

		Officialstreetlist_Filereader streetlist = new Officialstreetlist_Filereader();
		Updater_central_streetlist updater = new Updater_central_streetlist();

			// File format (geändet ab 07/2014)
			//# #;20 Satzart 'e-z': Lagebezeichnung der Art Eisenbahn(e)|Gewanne(g)|Straße(s)|Gewässer(w)|Zuwegung(z)
			//# #;21                LandNr;0;KreisNr;GemeindeNr;LagebezeichnungNr;Lagebezeichnung
			//6 s;LandNr;0;KreisNr;GemeindeNr;LagebezeichnungNr;Straße
			//6 s;16;0;54;000;00041;Georg-Friedrich-Händel-Straße
		streetlist.setInputfile_column_municipalityname(1);
		streetlist.setInputfile_column_ids(1, 4);
		streetlist.setInputfile_column_streetname(6);
		streetlist.setInputfile_column_filter(0);							// identify real street objects in inputfile, there are also some other object types
		streetlist.setInputfile_column_filtercontentmustbe("6 s");			// identify real street objects in inputfile, there are also some other object types
		streetlist.setInputfile_header_lastline("# #;30");
		streetlist.setInputfile_min_columnnumber_for_active_lines(6);
		streetlist.setInputfile_columnsepartor(";");

		streetlist.setMunicipalities_identify_adminhierarchy("%Thüringen%");

		streetlist.setInputdirectory("/home/openstreetmap/NASI/OSMshare/Projekte-Zusammenarbeiten/Straßenlisten-ab-2012-11/Deutschland/Thüringen/zentraleListe/20160222");
		//streetlist.setOutput_municipality_directory(streetlist.getInputdirectory() + "/" + "20140719_neu-Wiki-Update20141014");
		//streetlist.setOutput_wiki_directory(streetlist.getInputdirectory() + "/" + "20140719_Stand-Wiki-vor-Update20141014");

		streetlist.addFile("51_Erfurt.txt");
		streetlist.addFile("52_Artern.txt");
		streetlist.addFile("53_Gotha.txt");
		streetlist.addFile("54_Worbis.txt");
		streetlist.addFile("55_Poessneck.txt");
		streetlist.addFile("56_Saalfeld.txt");
		streetlist.addFile("57_Schmalkalden.txt");
		streetlist.addFile("58_Zeulenroda.txt");

		//streetlist.addWiki_ImporterIgnore("anystreetlistwikiuserinstate_BaWue");

		streetlist.setMunicipality_country("Bundesrepublik Deutschland");
		streetlist.setOfficialsource_copyrighttext("Landesamt für Vermessung und Geoinformation, D-99086 Erfurt");
		streetlist.setOfficialsource_url("http://www.geoportal-th.de/downloadbereiche/alkiskonformerlagebezeichnungskatalog.aspx");
		streetlist.setOfficialsource_useagetext("<a href=\"http://www.geoportal-th.de/de-de/nutzungsbedingungen.aspx\">allgemeine Nutzungsbedinungen</a>; Nutzungsrecht für OSM: Ohne Nutzungsbeschränkung, explizit angefragt bei Landesamt für Vermessung und Geoinformation, Erfurt in 09/2011");
		streetlist.setOfficialsource_contentdate("2016-02-22");
		streetlist.setOfficialsource_filedate("2016-02-22");
		//streetlist.setWiki_pageupdatesummary("Import zentrales Straßenverzeichnis von Thüringen, Stand 14.07.2014");
		//streetlist.setWiki_passwordstate("public");

		updater.setDestination("db");
		updater.update(streetlist);
	}
}
