package de.regioosm.listofstreetscore.util;
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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.activemq.util.IOHelper;		// external functions for filesystem io, here toFileSystemSafeName necessary

import de.regioosm.listofstreetscore.util.LogMessage;
import de.regioosm.listofstreetscore.util.Officialstreetlist_Filereader;
import de.regioosm.listofstreetscore.util.GermanyOfficialkeys;
import de.regioosm.listofstreetscore.util.Mediawiki_streetlist_article;
import de.regioosm.listofstreetscore.util.Streetlist_from_streetlistwiki;



public class Updater_central_streetlist {
	private final int INPUTFILE_COLUMN_UNSET = -1;

	private String inputdirectory = "";
	private List<String> filelist = new ArrayList<String>();
	private String output_municipality_directory = "wikineu";
	private String output_wiki_directory = "wikiaktuell";
	private List<String> Wiki_ImporterIgnorelist = new ArrayList<String>();

	private String countryname = "Bundesrepublik Deutschland";
	private String copyright = "";
	private String useagetext = "";
	private String sourceurl = "";
	private String filedate = "";
	private String content_date = "";
	private String wiki_pageupdatesummary = "";
	private String wiki_passwordstate = "";
	private String municipalities_identify_adminhierarchy = "";
	private String destination = "";

/*	private int inputfile_column_municipalityname = INPUTFILE_COLUMN_UNSET;
	private int inputfile_column_id = INPUTFILE_COLUMN_UNSET;
	private int inputfile_column_id_first = INPUTFILE_COLUMN_UNSET;
	private int inputfile_column_id_last = INPUTFILE_COLUMN_UNSET;
	private int inputfile_column_streetname = INPUTFILE_COLUMN_UNSET;
	private int inputfile_column_suburbname = INPUTFILE_COLUMN_UNSET;
	private int inputfile_column_filter = INPUTFILE_COLUMN_UNSET;						// special column to identify and analyse in Thüringen central streetlist
	private String inputfile_column_filtercontentmustbe = "";							// special column content to filter positive in Thüringen central streetlist
	private int inputfile_min_columnnumber_for_active_lines = INPUTFILE_COLUMN_UNSET;
	private String inputfile_header_lastline = "";
	private String inputfile_columnsepartor = ";";
*/
	private boolean justsimulateimport = false;
	
	static Applicationconfiguration configuration = new Applicationconfiguration();
	static Connection con_listofstreets = null;
	
	/**
	 * @return the inputdirectory
	 */
	public String getInputdirectory() {
		return inputdirectory;
	}


	/**
	 * @param inputdirectory the inputdirectory to set
	 */
	public void setInputdirectory(String inputdirectory) {
		this.inputdirectory = inputdirectory;
	}


	/**
	 * @return the filelist
	 */
	public List<String> getFilelist() {
		return filelist;
	}


	/**
	 * @param filelist the filelist to add a single file
	 */
	public void addFile(String filename) {
		this.filelist.add(filename);
	}


	/**
	 * @return the output_municipality_directoy
	 */
	public String getOutput_municipality_directory() {
		return output_municipality_directory;
	}


	/**
	 * @param output_municipality_directoy the output_municipality_directoy to set
	 */
	public void setOutput_municipality_directory(
			String output_municipality_directory) {
		this.output_municipality_directory = output_municipality_directory;
	}


	/**
	 * @return the output_wiki_directoy
	 */
	public String getOutput_wiki_directory() {
		return output_wiki_directory;
	}


	/**
	 * @param output_wiki_directoy the output_wiki_directoy to set
	 */
	public void setOutput_wiki_directory(String output_wiki_directory) {
		this.output_wiki_directory = output_wiki_directory;
	}


	/**
	 * @return the wiki_importerlist
	 */
	public List<String> getWiki_ImporterIgnorelist() {
		return Wiki_ImporterIgnorelist;
	}


	/**
	 * @param wiki_importerlist the wiki_importerlist to add
	 */
	public void addWiki_ImporterIgnore(String Wiki_ImporterIgnore) {
		this.Wiki_ImporterIgnorelist.add(Wiki_ImporterIgnore);
	}



	/**
	 * @return the filedate
	 */
	public String getFiledate() {
		return filedate;
	}


	/**
	 * @param filedate the filedate to set
	 */
	public void setFiledate(String filedate) {
		this.filedate = filedate;
	}


