/*

		AKTUELL
		Zeile "Municipality netto ===Landkreis Weißenburg-Gunzenhausen===" 
		/home/openstreetmap/programme/workspace/listofstreets/bin/localactive-owlgit-TEST-20120925.log


	V1.1, 23.09.2012 ff, Dietmar Seifert
		* produktiver Import in regio-osm.de
		FEHLER
			owlgitimportiert, Teil 5: ohne Titel: osm_relation_id 62509, 168564
			owlgitimportiert, Teil 6: ohne Titel: osm_relation_id 168548
			Übersichtssseiten entfernen, z.B. http://regio-osm.de/listofstreets_wiki/index.php/Landkreis_Freising
			Notify: zrqe09@gmail.com  noch offen, keine Ahnung, wohin damit
			User: xy
notify: 
* ace78@gmx.net
* MapperOG


INHALTLICH ZU DEN LISTEN
title: Niedersachsen/Landkreis Aurich/Marienhafe ::: klären mit User, ob Marienhafe oder gesamte Samtgemeinde mit Liste gemeint



	V1.0, 18.07.2011, Dietmar Seifert
		* derived from import_gemeindestrassen.java and secondary, get_streetlist_from_osmwiki.java

*/

package de.regioosm.listofstreetscore.util;

//import java.io.*;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Statement;
//import java.util.regex.*;
//import java.util.*;


public class Mediawiki_streetlist_article {


	/**
	 * @param args
	 */
	public  String erstellen  (
		String municipality_country, 
		String municipality_name, 
		String streets, 
		String sources,  
		String other, 
		String chunk, 
		String import_wikiusername, 
		String import_article_comment, 
		String article_template_municipalityadministrationid, 
		String article_template_osmrelationid, 
		String article_template_passwortschutz,
		String article_template_standdatum,
		String output_format
																) {

		streets = streets.replace("&auml;","ä");
		streets = streets.replace("&ouml;","ö");
		streets = streets.replace("&uuml;","ü");
		streets = streets.replace("&Auml;","Ä");
		streets = streets.replace("&Ouml;","Ö");
		streets = streets.replace("&Uuml;","Ü");
		streets = streets.replace("&szlig;","ß");
		streets = streets.replace("&","&amp;");
		streets = streets.replace("<","&lt;");
		streets = streets.replace(">","&gt;");

		sources = sources.replace("&auml;","ä");
		sources = sources.replace("&ouml;","ö");
		sources = sources.replace("&uuml;","ü");
		sources = sources.replace("&Auml;","Ä");
		sources = sources.replace("&Ouml;","Ö");
		sources = sources.replace("&Uuml;","Ü");
		sources = sources.replace("&szlig;","ß");
		sources = sources.replace("&","&amp;");
		sources = sources.replace("<","&lt;");
		sources = sources.replace(">","&gt;");

		other = other.replace("&auml;","ä");
		other = other.replace("&ouml;","ö");
		other = other.replace("&uuml;","ü");
		other = other.replace("&Auml;","Ä");
		other = other.replace("&Ouml;","Ö");
		other = other.replace("&Uuml;","Ü");
		other = other.replace("&szlig;","ß");
		other = other.replace("&","&amp;");
		other = other.replace("<","&lt;");
		other = other.replace(">","&gt;");

		chunk = chunk.replace("&auml;","ä");
		chunk = chunk.replace("&ouml;","ö");
		chunk = chunk.replace("&uuml;","ü");
		chunk = chunk.replace("&Auml;","Ä");
		chunk = chunk.replace("&Ouml;","Ö");
		chunk = chunk.replace("&Uuml;","Ü");
		chunk = chunk.replace("&szlig;","ß");
		chunk = chunk.replace("&","&amp;");
		chunk = chunk.replace("<","&lt;");
		chunk = chunk.replace(">","&gt;");

		StringBuffer sb = new StringBuffer();

		if(output_format.equals("xml")) {
			sb.append("  <page>\n");
			sb.append("    <title>"+municipality_name+"</title>\n");
			sb.append("    <ns>0</ns>\n");
			sb.append("    <revision>\n");
			sb.append("      <contributor>\n");
			sb.append("        <username>"+import_wikiusername+"</username>\n");
			sb.append("        <id>1</id>\n");
			sb.append("      </contributor>\n");
			sb.append("      <comment>/* "+import_article_comment+" */</comment>\n");
			sb.append("      <text xml:space=\"preserve\">\n");
		}
		sb.append("{{Strassenliste\n");

				
		if( (! municipality_country.equals("")) && 
			(! municipality_country.equals("Bundesrepublik Deutschland"))) {
			sb.append("|Land = " + municipality_country + "\n");
		}
		sb.append("|Gemeindeschlüssel = " + article_template_municipalityadministrationid + "\n");
		if( article_template_osmrelationid.equals("")) {
				sb.append("|OSM-Relation = " + article_template_osmrelationid + "\n");
		}
		sb.append("|Passwortgeschützt = " + article_template_passwortschutz + "\n");
		sb.append("}}\n");
		sb.append("\n");
		sb.append("\n");
		sb.append("== Liste ==\n");
		sb.append("\n");
		sb.append(streets + "\n");
		sb.append("\n");
		sb.append("\n");

		if( ! sources.equals("")) {
			sb.append("== Quelle ==\n");
			sb.append(sources + "\n");
		}

		if( ( ! other.equals("")) ||
			( ! chunk.equals(""))) {
			sb.append("");
			sb.append("== Sonstiges ==");
			sb.append("\n");
			if( ! other.equals("")) {
				sb.append(other + "\n");
				System.out.println("WARNING: chunk content in street list, PLEASE CHECK ==="+other+"===");
				other = "";
			}
			if( ! chunk.equals("")) {
				sb.append(chunk + "\n");
				System.out.println("ERROR: chunk content in street list, PLEASE CHECK ==="+chunk+"===");
				chunk = "";
			}
		}
		sb.append("\n");
		sb.append("[[Kategorie:Straßenliste]]\n");
			// wenn Liste nicht für Deutschland ist, dann extra Land-Kategorie ergänzen
		if( (	! municipality_country.equals("")) && 
				! municipality_country.equals("Bundesrepublik Deutschland")) {
			sb.append("[[Kategorie:"+municipality_country+"]]\n");
		}
		if(output_format.equals("xml")) {
			sb.append("</text>\n");
			sb.append("    </revision>\n");
			sb.append("  </page>\n");
		}

		return sb.toString();
	}

}
