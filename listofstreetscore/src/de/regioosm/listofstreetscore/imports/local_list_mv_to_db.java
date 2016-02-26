package de.regioosm.listofstreetscore.imports;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import de.regioosm.listofstreetscore.util.Applicationconfiguration;


public class local_list_mv_to_db {
	static Applicationconfiguration configuration = new Applicationconfiguration();


	private static boolean storeMunicipalityStreets(String country, String municipality_name, String ags, String[] streets, String[] streetids, Integer streetcount) {
		// take standard municipality name at this point as unique name - COULD BE A PROBLEM because of intransparency of this action
		String municipality_nameunique = municipality_name;

		try {
			// If there is only a osm relation id available, get the municipality-record with this id
			// code removed, because didn't find relation-ids in Sven Anders lists

			if( ! ags.equals("")) {					
				String sqlquery_officialkeys = "SELECT name, name_unique, hierarchy FROM officialkeys, country WHERE level = 6 AND ags = '"+ags+"'"
					+ " AND officialkeys.country_id = country.id and country = '" + country + "';";
				System.out.println("SQL-Query officialkeys ==="+sqlquery_officialkeys+"===");
				Statement stmt_officialkeys = con_listofstreets.createStatement();
				ResultSet rs_officialkeys = stmt_officialkeys.executeQuery( sqlquery_officialkeys );
				if(rs_officialkeys.next()) {
					if(!municipality_name.equals(rs_officialkeys.getString("name"))) {
						System.out.println("found  other spelling of municipality name ===" + rs_officialkeys.getString("name") + "=== for search name ===" + municipality_name + "===");
						municipality_name = rs_officialkeys.getString("name");
					}
					if((rs_officialkeys.getString("name_unique") != null) && ( ! rs_officialkeys.getString("name_unique").equals(""))) {
						municipality_nameunique = rs_officialkeys.getString("name_unique");
						System.out.println("found unique-municipality-name ==="+municipality_nameunique+"=== for municipality officialkeys_id ==="+ags+"===");
					}
				}
			} else {
				System.out.println("ERROR: municipality could not be verified due to missing officialkeys-id ");
				return false;
			}
		}
		catch( SQLException e) {
			System.out.println("Error occured while tried to connect to database");
			e.printStackTrace();
			return false;
		}
		if( ! municipality_nameunique.equals(municipality_name)) {
			System.out.println("Warning: change municipality name from file ==="+municipality_name+"=== to unique name ==="+municipality_nameunique+"===");
		}


		long country_id = -1L;
		long municipality_id = -1L;

		try {
			String sqlbefehl_country = "SELECT id FROM country WHERE country = ?";
			PreparedStatement selectqueryStmt = con_listofstreets.prepareStatement(sqlbefehl_country);
			selectqueryStmt.setString(1, country);
			System.out.println("Info: get country id " + sqlbefehl_country);
			ResultSet existingcountryRS = selectqueryStmt.executeQuery();
			if( existingcountryRS.next() ) {
				country_id = existingcountryRS.getLong("id");
			} else {
				System.out.println("ERROR, Country unknown");
				return false;
			}
			existingcountryRS.close();
			selectqueryStmt.close();
		}
		catch( SQLException e) {
			System.out.println("Error occured while query country");
			e.printStackTrace();
			return false;
		}
		

		try {
			String municipalityQuerySql = "SELECT id, name, osm_hierarchy FROM municipality WHERE country_id = ?"
				+ "AND officialkeys_id = ?;";
			System.out.println("Info: select query for searching in municipality ===" + municipalityQuerySql + "===");
			PreparedStatement municipalityQueryStmt = con_listofstreets.prepareStatement(municipalityQuerySql);
			municipalityQueryStmt.setLong(1, country_id);
			municipalityQueryStmt.setString(2, ags);
			ResultSet municipalityQueryRS = municipalityQueryStmt.executeQuery();
			if( municipalityQueryRS.next() ) {
				municipality_id = municipalityQueryRS.getLong("id");
			} // end of municipilaty already exists - if( rs_municipality.next() ) { 
			else {
				String municipalityInsertSql = "INSERT INTO municipality (country_id, name, officialkeys_id, osm_relation_id, polygon_state)"
					+ " VALUES (?, ?, ?, '', 'missing');";
				System.out.println("Insert municipality string ===" + municipalityInsertSql + "===");
				PreparedStatement municipalityInsertStmt = con_listofstreets.prepareStatement(municipalityInsertSql);
				municipalityInsertStmt.setLong(1, country_id);
				municipalityInsertStmt.setString(2, municipality_name);
				municipalityInsertStmt.setString(3, ags);
				municipalityInsertStmt.execute();
				try {
					municipalityQueryRS = municipalityQueryStmt.executeQuery();
					if( municipalityQueryRS.next() ) {
						municipality_id = municipalityQueryRS.getLong("id");
					}
				}
				catch( SQLException e) {
					e.printStackTrace();
					System.out.println("FEHLERHAFTER Insert-Befehl sqlinsert_string ===" + municipalityInsertSql + "===");
					System.out.println(" Forts. FEHLER: municipality_name ==="+municipality_name+"===");
					return false;
				}
				municipalityInsertStmt.close();
			}
			municipalityQueryStmt.close();
			municipalityQueryRS.close();
		}
		catch( SQLException e) {
			System.out.println("Error occured while tried to get municipality");
			e.printStackTrace();
			return false;
		}
		
//TODO complete transaction mode		
		Integer streets_imported = 0;
		String insertStreetSql = "";
		PreparedStatement insertStreetStmt = null;
	
		try {
			for(Integer streetindex = 0; streetindex < streetcount; streetindex++) {
				String street = streets[streetindex];
				String streetref = streetids[streetindex];
	
				insertStreetSql = "INSERT INTO streetoriginal (country_id, municipality_id, name, streetref) VALUES (?, ?, ?, ?);";
				insertStreetStmt = con_listofstreets.prepareStatement(insertStreetSql);
				insertStreetStmt.setLong(1, country_id);
				insertStreetStmt.setLong(2, municipality_id);
				insertStreetStmt.setString(3, street);
				insertStreetStmt.setString(4, streetref);
				System.out.println("insert streetoriginal ===" + insertStreetSql + "===");
	
			
				if((streets_imported % 100) == 0)
					System.out.println("  beim importieren Zwischenmeldung bei # "+streets_imported+"  ist konkret ===" + street + "===");
				try {
					insertStreetStmt.executeUpdate();
					streets_imported++;
				}
				catch( SQLException e) {
					e.printStackTrace();
					System.out.println("FEHLERHAFTER Insert-Befehl insertbefehl_street ===" + insertStreetSql + "===");
					System.out.println(" Forts. FEHLER: municipality_name ===" + municipality_name + "=== strasse ===" + street + "===");
					System.out.println(" Forts.                    Straße ===" + street + "===     Ref ===" + streetref + "===");
				}
			}
			insertStreetStmt.close();
		}
		catch( SQLException e) {
			System.out.println("Error occured while tried insert streets");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	

	static void read_file(String actual_filename)
	{
		BufferedReader filereader = null;

		String[] strassen = new String[30000];
		String[] strassenid = new String[30000];
		String[] streetsuburbs = new String[30000];
		Integer strassenanzahl = 0;

		String municipality_name = "";
		String municipality_submunicipalityname = "";
		String municipality_gemeindeschluessel = "";
		String streetid = "";

		Integer municipality_count = 0;

		String actual_street = "";

		try {

			filereader = new BufferedReader(new InputStreamReader(new FileInputStream(actual_filename),"UTF-8"));

			Integer file_lineno = 0;
			String dateizeile = "";

			while ( (dateizeile = filereader.readLine()) != null ) {

				file_lineno++;
				System.out.println("file line #"+file_lineno+"   file_line ==="+dateizeile+"===");
				if(dateizeile.equals(""))
					continue;
				if(dateizeile.indexOf("#") == 0) {
					System.out.println("ignore header line ==="+dateizeile+"===");
					continue;
				}

				//#id_Gemeinde	Gemeinde_Name	id_Strasse	Strasse_Name	id_Amt	Amt_Name	id_Kreis	Kreis_Name	de:strassenschluessel
				//13003000	Rostock, Hansestadt	00010	1.St.-Jürgen-Straße	0001	kreisfreie Stadt	13003	Hansestadt Rostock	13003000000100010

				String[] act_line_columns = dateizeile.split("\t");

				System.out.println(" aktuelle Dateizeile #"+file_lineno+":  ==="+dateizeile+"===");
				for(int inti=0;inti<act_line_columns.length;inti++)
					System.out.println("Spalte ["+inti+"] ==="+act_line_columns[inti]+"===");


				if(act_line_columns.length < 9) {
					System.out.println("ERROR: less than 9 columns ("+act_line_columns.length+"), line was ==="+dateizeile+"===  IGNORED");
					continue;
				}
		
				if( (! municipality_gemeindeschluessel.equals(act_line_columns[0])) && (! municipality_gemeindeschluessel.equals(""))) {
					System.out.println("Information: changed municipality in line "+file_lineno+"  from gemeindeschluessel ==="+municipality_gemeindeschluessel+"=== to ==="+act_line_columns[0]+"===");
		
					if(strassenanzahl > 0) {
						boolean stored = storeMunicipalityStreets(global_const_countryname, municipality_name, municipality_gemeindeschluessel, strassen, strassenid, strassenanzahl);
						if(stored) {
							municipality_count++;
						} else {
							System.out.println("ERROR ERROR: streets for municipality ===" + municipality_name + "=== could not be stored");
						}
							
				 	} // end of found streets when regionalkey changed: if(strassenanzahl > 0) {
		
					municipality_gemeindeschluessel = "";
					municipality_name = "";
					strassenanzahl = 0;
						// no continue here, because in this actual line first new street of next municipality
				}
					// work with current file line

				//#id_Gemeinde	Gemeinde_Name	id_Strasse	Strasse_Name	id_Amt	Amt_Name	id_Kreis	Kreis_Name	de:strassenschluessel
				//13003000	Rostock, Hansestadt	00010	1.St.-Jürgen-Straße	0001	kreisfreie Stadt	13003	Hansestadt Rostock	13003000000100010

				municipality_gemeindeschluessel = act_line_columns[0];
				municipality_name = act_line_columns[1];
municipality_submunicipalityname = "";
				if(municipality_submunicipalityname.equals(municipality_name))
					municipality_submunicipalityname = "";
				actual_street = act_line_columns[3];
				streetid = act_line_columns[8];

				if(actual_street.indexOf("str.") != -1) {
					actual_street = actual_street.replace("str.","strasse");
					System.out.println("Info: changed Street name because of abbrev str. from ==="+act_line_columns[4]+"=== to ==="+actual_street+"===");
				}
				if(actual_street.indexOf("Str.") != -1) {
					actual_street = actual_street.replace("Str.","Strasse");
					System.out.println("Info: changed Street name because of abbrev Str. from ==="+act_line_columns[4]+"=== to ==="+actual_street+"===");
				}

				strassen[strassenanzahl] = actual_street;
				strassenid[strassenanzahl] = streetid;
				streetsuburbs[strassenanzahl] = municipality_submunicipalityname;
				strassenanzahl++;

				System.out.println("# "+file_lineno+"  mun..name ==="+municipality_name+"=== street ==="+actual_street+"===  gemeindeschluessel ==="+municipality_gemeindeschluessel+"===");
			} // end of reading file complete: while ( (dateizeile = filereader.readLine()) != null ) {


				// After reading file complete: if streets still in buffer, import them now
			if(strassenanzahl > 0) {
				boolean stored = storeMunicipalityStreets(global_const_countryname, municipality_name, municipality_gemeindeschluessel, strassen, strassenid, strassenanzahl);
				if(stored) {
					municipality_count++;
				} else {
					System.out.println("ERROR ERROR: streets for municipality ===" + municipality_name + "=== could not be stored");
				}
		 	} // end of found streets when regionalkey changed: if(strassenanzahl > 0) {


	    } catch (FileNotFoundException e) {
	    	System.out.println("Error occured, file not found at file ==="+actual_filename+"===   will be ignored");
	      e.printStackTrace();
	    } catch (IOException e) {
	    	System.out.println("Error occured, i/o fail at file ==="+actual_filename+"===   will be ignored");
	      e.printStackTrace();
	    }

	}



	static String global_const_countryname = "";
	static String global_const_nutzungserlaubnis = "Ohne Nutzungsbeschränkung, explizit angefragt bei Landesamt für Vermessung und Geoinformation, Erfurt in 09/2011. Daten: http://www.geoportal-th.de/download/catalogs";


	static Connection con_listofstreets = null;

	
	public static void main(String args[]) {

		try {
			System.out.println("ok, jetzt Class.forName Aufruf ...");
			Class.forName("org.postgresql.Driver");
			System.out.println("ok, nach Class.forName Aufruf!");
		}
		catch(ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}

		//global_const_countryname = "Bundesrepublik Deutschland";
		global_const_countryname = "Neu-Meck-Vorp";

		try {
				//Connection of own project-specific DB
			String url_listofstreets = configuration.db_application_url;
			con_listofstreets = DriverManager.getConnection(url_listofstreets, configuration.db_application_username, configuration.db_application_password);

			String inputdateiname = "/home/openstreetmap/NASI/OSMshare/Projekte-Zusammenarbeiten/Straßenlisten-ab-2012-11/Deutschland/Mecklenburg-Vorpommmern-kpl/2016-02-09/Schluesselverz_LiegKat_MV_2015_12_10/Gemeinde_Strasse_Verwaltungsgemeinschaft_Kreis_MOD.csv";

			read_file(inputdateiname);

		} // end of try to open printbuffers - try {
		catch( SQLException e) {
			System.out.println("Error occured while tried to connect to database");
			e.printStackTrace();
			return;
		}
	}
}