	/**
	 * @return the content_date
	 */
	public String getContent_date() {
		return content_date;
	}


	/**
	 * @param content_date the content_date to set
	 */
	public void setContent_date(String content_date) {
		this.content_date = content_date;
	}


	/**
	 * @return the wiki_pageupdatesummary
	 */
	public String getWiki_pageupdatesummary() {
		return wiki_pageupdatesummary;
	}


	/**
	 * @param wiki_pageupdatesummary the wiki_pageupdatesummary to set
	 */
	public void setWiki_pageupdatesummary(String wiki_pageupdatesummary) {
		this.wiki_pageupdatesummary = wiki_pageupdatesummary;
	}


	/**
	 * @return the wiki_passwordstate
	 */
	public String getWiki_passwordstate() {
		return wiki_passwordstate;
	}


	/**
	 * @param wiki_passwordstate the wiki_passwordstate to set
	 */
	public void setWiki_passwordstate(String wiki_passwordstate) {
		this.wiki_passwordstate = wiki_passwordstate;
	}


	/**
	 * @return the municipalities_identify_adminhierarchy
	 */
	public String getMunicipalities_identify_adminhierarchy() {
		return municipalities_identify_adminhierarchy;
	}

	/**
	 * @return the destination
	 */
	public String getDestination() {
		return destination;
	}

	/**
	 * @param destination
	 */
	public void setDestination(String destination) {
		if(!destination.equals("db") && !destination.equals("wiki"))
			System.out.println("ERROR: unknown set of Destination: not 'db' and not 'wiki !!!");
		else
		this.destination = destination;
	}


	public Updater_central_streetlist() {
			// NICHT User DSeifert ergänzen, damit wurden offenbar falsche Updates zurückgesetzt
		Wiki_ImporterIgnorelist.add(configuration.streetlistwiki_importeruseraccount);
		Wiki_ImporterIgnorelist.add("Wikiadmin");

		try {
					//Connection of own project-specific DB
			String url_listofstreets = configuration.db_application_url;
			con_listofstreets = DriverManager.getConnection(url_listofstreets, configuration.db_application_username, configuration.db_application_password);
		
		} // end of try to open printbuffers - try {
		catch( SQLException e) {
			System.out.println("SQLException in de.diesei.listofstreets/germany_officialkeys/get_hierarchy, details follow ...");
			System.out.println(e.toString());
			e.printStackTrace();
			try {
				if(con_listofstreets != null)
					con_listofstreets.close();
			} catch (SQLException errorinerror) {
			}
		} catch (Exception e) {
			System.out.println("Exception in de.diesei.listofstreets/germany_officialkeys/get_hierarchy, details follow ...");
			System.out.println(e.toString());
			e.printStackTrace();
		}
		
	}

	private static boolean storeMunicipalityStreets(String country, String municipality_name, Officialstreetlist_Filereader streetlist) {
		return storeMunicipalityStreets(country, municipality_name, streetlist.getMunicipality_administrationid(), streetlist.getStreets(),
				streetlist.getStreetrefs(), streetlist.getStreetcount(), 
				streetlist.getOfficialsource_copyrighttext(), streetlist.getOfficialsource_useagetext(),
				streetlist.getOfficialsource_url(), streetlist.getOfficialsource_contentdate(), streetlist.getOfficialsource_filedate());
	}

	private static boolean storeMunicipalityStreets(String country, String municipality_name, String ags, String[] streets, String[] streetids, Integer streetcount) {
		return storeMunicipalityStreets(country, municipality_name, ags, streets, streetids, streetcount,
				"", "", "", "", "");
	}
	
