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



public class FilelistupdateIS {

	public static void main(String args[]) {

if(1==1) {
	System.out.println("WARNING: the path and file name have housenumber addresses.");
	System.out.println("  Please reduce file to just streets");
	return;
}
		
		Officialstreetlist_Filereader streetlist = new Officialstreetlist_Filereader();
		Updater_central_streetlist updater = new Updater_central_streetlist();
			// File format
			//#HNITNUM	SVFNR	BYGGD	LANDNR	HEINUM	FASTEIGNAHEITI	MATSNR	POSTNR	HEITI_NF	HEITI_TGF	HUSNR	BOKST	VIDSK	SERHEITI	DAGS_INN	DAGS_LEIDR	GAGNA_EIGN	TEGHNIT	YFIRFARID	ATH	NAKV_XY	X_ISN93	Y_ISN93	LAT_WGS84	LONG_WGS84
			//10095286	0000	01	111632	1000751	2.Gata v/Rauðavatn 29		110	2.Gata v/Rauðavatn	2.Gata v/Rauðavatn	29				10.11.2008	23.02.2009	Þjóðskrá Íslands	1	2			365037,809090909	404228,736363636	64,11438660967	-21,7702940057837
		streetlist.setInputfile_column_municipalityname(1);
		streetlist.setInputfile_column_id(2);
		streetlist.setInputfile_column_streetname(9);
		streetlist.setInputfile_min_columnnumber_for_active_lines(24);
		//streetlist.setInputfile_column_filter(5);
		//streetlist.setInputfile_column_filtercontentmustbe("^$");
		streetlist.setInputfile_columnsepartor("\\t");

		//streetlist.setMunicipalities_identify_adminhierarchy("%Rheinland-Pfalz%");

		streetlist.setInputdirectory("/home/openstreetmap/NASI/OSMshare/Projekte-Zusammenarbeiten/Hausnummernlisten/Island/20160229");
		//streetlist.setOutput_municipality_directory(streetlist.getInputdirectory() + "/" + "201401628_neu-Wiki");
		//streetlist.setOutput_wiki_directory(streetlist.getInputdirectory() + "/" + "201401628_Stand-Wiki");
		streetlist.addFile("STADFANG.dsv");

		streetlist.setMunicipality_country("Ísland");
		streetlist.setOfficialsource_copyrighttext("Þjóðskrá Íslands");
		streetlist.setOfficialsource_useagetext("<a href=\"http://www.skra.is/fasteignaskra/nidurhalsthjonusta/stadfangaskra/user-licence/\">License, ODbL Compliance</a>");
		streetlist.setOfficialsource_url("http://www.skra.is/default.aspx?PageID=ddaee5cd-fec7-4467-a913-131af29f2941");
		streetlist.setOfficialsource_contentdate("2016-02-28");
		streetlist.setOfficialsource_filedate("2016-02-28");
		//streetlist.setWiki_pageupdatesummary("Import Iceland street lists due to using addressfile vom Statistics Iceland");
		//streetlist.setWiki_passwordstate("public");

		updater.setDestination("db");
		updater.update(streetlist);
	}
}
