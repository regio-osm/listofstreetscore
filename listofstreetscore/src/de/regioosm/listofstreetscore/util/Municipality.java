/*

	V1.0, 18.07.2011, Dietmar Seifert
		* derived from import_gemeindestrassen.java and secondary, get_streetlist_from_osmwiki.java

*/

package de.regioosm.listofstreetscore.util;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
//import java.io.*;
import java.util.regex.*;

import de.regioosm.listofstreetscore.util.Applicationconfiguration;


public class Municipality {
	static Connection global_con_listofstreets = null;
	static Statement global_stmt = null;
	static Applicationconfiguration configuration = new Applicationconfiguration();

	public Municipality() {

		
		
			// Code for every new instance must be insert here
			// Code for every new instance must be insert here
			// Code for every new instance must be insert here
		
		
		
		
		
			// if a new instance will be initialized, check, if DB-connection is alread available
		if((Municipality.global_con_listofstreets != null) && (Municipality.global_stmt != null )) {
			System.out.println("in constructor of Municipality Abbruch, weil db-instanzen scon definiert ");
			return;
		}
		
		
		System.out.println("static-Variable is not useable, so establish new session ...");
		try {
				//Connection of own project-specific DB
			String url_listofstreets = configuration.db_application_url;
			global_con_listofstreets = DriverManager.getConnection(url_listofstreets, configuration.db_application_username, configuration.db_application_password);
			global_stmt = global_con_listofstreets.createStatement();
			System.out.println("hier die getClientInfo innerhalb constructor ==="+global_con_listofstreets.getClientInfo().toString()+"===");
		} catch( SQLException e) {
			e.printStackTrace();
			System.out.println("sqlexception ==="+e.toString()+"===");
			System.out.println("return bei sqlexception");
			return;
		}
	}

	public void hallo(){
		try {
			System.out.println("hier die getClientInfo innerhalb hallo ==="+global_con_listofstreets.getClientInfo().toString()+"===");
		} catch( SQLException e) {
			e.printStackTrace();
			System.out.println("sqlexception ==="+e.toString()+"===");
			System.out.println("return bei sqlexception");
			return;
		}
	}
	
	public void save(					String		country,
												String		municipality_name
											) {
		System.out.println("return empty fkt. import_municipality");
		return;
	}


