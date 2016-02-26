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


public class FilelistupdateDERP {

	public static void main(String args[]) {

		Officialstreetlist_Filereader streetlist = new Officialstreetlist_Filereader();
		Updater_central_streetlist updater = new Updater_central_streetlist();

			// File format
			// (((kein Header vorhanden)))
			//2|GV Lambsheim|07338016|Lambsheim|Alte Mälzerei||1|1
			//2|GV Lambsheim|07338016|Lambsheim|Am alten Großmarkt||1|114
		streetlist.setInputfile_column_municipalityname(3);
		streetlist.setInputfile_column_id(2);
		streetlist.setInputfile_column_streetname(4);
		//streetlist.setInputfile_header_lastline("lageschluessel;lagebezeichnung;gemeindeschluessel;gemeinde");
		streetlist.setInputfile_min_columnnumber_for_active_lines(5);
		streetlist.setInputfile_column_filter(5);
		streetlist.setInputfile_column_filtercontentmustbe("^$");
		streetlist.setInputfile_columnsepartor("\\|");
		
		streetlist.setMunicipalities_identify_adminhierarchy("%Rheinland-Pfalz%");

		streetlist.setInputdirectory("/home/openstreetmap/NASI/OSMshare/programme/workspace/listofstreets/data/RP/2014-01");
		streetlist.setOutput_municipality_directory(streetlist.getInputdirectory() + "/" + "20140113_neu-Wiki-Update201401");
		streetlist.setOutput_wiki_directory(streetlist.getInputdirectory() + "/" + "20140113_Stand-Wiki_vor_Update201401");
		streetlist.addFile("strassen_rlp_2014-01-utf8.txt");

		//streetlist.addWiki_ImporterIgnore("anystreetlistwikiuserinstate_BaWue");

		streetlist.setMunicipality_country("Bundesrepublik Deutschland");
		streetlist.setOfficialsource_copyrighttext("Ohne Nutzungsbeschränkung, Daten: [http://www.kommwis.de/kommwis/Dokumente/Tabellen/ Straßentabelle Rheinland-Pfalz]\n\nDatenstand: 01.01.2014");
		streetlist.setOfficialsource_filedate("");
		streetlist.setOfficialsource_contentdate("");
		//streetlist.setWiki_pageupdatesummary("Import zentrales Straßenverzeichnis KommWis Gesellschaft für Kommunikation und Wissenstransfer mbH, Mainz");
		//streetlist.setWiki_passwordstate("public");
		
		updater.setDestination("db");
		//updater.setJustsimulateimport(true);
		updater.update(streetlist);
	}
}
