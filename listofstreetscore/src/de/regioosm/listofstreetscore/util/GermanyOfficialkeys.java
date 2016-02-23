/*

	V1.0, 18.07.2011, Dietmar Seifert
		* derived from import_gemeindestrassen.java and secondary, get_streetlist_from_osmwiki.java

*/

package de.regioosm.listofstreetscore.util;

import java.sql.*;


public class GermanyOfficialkeys {

	private static Connection con_listofstreets = null;
	private static Applicationconfiguration configuration = new Applicationconfiguration();

	
	public GermanyOfficialkeys( ) {
		System.out.println("beginn constructor of germany_officialkeys ...");
	
			// Code for every new instance must be insert here
			// Code for every new instance must be insert here
			// Code for every new instance must be insert here
	
		
			// if a new instance will be initialized, check, if DB-connection is alread available
		if(con_listofstreets != null) {
			System.out.println("in constructor of germany_officialkeys  abbruch, weil db-instanzen scon definiert ");
			return;
		}

		try {
			//Connection of own project-specific DB
			String url_listofstreets = configuration.db_application_url;
			con_listofstreets = DriverManager.getConnection(url_listofstreets, configuration.db_application_username, configuration.db_application_password);
		}
		catch( SQLException e) {
			System.out.println("SQLException in de.diesei.listofstreets/germany_officialkeys/get_hierarchy, details follow ...");
			System.out.println(e.toString());
			e.printStackTrace();
		}
		System.out.println("end constructor of germany_officialkeys ...");
	}



	public String get_hierarchy( String gemeindeschluessel ) {
		String out_hierarchy = "";

		try {

			String officialkeysSelectSql = "SELECT hierarchy from officialkeys where ags = ?;";
			System.out.println("SQL-Statement officialkeys ===" + officialkeysSelectSql + "===");
			PreparedStatement officialkeysSelectStmt = con_listofstreets.prepareStatement(officialkeysSelectSql);
			officialkeysSelectStmt.setString(1, gemeindeschluessel);
			ResultSet officialkeysSelectRS = officialkeysSelectStmt.executeQuery();
			
			Integer count_officialkeys_records = 0;
			while(officialkeysSelectRS.next()) {
				if((officialkeysSelectRS.getString("hierarchy") != null) &&
					( ! officialkeysSelectRS.getString("hierarchy").equals(""))) {
					count_officialkeys_records++;
					if(count_officialkeys_records != 1) {
						System.out.println(" cont. actual hierarchy is ==="+officialkeysSelectRS.getString("hierarchy")+"===");
						System.out.println(" cont. sql-string was ==="+officialkeysSelectSql+"===");
					} else {
						out_hierarchy = officialkeysSelectRS.getString("hierarchy");
						System.out.println("found hierarchy from table officialkeys ==="+out_hierarchy+"===");
					}
				}
			}
			officialkeysSelectRS.close();
			officialkeysSelectStmt.close();
		}
		catch( SQLException e) {
			System.out.println("SQLException in de.diesei.listofstreets/germany_officialkeys/get_hierarchy, details follow ...");
			System.out.println(e.toString());
			e.printStackTrace();
		}
		return out_hierarchy;
	}



	public String get_unique_municipalityname( String gemeindeschluessel ) {
		String out_uniquename = "";

		try {

			String officialkeysSelectSql = "SELECT name_unique from officialkeys where ags = ?;";
			System.out.println("SQL-Statement officialkeys  ags ===" + gemeindeschluessel + "=== query ist ===" + officialkeysSelectSql + "===");
			PreparedStatement officialkeysSelectStmt = con_listofstreets.prepareStatement(officialkeysSelectSql);
			officialkeysSelectStmt.setString(1, gemeindeschluessel);
			ResultSet officialkeysSelectRS = officialkeysSelectStmt.executeQuery();
			Integer count_officialkeys_records = 0;
			while(officialkeysSelectRS.next()) {
				if((officialkeysSelectRS.getString("name_unique") != null) &&
					( ! officialkeysSelectRS.getString("name_unique").equals(""))) {
					count_officialkeys_records++;
					if(count_officialkeys_records != 1) {
						System.out.println(" cont. actual name_unique is ==="+officialkeysSelectRS.getString("name_unique")+"===");
						System.out.println(" cont. sql-string was ==="+officialkeysSelectSql+"===");
					} else {
						out_uniquename = officialkeysSelectRS.getString("name_unique");
						System.out.println("found name_unique from table officialkeys ==="+out_uniquename+"===");
					}
				}
			}
			officialkeysSelectRS.close();
			officialkeysSelectStmt.close();
		}
		catch( SQLException sqle) {
			System.out.println("SQLException in de.diesei.listofstreets/germany_officialkeys/get_unique_municipalityname, details follow ...");
			System.out.println(sqle.toString());
			sqle.printStackTrace();
		}
		catch( Exception e) {
			System.out.println("Exception in de.diesei.listofstreets/germany_officialkeys/get_unique_municipalityname, details follow ...");
			System.out.println(e.toString());
			e.printStackTrace();
		}
		return out_uniquename;
	}
}