	public void save(		String		country,
												String		municipality_name,
												String		municipality_administrationid,
												String		municipality_osmrelationid,
												String[]		strassen,
												String[]		streetsuburbs,
												Integer		strassenanzahl,
												String		source_url,
												String		password_status,
												String		source_text,
												String 		source_deliverydate,
												String		source_source, 
												String		source_filedate
											) {
		boolean update_force = false;	// just for debugging. Force update of list into DB, regardless of timestamp between wiki article date and last db import date

		System.out.println("beginn import_municipality ...");
		try {
			java.util.Date method_starttime = new java.util.Date();
			Statement stmt = global_con_listofstreets.createStatement();
				// Hol country_id. Wenn nicht vorhanden, erzeuge Datensatz und hol dann die country_id
			long country_id = -1L;
			String sqlbefehl_country = "SELECT id FROM country WHERE country = '"+country+"'";
			//System.out.println("Select-Anfrage ==="+selectbefehl+"=== ...");
			ResultSet rs_country = stmt.executeQuery( sqlbefehl_country );
			if( rs_country.next() ) {
				country_id =rs_country.getLong("id");
			} else {
				stmt.executeUpdate( "INSERT INTO country (country) VALUES ('"+country+"');");
				rs_country = stmt.executeQuery( sqlbefehl_country );
				if( rs_country.next() ) {
					country_id =rs_country.getLong("id");
				}
			}
			//rs_country.close();
			System.out.println("Info: am Ende select from country, id ==="+country_id+"=== select anfrage war ==="+sqlbefehl_country+"===");
	
				// Hol municipality_id. Wenn nicht vorhanden, erzeuge Datensatz und hol dann die municipality_id
			long municipality_id = -1L;

//prod			String sqlbefehl_municipality = "SELECT id, sourcelist_url, sourcelist_filedate  FROM municipality WHERE country_id = "+country_id+" AND name like '%"+municipality_name+"%' ";
// hack fuer get_streetlist_from_liststreetwiki - unterschiedliche Namenschreibweisen
// temp to get more matches during initial mass import
String temp_muniname_reduced = "";
if(	(municipality_name.indexOf("(") != -1) &&
	(municipality_name.indexOf(")") != -1)) {
	temp_muniname_reduced = municipality_name.substring(0, municipality_name.indexOf("(")-1);
	temp_muniname_reduced = temp_muniname_reduced.trim();
}
			String sqlbefehl_municipality = "SELECT id, sourcelist_url, sourcelist_filedate  FROM municipality WHERE ";
			sqlbefehl_municipality += "country_id = "+country_id+" AND ";
sqlbefehl_municipality += " ( ";
			sqlbefehl_municipality += "levenshtein(name,'"+municipality_name+"') < 20 ";
if( ! temp_muniname_reduced.equals(""))
	sqlbefehl_municipality += "OR levenshtein(name,'"+temp_muniname_reduced+"') < 6 ";
sqlbefehl_municipality += " ) ";
			if( ! municipality_administrationid.equals(""))
				sqlbefehl_municipality += "AND officialkeys_id = '"+municipality_administrationid+"';";
			else if( ! municipality_osmrelationid.equals(""))
				sqlbefehl_municipality += "AND osm_relation_id = '"+municipality_osmrelationid+"';";
			else {
				System.out.println("ERROR ERROR: neither administrationid nor osm_relation_id were set, so insert of municipality failed !!!");
				stmt.close();
				//con_listofstreets.close();
				System.out.println("return bei neither administrationid nor osm_relation_id were set");
				return;
			}

			DateFormat time_formatter_iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"); 
			DateFormat time_formatter_iso8601z = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); 
			
			Integer act_import_prio = 0;
			if(source_url.equals("local-archive"))
				act_import_prio = 1;
			if(source_url.equals("local-file"))
				act_import_prio = 2;
			if(source_url.equals("local-active"))
				act_import_prio = 3;
			if(source_url.indexOf("http://") == 0)
				act_import_prio = 4;

			System.out.println("Info: select query for searching in municipality ==="+sqlbefehl_municipality+"===");
			ResultSet rs_municipality = stmt.executeQuery( sqlbefehl_municipality );
			if( rs_municipality.next() ) {
				municipality_id = rs_municipality.getLong("id");

				String list_source = rs_municipality.getString("sourcelist_url");
				Integer act_exist_prio = 0;
				if(list_source.equals("local-archive"))
					act_exist_prio = 1;
				if(list_source.equals("local-file"))
					act_exist_prio = 2;
				if(list_source.equals("local-active"))
					act_exist_prio = 3;
				if(list_source.indexOf("http://") == 0)
					act_exist_prio = 4;

				if( act_import_prio < act_exist_prio ) {
					System.out.println("CAUTION: ignore actual municipality ==="+municipality_name+"===, because it already exists and has higher priority ==="+list_source+"===");
					//rs_municipality.close();
					stmt.close();
					//con_listofstreets.close();
					System.out.println("return bei ignore already exists");
					return;
				}

				if( ! source_filedate.equals("") && 
					 	( rs_municipality.getString("sourcelist_filedate") != null) && 
					 	( ! rs_municipality.getString("sourcelist_filedate").equals(""))) {

					long time = 0L;

					String local_time_to_parse = "";
					java.util.Date local_wiki_time = null;
					java.util.Date local_db_time = null;
					try {
						local_time_to_parse = source_filedate;
//						DateFormat time_formatter_iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
//						                                                         "2012-09-28T00:01:02Z
						if(local_time_to_parse.indexOf("Z") != -1)
							local_wiki_time = time_formatter_iso8601z.parse(local_time_to_parse);
						else
							local_wiki_time = time_formatter_iso8601.parse(local_time_to_parse);
						System.out.println("local_wiki_time ==="+local_wiki_time.toString()+"===");
						local_time_to_parse = rs_municipality.getString("sourcelist_filedate");
						if(local_time_to_parse.indexOf("Z") != -1)
							local_db_time = time_formatter_iso8601z.parse(local_time_to_parse);
						else
							local_db_time = time_formatter_iso8601.parse(local_time_to_parse);
						System.out.println("local_db_time ==="+local_db_time.toString()+"===");
						long diff_ms = local_wiki_time.getTime()-local_db_time.getTime();
						System.out.println("Zeit-diffenz mit parsing: diff wiki to db in ms ==="+diff_ms+"===");
						System.out.println(" (Forts.) in sec: "+diff_ms/60+"   in min: "+diff_ms/60/60);
						if(time != local_wiki_time.getTime()-local_db_time.getTime()) {
							time = local_wiki_time.getTime()-local_db_time.getTime();
							System.out.println("sorry, vertrauen zur neuen Diff-Methode größe, time neu gesetzt, jetzt Zeit-Differenz in msek: " + time );
						}
					}
					catch (Exception e) {
						System.out.println("ERROR: failed to parse timestamp ==="+local_time_to_parse+"===");
						e.printStackTrace();
						//rs_municipality.close();
						stmt.close();
						//con_listofstreets.close();
						System.out.println("return bei faild to parse timestamp");
						return;
					}
					if((time <= 1000) && (! update_force)) {
						System.out.println("CAUTION: ignore actual municipality ==="+municipality_name+"===, because the Timestamp of actual import ==="+source_filedate+"=== is not newer than the one in database ==="+rs_municipality.getString("sourcelist_filedate")+"===, Diff in msek ==="+time+"=== (tolaernace 1000ms)");
						//rs_municipality.close();
						stmt.close();
						//con_listofstreets.close();
						System.out.println("return bei ignoree because timestamp");
						return;
					}
				}

				String updatesqlstring = "UPDATE municipality SET name = '"+municipality_name+"'";
				updatesqlstring += ", officialkeys_id = '"+municipality_administrationid+"'";
				updatesqlstring += ", osm_relation_id = '"+municipality_osmrelationid+"'";
				updatesqlstring += ", sourcelist_url = '"+source_url+"'";
				updatesqlstring += ", sourcelist_text = '" + source_text + "' ";
				updatesqlstring += ", sourcelist_passwordstatus = '" + password_status + "' ";
				updatesqlstring += ", sourcelist_deliverydate = '" + source_deliverydate + "' ";
				updatesqlstring += ", sourcelist_source = '" + source_source + "' ";
				updatesqlstring += ", sourcelist_filedate = '" + source_filedate + "' ";
				updatesqlstring += ", polygon_state = 'old'";
				updatesqlstring += " WHERE id = "+municipality_id + ";";

				System.out.println("updatesqlstring ==="+updatesqlstring+"===");
				stmt.executeUpdate( updatesqlstring );
				rs_municipality = stmt.executeQuery( sqlbefehl_municipality );
//TODO einfache alle strassen löschen ist zu aggresiv. evtl. können die alten Auswertungen nicht komplett angzeigt werden,
//		und es gehen u.a. bei fehlenden Straßen evtl. schon geholte google-Koordinaten verloren
				String deletesqlstring = "DELETE FROM street WHERE municipality_id = "+municipality_id+";";
				System.out.println("sql-delete statement for all streets of actual municipality-ID: "+municipality_id+"     ==="+deletesqlstring+"===");
				stmt.executeUpdate( deletesqlstring );
				rs_municipality = stmt.executeQuery( sqlbefehl_municipality );
				System.out.println("streets deleted.");
			} // end of municipilaty already exists - if( rs_municipality.next() ) { 
			else {
				String sqlinsert_string = "INSERT INTO municipality (country_id, name, officialkeys_id, osm_relation_id, ";
				sqlinsert_string += "sourcelist_url, sourcelist_text, sourcelist_passwordstatus, ";
				sqlinsert_string += "sourcelist_deliverydate, sourcelist_source, sourcelist_filedate, ";
				sqlinsert_string += "polygon_state) ";
				sqlinsert_string += "VALUES ("+country_id+",'"+municipality_name+"','"+municipality_administrationid+"','"+municipality_osmrelationid+"',";
				sqlinsert_string += "'"+source_url+"', '"+source_text+"', '"+password_status+"', ";
				sqlinsert_string += "'"+source_deliverydate+"', '"+source_source+"', '"+source_filedate+"', ";
				sqlinsert_string += "'missing');";
				System.out.println("Insert municipality string ==="+sqlinsert_string+"===");
				try {
					stmt.executeUpdate( sqlinsert_string );
					rs_municipality = stmt.executeQuery( sqlbefehl_municipality );
					if( rs_municipality.next() ) {
						municipality_id =rs_municipality.getLong("id");
					}
				}
				catch( SQLException e) {
					e.printStackTrace();
					System.out.println("FEHLERHAFTER Insert-Befehl sqlinsert_string ==="+sqlinsert_string+"===");
					System.out.println(" Forts. FEHLER: municipality_name ==="+municipality_name+"===");
				}
			}

			if(( country_id == -1) || (municipality_id == -1))
			{
				System.out.println("FEHLER FEHLER: country_id oder municipality_id konnte nicht ermittelt werden, ABBRUCH");
				//rs_municipality.close();
				stmt.close();
				//con_listofstreets.close();
				System.out.println("return bei country_id oder muni_id fehlend");
				return;
			}
		
		
			Integer streets_imported = 0;
				// to enable transaction mode, disable auto-transation mode for every db-action
			global_con_listofstreets.setAutoCommit(false);
			for(Integer strassenindex=0;strassenindex<strassenanzahl;strassenindex++) {
				String strasse = strassen[strassenindex];
				String municipality_addition = streetsuburbs[strassenindex];
		
				if(strasse.indexOf("'") != -1)
					strasse = replace(strasse,"'","''");

				String insertbefehl_street = "INSERT INTO street (country_id,municipality_id,name,municipality_addition)";
				insertbefehl_street += " VALUES ("+country_id+","+municipality_id+",'"+strasse+"','"+municipality_addition+"');";
				//System.out.println("insertbefehl_street ==="+insertbefehl_street+"===");
				if((streets_imported % 100) == 0)
					System.out.println("  beim importieren Zwischenmeldung bei # "+streets_imported+"  ist konkret ==="+strasse+"===");
				try {
					stmt.executeUpdate( insertbefehl_street );
					streets_imported++;
				}
				catch( SQLException e) {
					e.printStackTrace();
					System.out.println("FEHLERHAFTER Insert-Befehl insertbefehl_street ==="+insertbefehl_street+"===");
					System.out.println(" Forts. FEHLER: municipality_name ==="+municipality_name+"=== strasse ==="+strasse+"===");
					System.out.println(" Forts.                    Straße ==="+strasse+"===     Zusatz ==="+municipality_addition+"===");
				}
			}
				// transaction commit
			global_con_listofstreets.commit();
				// re-activate standard auto-transation mode for every db-action
			global_con_listofstreets.setAutoCommit(true);
			System.out.println(" inserted Streets for municipality ==="+municipality_name+"=== Streets to import: "+strassenanzahl+"   real imported: "+streets_imported);
			//rs_municipality.close();
			stmt.close();
			//con_listofstreets.close();
			java.util.Date method_endtime = new java.util.Date();
			System.out.println("Methode save Dauer in msek: "+(method_endtime.getTime()-method_starttime.getTime()));		// in sek: /1000
			System.out.println("return regulaeres ende");
			return;
		}
		catch( SQLException e) {
			e.printStackTrace();
			System.out.println("sqlexception ==="+e.toString()+"===");
			System.out.println("return bei sqlexception");
			return;
		}
	}



	static String replace(String sourcestring, String searchstring, String replacestring)
	{
		String outputstring = sourcestring;

		if(sourcestring.contains(searchstring)) {
			Pattern pattern = Pattern.compile(searchstring);
			//System.out.println("searchstring==="+searchstring+"=== pattern ==="+pattern.toString()+"===");
			Matcher match = pattern.matcher(sourcestring);
			StringBuffer sb = new StringBuffer();
			boolean match_find = match.find();
			while(match_find) {
				match.appendReplacement(sb,replacestring);
				match_find = match.find();
			}
			match.appendTail(sb);
			outputstring = sb.toString();
		}
		return outputstring;
	}

}