	private static boolean storeMunicipalityStreets(String country, String municipality_name, String ags, String[] streets, 
		String[] streetids, Integer streetcount,
		String sourceCopyrighttext, String sourceUseagetext, String sourceUrl, String sourceContentdate, String sourceFiledate) {
		// take standard municipality name at this point as unique name - COULD BE A PROBLEM because of intransparency of this action
		String municipality_nameunique = municipality_name;

		try {
			// If there is only a osm relation id available, get the municipality-record with this id
			// code removed, because didn't find relation-ids in Sven Anders lists

			if( ! ags.equals("")) {					
				String officiakeysSelectSql = "SELECT name, name_unique, hierarchy FROM officialkeys, country WHERE level = 6 AND"
					+ " ags = ? AND officialkeys.country_id = country.id and country = ?;";
				System.out.println("SQL-Query officialkeys ==="+officiakeysSelectSql+"===");
				PreparedStatement officiakeysSelectStmt = con_listofstreets.prepareStatement(officiakeysSelectSql);
				officiakeysSelectStmt.setString(1, ags);
				officiakeysSelectStmt.setString(2, country);
				ResultSet officiakeysSelectRS = officiakeysSelectStmt.executeQuery();
				if(officiakeysSelectRS.next()) {
					if(!municipality_name.equals(officiakeysSelectRS.getString("name"))) {
						System.out.println("found  other spelling of municipality name ===" + officiakeysSelectRS.getString("name") + "=== for search name ===" + municipality_name + "===");
						municipality_name = officiakeysSelectRS.getString("name");
					}
					if((officiakeysSelectRS.getString("name_unique") != null) && ( ! officiakeysSelectRS.getString("name_unique").equals(""))) {
						municipality_nameunique = officiakeysSelectRS.getString("name_unique");
						System.out.println("found unique-municipality-name ==="+municipality_nameunique+"=== for municipality officialkeys_id ==="+ags+"===");
					}
				}
				officiakeysSelectRS.close();
				officiakeysSelectStmt.close();
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
			String municipalityQuerySql = "SELECT id, name, osm_hierarchy FROM municipality"
				+ " WHERE country_id = ? AND officialkeys_id = ?;";
			System.out.println("Info: select query for searching in municipality ===" + municipalityQuerySql + "===");
			PreparedStatement municipalityQueryStmt = con_listofstreets.prepareStatement(municipalityQuerySql);
			municipalityQueryStmt.setLong(1, country_id);
			municipalityQueryStmt.setString(2, ags);
			ResultSet municipalityQueryRS = municipalityQueryStmt.executeQuery();
			if( municipalityQueryRS.next() ) {
				municipality_id = municipalityQueryRS.getLong("id");

				String municipalityUpdateSql = "UPDATE municipality SET"
					+ " officialsource_copyrighttext = ?,"
					+ " officialsource_useagetext = ?,"
					+ " officialsource_url = ?,"
					+ " officialsource_contentdate = ?,"		// MUST BE CASTED TO DATE
					+ " officialsource_filedate = ?"         // MUST BE CASTED TO DATE
					+ " WHERE id = ?;";
				PreparedStatement municipalityUpdateStmt = con_listofstreets.prepareStatement(municipalityUpdateSql);
				municipalityUpdateStmt.setString(1, sourceCopyrighttext);
				municipalityUpdateStmt.setString(2, sourceUseagetext);
				municipalityUpdateStmt.setString(3, sourceUrl);
				municipalityUpdateStmt.setString(4, sourceContentdate);
				municipalityUpdateStmt.setString(5, sourceFiledate);
				municipalityUpdateStmt.setLong(6, municipality_id);
				System.out.println("Update municipality copyright ===" + sourceCopyrighttext + "===,  useage ===" + sourceUseagetext
					+ "=== , url ===" + sourceUrl + "===, contentdate ===" + sourceContentdate + "===, filedate ===" + sourceFiledate
					+ "=== WHERE muni-db-id: " + municipality_id);
				municipalityUpdateStmt.execute();
				try {
					municipalityQueryRS = municipalityQueryStmt.executeQuery();
					if( municipalityQueryRS.next() ) {
						municipality_id = municipalityQueryRS.getLong("id");
					}
				}
				catch( SQLException e) {
					e.printStackTrace();
					System.out.println("FEHLERHAFTER Insert-Befehl sqlinsert_string ===" + municipalityUpdateSql + "===");
					System.out.println(" Forts. FEHLER: municipality_name ==="+municipality_name+"===");
					return false;
				}
				municipalityUpdateStmt.close();
				municipalityQueryRS.close();
			} // end of municipilaty already exists - if( rs_municipality.next() ) { 
			else {
				municipalityQueryRS.close();
				String municipalityInsertSql = "INSERT INTO municipality (country_id, name, officialkeys_id, osm_relation_id, polygon_state)"
					+ " VALUES (?, ?, ?, '', 'missing', ?, ?, ?, ?, ?);";
				PreparedStatement municipalityInsertStmt = con_listofstreets.prepareStatement(municipalityInsertSql);
				municipalityInsertStmt.setLong(1, country_id);
				municipalityInsertStmt.setString(2, municipality_name);
				municipalityInsertStmt.setString(3, ags);
				municipalityInsertStmt.setString(4, sourceCopyrighttext);
				municipalityInsertStmt.setString(5, sourceUseagetext);
				municipalityInsertStmt.setString(6, sourceUrl);
				municipalityInsertStmt.setString(7, sourceContentdate);
				municipalityInsertStmt.setString(8, sourceFiledate);
				System.out.println("Insert municipality country_id ===" + country_id + "===, muni-name ===" + municipality_name
					+ "===, ags ===" + ags + "===, copyright ===" + sourceCopyrighttext + "===,  useage ===" + sourceUseagetext
					+ "=== , url ===" + sourceUrl + "===, contentdate ===" + sourceContentdate + "===, filedate ===" + sourceFiledate
					+ "=== WHERE muni-db-id: " + municipality_id);
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


		PreparedStatement deleteStreetsStmt = null;
		String deleteStreetsSql = "";

		try {
			deleteStreetsSql = "DELETE FROM streetoriginal WHERE municipality_id = ?;";
			deleteStreetsStmt = con_listofstreets.prepareStatement(deleteStreetsSql);
			deleteStreetsStmt.setLong(1, municipality_id);
			System.out.println("deleete streets in streetoriginal   muni-id " + municipality_id + "   sql was ===" + deleteStreetsSql + "===");
			try {
				deleteStreetsStmt.executeUpdate();
			}
			catch( SQLException sqle) {
				System.out.println("FEHLERHAFTER Insert-Befehl insertbefehl_street ===" + deleteStreetsSql + "===");
				sqle.printStackTrace();
			}
			deleteStreetsStmt.close();
		}
		catch( SQLException e) {
			System.out.println("Error occured while tried to delete streets in streetoriginal");
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
				System.out.println("insert streetoriginal   country_id " + country_id + "   muni-id " + municipality_id + "   .street ===" + street + "===   ref ===" + streetref + "===");
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

	
	public boolean update(Officialstreetlist_Filereader streetlist) {
		String				args_mode = "recentchanges";

		if(getDestination().equals("")) {
			System.out.println("ERROR: destination unset, must be set before first update");
		}
			
//boolean just_simulate_import = false;

		Mediawiki_streetlist_article wikiartikel = new Mediawiki_streetlist_article();
		PrintWriter wiki_output_content = null;

		
		String output_municipality_filename = "";
		String messagetext = "";

		java.util.Date program_starttime = new java.util.Date();
		java.util.Date step_starttime = new java.util.Date();
		java.util.Date step_endtime = new java.util.Date();

		long time_read_inputfile = 0l;
		long time_read_wiki = 0l;
		
		try {
		
			Mediawiki_API listofstreetswiki = new Mediawiki_API();
			boolean logged_in = listofstreetswiki.login("http://regio-osm.de/listofstreets_wiki", 
				configuration.streetlistwiki_importeruseraccount, configuration.streetlistwiki_importeruserpassword);
			System.out.println("Ergebniszustand Login: " + logged_in);
			if( ! logged_in) {
				messagetext = "FEHLER-wiki-login";
				new LogMessage(LogMessage.CLASS_ERROR, "updatewiki", "municipality_nameunique", -1L, messagetext);
				return false;
			}

			HashMap<String,String> list_imported_municipalities = new HashMap<String,String>();
			Integer wikianzahl = 0;
			Integer updateanzahl = 0;

			GermanyOfficialkeys germany_officialkeys_object = new GermanyOfficialkeys();
			Streetlist_from_streetlistwiki wiki_streetlist = new Streetlist_from_streetlistwiki();

    		int count_imported_municipalities = 0;
    		int count_notimported_municipalities_identisch = 0;
			int count_notimported_municipalities_editiert = 0;
			int count_notimported_municipalities_bishernichtimwiki = 0;
    		int count_notimported_municipalities_keinestrassen = 0;
    		int count_imported_municipalities_read = 0;
    		int count_municipality_errors = 0;
    		int count_municipality_doubled = 0;
			int count_municipalities_netto_identisch = 0;
			int count_municipalities_netto_unterschiedlich = 0;
			
				// Loop over all input files (MeVo has only 1 central file)
			for(String act_filename : getFilelist()) {
				System.out.println("Verarbeite Datei ==="+act_filename+"===");
				String municipality_nameunique = "";
				streetlist.openFile(getInputdirectory()+"/"+act_filename, StandardCharsets.UTF_8);
					// Loop over all municipalities in input file
				for(Integer muniindex=0;muniindex<30000;muniindex++) {

						// read next municipality
					step_starttime = new java.util.Date();
					if( ! streetlist.read_next_municipality()) {
						System.out.println("Info: end of actual inputfile reached, filename ==="+act_filename+"===");
						break;
					}
					step_endtime = new java.util.Date();
					count_imported_municipalities_read++;
					time_read_inputfile += (step_endtime.getTime()-step_starttime.getTime());

					if(streetlist.getMunicipality_country().equals("Bundesrepublik Deutschland")) {
						System.out.println("***************************************** Gemeindename # "+muniindex+1+" ==="+streetlist.getMunicipality_name()+"===");
						System.out.println("    - gemeindeschluessel ==="+streetlist.getMunicipality_administrationid()+"===");
						municipality_nameunique = germany_officialkeys_object.get_unique_municipalityname(streetlist.getMunicipality_administrationid());
						if(municipality_nameunique.equals("")) {
							count_municipality_errors++;
							messagetext = "Update-Info-ungültigeKommune\t"+streetlist.getMunicipality_administrationid()+"\t"+streetlist.getMunicipality_administrationid();
							new LogMessage(LogMessage.CLASS_WARNING, "updatefehler", "municipality_nameunique", -1L, messagetext);
							continue;
						}
						System.out.println("    - Gemeindenameunique ==="+municipality_nameunique+"===");
						if( list_imported_municipalities.get(streetlist.getMunicipality_administrationid()) == null)
								list_imported_municipalities.put(streetlist.getMunicipality_administrationid(), municipality_nameunique);
						else {
							count_municipality_doubled++;
							System.out.println("ERROR: mehrfache Eintrag fuer listen ags ==="+streetlist.getMunicipality_administrationid()+"===. Bisher schon muni-wert ==="+list_imported_municipalities.get(streetlist.getMunicipality_administrationid())+"===");
							System.out.println(" (forts.) jetzt noch der neue muni wert ==="+municipality_nameunique+"===");
							continue;
						}
					} else {
						municipality_nameunique = streetlist.getMunicipality_name();
					}

					if(streetlist.getStreetcount() == 0) {
						count_notimported_municipalities_keinestrassen++;
						System.out.println("ERROR: in der inputliste sind für die Gemeinde "+municipality_nameunique+" keine Straßen vorhanden, die Gemeinde wird daher ignoriert");
						messagetext = "Update-Info-leereKommune\t"+streetlist.getMunicipality_administrationid()+"\t"+streetlist.getMunicipality_administrationid();
						new LogMessage(LogMessage.CLASS_WARNING, "updateleereliste", "municipality_nameunique", -1L, messagetext);
						continue;
					}
					
					if(getDestination().equals("wiki")) {
						StringBuffer streets = new StringBuffer();
						System.out.println("Anzahl Straßen: "+streetlist.getStreetcount());
						for(Integer strassenindex=0; strassenindex < streetlist.getStreetcount(); strassenindex++) {
							if( ! streets.toString().equals(""))
								streets.append("\n");
							if( ! streetlist.getStreets()[strassenindex].equals("")) {
								streets.append("* " + streetlist.getStreets()[strassenindex]);
							}
						}
//TODO metadate for wiki article must be set - check new officialsource... fields against old sourcelist_... fields
						// generate new wiki pagecontent with actual streetlist version to compare
						String list_newpagecontent = wikiartikel.erstellen(
								streetlist.getMunicipality_country(),										//country of municipality 
							municipality_nameunique,								//unique municipality name 
							streets.toString(),										//all streets in a string, with prefixes "* "
							streetlist.getOfficialsource_copyrighttext(),											//information about data usable
							"",														//String other, 
							"", 													//String chunk,
							configuration.streetlistwiki_importeruseraccount,						//streetlist wiki username 
							getWiki_pageupdatesummary(),							//streetlist wiki page comment for page update 
							streetlist.getMunicipality_administrationid(),			//unique id of municipality in country (in germany AGS gemeindeschluessel) 
							"",														//optionaly osm relation id, if unique id of municipality should not be avaiable 
							getWiki_passwordstate(),								//streetlist wiki information about visibility of wiki page
							getContent_date(),										//information, how actually file content is
							"text"													//output format (xml or text)
						);

						list_newpagecontent = list_newpagecontent.trim();
						output_municipality_filename = getOutput_municipality_directory() + "/" + IOHelper.toFileSystemSafeName(municipality_nameunique) + ".wikiarticle";
						wiki_output_content = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output_municipality_filename),StandardCharsets.UTF_8)));
						wiki_output_content.println(list_newpagecontent);
						wiki_output_content.close();


							// get actual pagecontent from streetlist wiki to compare 
						step_starttime = new java.util.Date();
						String wiki_actual_pagecontent = wiki_streetlist.read(args_mode,municipality_nameunique, streetlist.getMunicipality_country(), "methodoutput");
						step_endtime = new java.util.Date();
						String wiki_actual_pageurl = wiki_streetlist.source_url;
						time_read_wiki += (step_endtime.getTime()-step_starttime.getTime());
						wiki_actual_pagecontent = wiki_actual_pagecontent.trim();
						if(wiki_actual_pagecontent.length() > 5)
							wikianzahl++;
						else {
							messagetext = "fehlendewikipage\t"+municipality_nameunique;
							new LogMessage(LogMessage.CLASS_WARNING, "wikipagefehlt", "municipality_nameunique", -1L, messagetext);
						}


						System.out.println("Status-muniindex: "+muniindex);
						System.out.println("Status-wikianzahl: "+wikianzahl);

							// write actual wiki content to file directory as backup and file compage during application development
						output_municipality_filename = getOutput_wiki_directory() + "/" + IOHelper.toFileSystemSafeName(municipality_nameunique) + ".wikiarticle";
						wiki_output_content = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output_municipality_filename),StandardCharsets.UTF_8)));
						wiki_output_content.println(wiki_actual_pagecontent);
						wiki_output_content.close();

					    boolean import_actual_municipality = true;
					    int wiki_netto_diffbytes = 0;

						if((wiki_streetlist == null) || (wiki_streetlist.strassenanzahl == 0)) {
			    			count_notimported_municipalities_bishernichtimwiki++;
							messagetext = "Update-Info-leerewikiliste\t"+municipality_nameunique+"\t"+wiki_actual_pageurl;
							new LogMessage(LogMessage.CLASS_WARNING, "updatewikineu", "municipality_nameunique", -1L, messagetext);
						} else {
								// get metadata about actual wiki municipality page
							HashMap<String,String> page_metadata = listofstreetswiki.get_page_info(streetlist.getMunicipality_country(), municipality_nameunique);
							for (String key : page_metadata.keySet()) {
							    System.out.println("Metadatum  Key: " + key + ", Value: " + page_metadata.get(key));
							}

							if(wiki_actual_pagecontent.equals(list_newpagecontent)) {
				    			count_notimported_municipalities_identisch++;
								messagetext = "Update-Info-identisch\t"+municipality_nameunique+"\tStraßenlisteninhalt identisch" + "\t"+wiki_actual_pageurl;
								System.out.println(messagetext);
								continue;
							} else {
								int abpos = wiki_actual_pagecontent.indexOf("== Liste ==");
								int bispos = wiki_actual_pagecontent.indexOf("== Quelle ==");
								if(bispos == -1) {
									if(wiki_actual_pagecontent.indexOf("[[Kategorie:Straßenliste]]") != -1)
										bispos = wiki_actual_pagecontent.indexOf("[[Kategorie:Straßenliste]]");
									else
										bispos = wiki_actual_pagecontent.length();
								}
								String wikiaktuell_netto = wiki_actual_pagecontent.substring(abpos,bispos).trim();
	
								abpos = list_newpagecontent.indexOf("== Liste ==");
								bispos = list_newpagecontent.indexOf("== Quelle ==");
								if(bispos == -1) {
									if(list_newpagecontent.indexOf("[[Kategorie:Straßenliste]]") != -1)
										bispos = list_newpagecontent.indexOf("[[Kategorie:Straßenliste]]");
									else
										bispos = list_newpagecontent.length();
								}
								String wikineu_netto = list_newpagecontent.substring(abpos,bispos).trim();
								if(wikiaktuell_netto.equals(wikineu_netto)) {
					    			count_municipalities_netto_identisch++;
									messagetext = "Update-Info-identisch\t"+municipality_nameunique+"\tStraßenlisteninhalt identisch" + "\t"+wiki_actual_pageurl;
									new LogMessage(LogMessage.CLASS_INFORMATION, "updateidentisch", "municipality_nameunique", -1L, messagetext);
					    			System.out.println("Muninetto identisch ==="+municipality_nameunique+"===");
								} else {
					    			count_municipalities_netto_unterschiedlich++;
					    			System.out.println("Muninetto unterschiedlich ==="+municipality_nameunique+"===");
					    			wiki_netto_diffbytes = wikineu_netto.length() - wikiaktuell_netto.length();
					    			if(Math.abs(wiki_netto_diffbytes) < 10)
					    				System.out.println("dateidiff1 ("+wiki_netto_diffbytes+")==="+municipality_nameunique+"===");
					    			else if(Math.abs(wiki_netto_diffbytes) < 30)
					    				System.out.println("dateidiff2 ("+wiki_netto_diffbytes+")==="+municipality_nameunique+"===");
					    			else
						    			System.out.println("dateidiff3 ("+wiki_netto_diffbytes+")==="+municipality_nameunique+"===");
								}

						    	if( (page_metadata.get("user") != null)) {
						    		boolean wiki_importer_made_last_edit = false;
						    		for(String actimporter : getWiki_ImporterIgnorelist()) {
						    			if(page_metadata.get("user").equals(actimporter)) {
						    				wiki_importer_made_last_edit = true;
						    				break;
						    			}
						    		}
						    		if( ! wiki_importer_made_last_edit) {
						    			count_notimported_municipalities_editiert++;
							    		import_actual_municipality = false;
										messagetext = "Update-Info-falscherletzterbearbeiter\t"+municipality_nameunique+"\tUser==="+page_metadata.get("user")+"===   Datum==="+page_metadata.get("timestamp")+"==="+"\t"+wiki_actual_pageurl;
										new LogMessage(LogMessage.CLASS_INFORMATION, "wikiusereditiert", "municipality_nameunique", -1L, messagetext);
										continue;
						    		}
						    	}
							}
						}
				    	
				    	if(import_actual_municipality) {
				    		count_imported_municipalities++;
				    		System.out.println("Import erfolgt für Gemeinde ==="+municipality_nameunique+"===");
							messagetext = "Info-erfolgt\t"+municipality_nameunique+"\t"+wiki_netto_diffbytes+"\t"+wiki_actual_pageurl;
							new LogMessage(LogMessage.CLASS_INFORMATION, "wikiaktualisiert", "municipality_nameunique", -1L, messagetext);
				    	} else {
				    		System.out.println("Import wird unterdrückt für Gemeinde ==="+municipality_nameunique+"===");
				    	}
	
						if(this.isJustsimulateimport()) {
							import_actual_municipality = false;
						}
	
						if(import_actual_municipality) {
							boolean wikipage_updated = listofstreetswiki.action(Mediawiki_API.EDIT_ACTION, streetlist.getMunicipality_country(), municipality_nameunique, list_newpagecontent, getWiki_pageupdatesummary());
							updateanzahl++;
							System.out.println("Status-updateanzahl: "+updateanzahl);
							System.out.println("Ergebniszustand Action: " + wikipage_updated);
							if(wikipage_updated) {
								messagetext = "Update-Info-erfolgt\t"+municipality_nameunique;
								new LogMessage(LogMessage.CLASS_INFORMATION, "updatewiki", "municipality_nameunique", -1L, messagetext);
							} else {
								messagetext = "Update-Warnung-Updatefehler\t"+municipality_nameunique;
								new LogMessage(LogMessage.CLASS_WARNING, "updatewiki", "municipality_nameunique", -1L, messagetext);
							}
						}
					} // end of if(getDestination.equals("wiki")
					else if(getDestination().equals("db")) {
//TODO check, if name or nameunque should be used for storing
//						boolean stored = storeMunicipalityStreets(getCountryname(), municipality_nameunique, 
//							streetlist.getMunicipality_administrationid(), streetlist.getStreets(), streetlist.getStreetrefs(), streetlist.getStreetcount());
						boolean stored = storeMunicipalityStreets(streetlist.getMunicipality_country(), municipality_nameunique, streetlist);
						if(stored) {
							count_imported_municipalities++;
						} else {
							System.out.println("ERROR ERROR: streets for municipality ===" + municipality_nameunique + "=== could not be stored");
						}
						
						
					} else {
						System.out.println("unknown Destination ===" + getDestination() + "===, will not be supported");
					}
				} // end of loop over actual file - for(Integer muniindex=0;muniindex<200;muniindex++) {
				streetlist.closeFile();
			} // end of loop over all files


    		System.out.println("Anzahl importierte Gemeinden:       " + count_imported_municipalities);
    		System.out.println("Anzahl nicht importierte Gemeinden (Inhalt identisch): " + count_notimported_municipalities_identisch);
    		System.out.println("Anzahl nicht importierte Gemeinden (Inhalt editiert): " + count_notimported_municipalities_editiert);
    		System.out.println("Anzahl nicht importierte Gemeinden (keine Straßen): " + count_notimported_municipalities_keinestrassen);
    		System.out.println("Anzahl fehlerhafte Gemeinden:       " + count_municipality_errors);
    		System.out.println("Anzahl doppelte Gemeinden:          " + count_municipality_doubled);
    		System.out.println("Anzahl gelesene Gemeinden:          " + count_imported_municipalities_read);

    		System.out.println("Anzahl bisher noch nicht im Wiki vorhandene Gemeinden: " + count_notimported_municipalities_bishernichtimwiki);
    		System.out.println("Nettoanalyse Anzahl  identisch ==="+count_municipalities_netto_identisch+"===");
    		System.out.println("Nettoanalyse Anzahl unterschiedlich ==="+count_municipalities_netto_unterschiedlich+"===");
    		
    		

			String officiakeysSelectSql = "SELECT name,name_unique, ags FROM officialkeys"
				+ " WHERE hierarchy LIKE ? and level = 6;";
			System.out.println("SQL-Statement officialkeys ==="+officiakeysSelectSql+"===");
			PreparedStatement officiakeysSelectStmt = con_listofstreets.prepareStatement(officiakeysSelectSql);
			officiakeysSelectStmt.setString(1, getMunicipalities_identify_adminhierarchy());
			ResultSet officiakeysSelectRS = officiakeysSelectStmt.executeQuery();
		    while (officiakeysSelectRS.next()) {
		    	String act_name_unique = officiakeysSelectRS.getString("name_unique");
		    	String act_ags = officiakeysSelectRS.getString("ags");
			    if(list_imported_municipalities.get(act_ags) == null) {
			    	System.out.println("in Liste fehlender ags ==="+act_ags+"=== mit muniunique ==="+act_name_unique+"===");
			    	messagetext = "fehlendeMuniinoffiziellerListe\t"+act_name_unique+"\t"+act_ags+"\tStraßenlisteninhalt identisch";
			    	new LogMessage(LogMessage.CLASS_INFORMATION, "fehlendemuniinliste", "act_name_unique", -1L, messagetext);
			    }
			}
		    officiakeysSelectRS.close();
		    officiakeysSelectStmt.close();

		} // end of try to open printbuffers - try {
		catch (FileNotFoundException e) {
			System.out.println("Error occured, file not found at file ==="+output_municipality_filename+"===   will be ignored");
			e.printStackTrace();
		} 
		catch (IOException e) {
			System.out.println("Error occured, i/o fail at file ==="+output_municipality_filename+"===   will be ignored");
			e.printStackTrace();
		}
		catch( SQLException e) {
			System.out.println("SQLException in de.diesei.listofstreets/germany_officialkeys/get_hierarchy, details follow ...");
			System.out.println(e.toString());
			e.printStackTrace();
			try {
				if(con_listofstreets != null)
					con_listofstreets.close();
			} catch (SQLException errorinerror) {
			}
		} catch (Exception e) {
			System.out.println("Exception in de.diesei.listofstreets/germany_officialkeys/get_hierarchy, details follow ...");
			System.out.println(e.toString());
			e.printStackTrace();
		}


		java.util.Date program_endtime = new java.util.Date();
		System.out.println(" net time read inputfile in msek: "+time_read_inputfile);
		System.out.println(" net time read wiki articles in msek: "+time_read_wiki);
		System.out.println("Program time in msek: "+(program_endtime.getTime()-program_starttime.getTime()));

		return true;
	}


	/**
	 * @return the justsimulateimport
	 */
	public boolean isJustsimulateimport() {
		return justsimulateimport;
	}


	/**
	 * @param justsimulateimport the justsimulateimport to set
	 */
	public void setJustsimulateimport(boolean justsimulateimport) {
		this.justsimulateimport = justsimulateimport;
	}


}
	
