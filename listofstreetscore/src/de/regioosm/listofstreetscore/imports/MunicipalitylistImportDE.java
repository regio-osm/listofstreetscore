package de.regioosm.listofstreetscore.imports;


import java.nio.charset.StandardCharsets;
import java.sql.*;

import java.io.*;

import de.regioosm.listofstreetscore.util.Applicationconfiguration;


public class MunicipalitylistImportDE {
	static Connection con_listofstreets = null;
	static Applicationconfiguration configuration = new Applicationconfiguration();


	/**
	 * @param args
	 */
	public static void main(String[] args) {

		final String import_country = "Bundesrepublik Deutschland";
		
		for(int lfdnr=0;lfdnr<args.length;lfdnr++) {
			System.out.println("args["+lfdnr+"] ==="+args[lfdnr]+"===");
		}

		if((args.length >= 1) && (args[0].equals("-h"))) {
			System.out.println("-filename: quarterly updated file from Destatis, in csv format, with tabulators");
			return;
		}

		String inputfilename = "";
		if(args.length >= 1) {
			int args_ok_count = 0;
			for(int argsi=0;argsi<args.length;argsi+=2) {
				System.out.println(" args pair analysing #: "+argsi+"  ==="+args[argsi]+"===");
				if(args.length > argsi+1)
					System.out.println("  args #+1: "+(argsi+1)+"   ==="+args[argsi+1]+"===");
				System.out.println("");

				if(args[argsi].equals("-filename")) {
					inputfilename = args[argsi+1];
					args_ok_count += 2;
				}
			}
			if(args_ok_count != args.length) {
				System.out.println("ERROR: not all programm parameters were valid, STOP");
				return;
			}
		}
	
		
		try {
			System.out.println("ok, jetzt Class.forName Aufruf ...");
			Class.forName("org.postgresql.Driver");
			System.out.println("ok, nach Class.forName Aufruf!");
		}
		catch(ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}

		try {
				//Connection of own project-specific DB
			String url_listofstreets = configuration.db_application_url;
			con_listofstreets = DriverManager.getConnection(url_listofstreets, configuration.db_application_username, configuration.db_application_password);
	

			Statement stmt = null;
			Statement stmt_update = null;

			try {
				stmt = con_listofstreets.createStatement();
				stmt_update = con_listofstreets.createStatement();
			}
			catch( SQLException e) {
				System.out.println("ERROR, when tried to create con.createstatment CRITICAL");
				e.printStackTrace();
				return;
			}
	
				// Hol country_id. Wenn nicht vorhanden, Abbruch
			long country_id = -1L;
			String sql_country = "SELECT id FROM country WHERE country = '" + import_country + "'";
			System.out.println("Select-Request to get country_id ===" + sql_country + "=== ...");
			ResultSet rs_country = stmt.executeQuery(sql_country);
			if( rs_country.next() ) {
				country_id =rs_country.getLong("id");
			} else {
				System.out.println("FATAL ERROR: failed to find country in table, sql query was ===" + sql_country + "===");
				System.out.println("STOP EXECUTION OF IMPORT");
				return;
			}
			
			PrintWriter wiki_output_content = null;

			
				//String dateipfadname = "/home/openstreetmap/programme/listofstreets/programs/AuszugGV3QAktuell.csv";
				//String dateipfadname = configuration.application_homedir + "/data/20130630---AuszugGV2QAktuell.txt";
				//String dateipfadname = configuration.application_datadir + "/Destatis/20140120---AuszugGV4QAktuell.csv";
			String dateipfadname = configuration.application_datadir + "/" + inputfilename;
	
			BufferedReader filereader = null;
			try {
				filereader = new BufferedReader(new InputStreamReader(new FileInputStream(dateipfadname),StandardCharsets.UTF_8));		// on server UTF-8, different to local

				System.out.println("Info: opened File Gemeindeverzeichnis for reading ==="+dateipfadname+"===");


				Integer column_satzart = -1;							//in 2014: 0;
				Integer column_textkennzeichen = -1;					//in 2014: 1;
				Integer column_ags_land = -1;							//in 2014: 2;
				Integer column_ags_rb = -1;								//in 2014: 3;
				Integer column_ags_kreis = -1;							//in 2014: 4;
				Integer column_rs_vb = -1;								//in 2014: 5;
				Integer column_ags_gemeinde = -1;						//in 2014: 6;
				Integer column_gemeindename = -1;						//in 2014: 7;
				Integer column_flaechekm2 = -1;							//in 2014: 8;
				Integer column_bevoelkerung = -1;						//in 2014: 9;
				Integer column_gliederungstadtlandnumerisch = -1;		//in 2014: 18;
System.out.println("Header Indexes initially set:        satzart: "+column_satzart);
System.out.println("                             textkennzeichen: "+column_textkennzeichen);
System.out.println("                                    ags_land: "+column_ags_land);
System.out.println("                                      ags_rb: "+column_ags_rb);
System.out.println("                                   ags_kreis: "+column_ags_kreis);
System.out.println("                                       rs_vb: "+column_rs_vb);
System.out.println("                                ags_gemeinde: "+column_ags_gemeinde);
System.out.println("                                gemeindename: "+column_gemeindename);
System.out.println("                                  flaechekm2: "+column_flaechekm2);
System.out.println("                                bevoelkerung: "+column_bevoelkerung);
System.out.println("                gliederungstadtlandnumerisch: "+column_gliederungstadtlandnumerisch);
				String row_id = "";
				String key_ags = "";
				String key_ags1 = "";
				String key_ags2 = "";
				String key_ags3 = "";
				String key_ags4 = "";
				String key_rs = "";
				String key_rs1 = "";
				String municipality = "";
				Float flaechekm2 = -1.0F;
				//String stand = "";
				Integer bevoelkerungszahl = -1;
				Integer gliederungstadtland = 0;
				
				Integer level = 0;
				Integer level_prev = 0;
				String hierarchy_base = "Bundesrepublik Deutschland";
				String hierarchy_element[] = new String[7];
				String hierarchy = "";

				
				
				Integer file_lineno = 0;

				String fileline = "";
					// die input file has a header, so start in this header active mode
				boolean header_aktiv = true;
				Integer header_column_offset = 0;



				while ((fileline = filereader.readLine()) != null) {

					if(fileline == null || fileline.equals("null")) {
						System.out.println("END OF FILE??? received ==="+fileline+"===, File line No was: "+file_lineno);
						break;
					}

					file_lineno++;
					System.out.println("file line #"+file_lineno+"   file_line ==="+fileline+"===");
					if(fileline.equals(""))
						continue;

					if(header_aktiv) {
							// Header ends, when get in actual line starts with "10" tab "01"
						if(fileline.indexOf("10\t\t01") == 0) {
							System.out.println("Header Indexes at end of header:     satzart: "+column_satzart);
							System.out.println("                             textkennzeichen: "+column_textkennzeichen);
							System.out.println("                                    ags_land: "+column_ags_land);
							System.out.println("                                      ags_rb: "+column_ags_rb);
							System.out.println("                                   ags_kreis: "+column_ags_kreis);
							System.out.println("                                       rs_vb: "+column_rs_vb);
							System.out.println("                                ags_gemeinde: "+column_ags_gemeinde);
							System.out.println("                                gemeindename: "+column_gemeindename);
							System.out.println("                                  flaechekm2: "+column_flaechekm2);
							System.out.println("                                bevoelkerung: "+column_bevoelkerung);
							System.out.println("                gliederungstadtlandnumerisch: "+column_gliederungstadtlandnumerisch);

							if(		( column_satzart == -1)
								||	( column_textkennzeichen == -1)
								||	( column_ags_land == -1)
								||	( column_ags_rb == -1)
								||	( column_ags_kreis == -1)
								||	( column_rs_vb == -1)
								||	( column_ags_gemeinde == -1)
								||	( column_gemeindename == -1)
								||	( column_flaechekm2 == -1)
								||	( column_bevoelkerung == -1)
								||	( column_gliederungstadtlandnumerisch == -1)
							) {
								System.out.println("im Dateikopf nicht alle relevanten Spaltenüberschriften gefunden ...");
								System.out.println("  ABBRUCH, bitte Programm an geänderten Dateiaufbau anpassen");
								return;
							}
								
								
							String update_sql_query = "UPDATE officialkeys"
									+	" SET temp_import_state = 'old'"
									+	" WHERE country_id = " + country_id
									+	";";
							System.out.println("SQL-Statement to update import-only column to old-state ===" + update_sql_query + "===");
								// set import-only column to initial state, to find rows after end of import, which are not longer valid
							try {
								stmt_update.executeUpdate( update_sql_query );
							}
							catch( SQLException e) {
								e.printStackTrace();
								System.out.println("Error during set import-only column ===" + update_sql_query + "===");
								System.out.println(e.toString());
							}
							
							header_aktiv = false;
							// start in manual Transaction mode for faster import
							con_listofstreets.setAutoCommit(false);
						} else {
								// Header aktiv
							if(fileline.indexOf("Satz-art") == 0) {
								System.out.println("finde wichtige Dateiheader-Zeile #1 ==="+fileline+"===");
								String[] act_line_columns = fileline.split("\t");
								if(act_line_columns.length < 7) {
									System.out.println("ERROR: less than 8 columns ("+act_line_columns.length+"), line was ==="+fileline+"===  IGNORED");
									continue;
								}
								for(int act_line_columns_index = 0; act_line_columns_index < act_line_columns.length; act_line_columns_index++) {
									if(act_line_columns[act_line_columns_index].equals(""))
										continue;
									String	act_column_normalized = act_line_columns[act_line_columns_index].toLowerCase();
									act_column_normalized = act_column_normalized.replace("-","");
									System.out.println("- Spalte ["+act_line_columns_index+"] ===" + act_column_normalized + "=== normalized");

									if(act_column_normalized.equals("satzart"))
										column_satzart = act_line_columns_index;
									if(act_column_normalized.equals("textkennzeichen"))
										column_textkennzeichen = act_line_columns_index;
									if(act_column_normalized.equals("gemeindename")) {
										column_gemeindename = act_line_columns_index;
									}
									if(act_column_normalized.equals("fläche km2 1)")) {
										column_flaechekm2  = 8;
										column_bevoelkerung = 10;
									}
								}
							} else if (fileline.indexOf("mit Zuordnungsstand") == 0) {
										// mit Zuordnungsstand		Bevölkerung				Post-leit-zahl	Geografische Mittelpunktkoordinaten		Reisegebiete		Grad der Verstädterung								
								System.out.println("finde wichtige Dateiheader-Zeile #2 ==="+fileline+"===");
								String[] act_line_columns = fileline.split("\t");
								if(act_line_columns.length < 8) {
									System.out.println("ERROR: less than 9 columns ("+act_line_columns.length+"), line was ==="+fileline+"===  IGNORED");
									continue;
								}
								for(int act_line_columns_index = 0; act_line_columns_index < act_line_columns.length; act_line_columns_index++) {
									if(act_line_columns[act_line_columns_index].equals(""))
										continue;
									String	act_column_normalized = act_line_columns[act_line_columns_index].toLowerCase();
									act_column_normalized = act_column_normalized.replace("-","");
									System.out.println("- Spalte ["+act_line_columns_index+"] ===" + act_column_normalized + "=== normalized");
								}
							} else if(fileline.indexOf("Land\tRB") == 2) {
								System.out.println("finde wichtige Dateiheader-Zeile #3 ==="+fileline+"===");
								String[] act_line_columns = fileline.split("\t");
								if(act_line_columns.length < 7) {
									System.out.println("ERROR: less than 8 columns ("+act_line_columns.length+"), line was ==="+fileline+"===  IGNORED");
									continue;
								}
								for(int act_line_columns_index = 0; act_line_columns_index < act_line_columns.length; act_line_columns_index++) {
									if(act_line_columns[act_line_columns_index].equals(""))
										continue;
									String	act_column_normalized = act_line_columns[act_line_columns_index].toLowerCase();
									act_column_normalized = act_column_normalized.replace("-","");
									System.out.println("- Spalte ["+act_line_columns_index+"] ===" + act_column_normalized + "=== normalized");

									if(act_column_normalized.equals("land"))
										column_ags_land = act_line_columns_index + header_column_offset;
									if(act_column_normalized.equals("rb"))
										column_ags_rb = act_line_columns_index + header_column_offset;
									if(act_column_normalized.equals("kreis"))
										column_ags_kreis = act_line_columns_index + header_column_offset;
									if(act_column_normalized.equals("vb"))
										column_rs_vb = act_line_columns_index + header_column_offset;
									if(act_column_normalized.equals("gem"))
										column_ags_gemeinde = act_line_columns_index + header_column_offset;
								}
							} else if(fileline.indexOf("auf Grundlage des Zensus") == 0) {
								System.out.println("finde wichtige Dateiheader-Zeile #3 ==="+fileline+"===");
								String[] act_line_columns = fileline.split("\t");
								if(act_line_columns.length < 4) {
									System.out.println("ERROR: less than 8 columns ("+act_line_columns.length+"), line was ==="+fileline+"===  IGNORED");
									continue;
								}
								for(int act_line_columns_index = 0; act_line_columns_index < act_line_columns.length; act_line_columns_index++) {
									if(act_line_columns[act_line_columns_index].equals(""))
										continue;
									String	act_column_normalized = act_line_columns[act_line_columns_index].toLowerCase();
									act_column_normalized = act_column_normalized.replace("-","");
									System.out.println("- Spalte ["+act_line_columns_index+"] ===" + act_column_normalized + "=== normalized");

									if(act_column_normalized.indexOf("schlüssel") != -1)
										//column_gliederungstadtlandnumerisch = act_line_columns_index + header_column_offset;
										column_gliederungstadtlandnumerisch = 19;
								}
							} else {
								System.out.println("überlese irrelevante Dateiheader-Zeile ==="+fileline+"===");
								continue;
							}
								// continue actual header line
							continue;
						}
					}


					
					
						// ========================================================================================================
						// ========================================================================================================
						// here read of file after end-of-header
						// --------------------------------------------------------------------------------------------------------
					
					String[] act_line_columns = fileline.split("\t");
					if(act_line_columns.length < 7) {
						System.out.println("ERROR: less than 8 columns ("+act_line_columns.length+"), line was ==="+fileline+"===  IGNORED");
						continue;
					}
					System.out.println(" col[column_satzart]==="+act_line_columns[column_satzart]+"=== col[column_textkennzeichen]==="+act_line_columns[column_textkennzeichen]+"=== col[column_ags_land]==="+act_line_columns[column_ags_land]+"===");

					row_id = act_line_columns[column_textkennzeichen];


					key_ags1 = act_line_columns[column_ags_land];
					key_ags2 = act_line_columns[column_ags_rb];
					key_ags3 = act_line_columns[column_ags_kreis];
					key_ags4 = act_line_columns[column_ags_gemeinde];
					key_rs1 = act_line_columns[column_rs_vb];
					municipality = act_line_columns[column_gemeindename];

					
					boolean update_actual_row = true;
					flaechekm2 = -1.0F;
					//stand = "";
					bevoelkerungszahl = -1;
					gliederungstadtland = 0;
					try {
						level = Integer.parseInt(act_line_columns[column_satzart].substring(0,1));

						if(	(level == 6) && 
							(act_line_columns.length >= 19)) {
							flaechekm2 = Float.parseFloat(act_line_columns[column_flaechekm2].replace(",","."));
							//stand = act_line_columns[9];
							bevoelkerungszahl = Integer.parseInt(act_line_columns[column_bevoelkerung].replace(" ",""));
							gliederungstadtland = Integer.parseInt(act_line_columns[column_gliederungstadtlandnumerisch]);
						} else {
							System.out.println("no detail information in available in row #" + file_lineno + "===" + fileline + "===");
							update_actual_row = false;
						}
					} catch( NumberFormatException numexception) {
						System.out.println("ERROR ERROR: Parsing of a numeric value failed in row #" + file_lineno + "===" + fileline + "===");
						numexception.printStackTrace();
						update_actual_row = false;
						continue;
					}
					
					// ===========================================================
					//	Ignore line rules
					// -----------------------------------------------------------

					if(row_id.equals("")) {
						if(level > 2) {
							System.out.println("WARNING: column 2 is empty, please check ==="+fileline+"===");
							continue;
						}
					}
					// ignore Adminlevel 50 = Verbandsgemeinde
					//if(level == 5)
					//	continue;
						// ignore Adminlevel 41 = Kreisfreie Stadt
					if(row_id.equals("41")) {
						hierarchy_element[level] = "";						
						continue;
					}
					if(row_id.equals("50")) { // 50= pure Dublette ags für rs
						hierarchy_element[level] = "";						
						continue;
					}
						// ignore info-only line (prefix früher:), its an old admin level, not anymore active
					if(municipality.indexOf("früher:") == 0) {
						System.out.println("Info: ignore line with früher: Prefix ==="+fileline+"===");
						hierarchy_element[level] = "";
						continue;
					}
					if((level == 2) && (municipality.indexOf("Statistische ") == 0)) {
						System.out.println("Info: ignore line with inofficial admin-level (Prefix Statistische ) ==="+fileline+"===");
						hierarchy_element[level] = "";						
						continue;
					}

					// -----------------------------------------------------------
					//	Ignore line rules
					// ===========================================================

					
					// ===========================================================
					//	remove some prefixes and suffixes
					// -----------------------------------------------------------					

					if(municipality.indexOf(",") != -1)		// ", " not exactly enough, because onetime missing suffix space
						municipality = municipality.substring(0,municipality.indexOf(","));

					if(municipality.indexOf("Reg.-Bez. ") != -1)
						municipality = municipality.substring(municipality.indexOf("Reg.-Bez. ")+"Reg.-Bez. ".length());

					// -----------------------------------------------------------
					//	remove some prefixes and suffixes
					// ===========================================================
					
					if(level < level_prev) {
						System.out.println("level: change, from "+level_prev+"  to "+level);
						for(Integer hieri=level;hieri<=(level_prev);hieri++)
							hierarchy_element[hieri] = "";						
					}
					
					// if not lowest level then store name als hierarchy-element
					if( level < 6 )
						hierarchy_element[level] = municipality;

						// build hierarchy downsize, separted with comma  (level-1 = fix Bundesrepublik Deutschland)
					System.out.println("level: set level "+level+" with ==="+municipality+"===");
					hierarchy = hierarchy_base;
					for(Integer hieri=1;hieri<level;hieri++) {
						if((hierarchy_element[hieri] != null) && ( ! hierarchy_element[hieri].equals("")))
							hierarchy += "," + hierarchy_element[hieri];
					}

					key_ags = key_ags1 + key_ags2 + key_ags3 + key_ags4;
					key_rs = key_ags1 + key_ags2 + key_ags3 + key_rs1 + key_ags4;

					System.out.println("* "+municipality+"     "+key_ags+"     "+key_rs);

					String municipality_unique = "";
					if( level == 6 ) {
						if(		(act_line_columns[column_gemeindename].indexOf("gemfr") != -1) ||
								(act_line_columns[column_gemeindename].indexOf("gemeindefreies") != -1)) {
							municipality_unique = act_line_columns[column_gemeindename];	// take original column-entry for unique values
							municipality_unique = municipality_unique.replace("gemfr. Geb.","gemeindefreies Gebiet");
							municipality_unique = municipality_unique.replace("gemfr. Gebiet","gemeindefreies Gebiet");
						} else
							municipality_unique = municipality;
						if((hierarchy_element[4] != null) && ( ! hierarchy_element[4].equals("")))
							municipality_unique += " (" + hierarchy_element[4] + ")";
						else {
							System.out.println("ERROR: when tried to set municipality_unique, admin-level 4 is missing. actual municipality ==="+municipality+"===, fileline ==="+fileline+"=== file line no: "+file_lineno);
						}
					}
					if( level == 5 ) {
						key_ags = "";		// bei Level 5 (Amtsverbaende/Samtgemeinden...) gibt es keinen AGS
					}

					
				String insertbefehl_street = "INSERT INTO officialkeys (country_id, name, name_unique, "
					+ "ags, rs, level, hierarchy";
				if(flaechekm2 != -1.0F) {
					insertbefehl_street += ", flaechekm2";
				}
				if(bevoelkerungszahl != -1) {
					insertbefehl_street += ", bevoelkerungszahl";
				}
				if(gliederungstadtland != 0) {
					insertbefehl_street += ", gliederungstadtland";
				}
				insertbefehl_street += ") VALUES "
					+ "(" + country_id + ", '"+municipality+"', '"+municipality_unique+"', "
					+ "'"+key_ags+"', '"+key_rs+"',"+level+",'"+hierarchy+"'";
				if(flaechekm2 != -1.0F) {
					insertbefehl_street += ", " + flaechekm2;
				}
				if(bevoelkerungszahl != -1) {
					insertbefehl_street += ", " + bevoelkerungszahl;
				}
				if(gliederungstadtland != 0) {
					insertbefehl_street += ", " + gliederungstadtland;
				}
				insertbefehl_street += ");";
				System.out.println("insertbefehl_street ==="+insertbefehl_street+"===");
				try {
					stmt.executeUpdate( insertbefehl_street );
				}
				catch( SQLException e) {
					e.printStackTrace();
					System.out.println("FEHLERHAFTER Insert-Befehl insertbefehl_street ==="+insertbefehl_street+"===");
					System.out.println(e.toString());
				}


					level_prev = level;

				} // end of loop-over-alle-filelines - while ((fileline = filereader.readLine()) != null) {

				filereader.close();
				if(!con_listofstreets.getAutoCommit()) {
					con_listofstreets.commit();
					con_listofstreets.setAutoCommit(true);
				}

				// ===========================================================
				// now create unique entries for all municipalities, in column name_unique
				// -----------------------------------------------------------

				// ===========================================================
				// Round 1 - get all rows and add level 4 (Landkreis) to it, if necessary
				// -----------------------------------------------------------
					
					// get all municipalities with their number of identical municipality name
				String updatesql_officialkeys = "UPDATE officialkeys SET name_unique = name WHERE id IN"
					+ " (SELECT o.id AS id"
					+ " FROM officialkeys AS o,"
					+ " (SELECT name, country_id, count(name) AS name_count FROM officialkeys WHERE level = 6"
					+ " AND country_id = " + country_id
					+ " GROUP BY name, country_id ORDER BY count(name) DESC, name) AS g"
					+ " WHERE o.country_id = g.country_id"
					+ " AND level = 6 AND o.name = g.name"
					+ " AND name_count = 1);";
				
				Statement stmt_updateofficialkeys = con_listofstreets.createStatement();
				System.out.println("officialkeys-Update with name_unique=name ==="+updatesql_officialkeys+"===");
				try {
					stmt_updateofficialkeys.executeUpdate( updatesql_officialkeys );
				}
				catch( SQLException f) {
					System.out.println("Error occured during try of update officialkeys with update string ==="+updatesql_officialkeys+"===");
					f.printStackTrace();
					return;
				}
				// -----------------------------------------------------------
				// Round 1 - get all rows and add level 4 (Landkreis) to it, if necessary
				// ===========================================================


				
				// ===========================================================
				// Round 2 - individual corrections
				// -----------------------------------------------------------
				stmt_updateofficialkeys = con_listofstreets.createStatement();
				updatesql_officialkeys = "";
				try {
					updatesql_officialkeys = "UPDATE officialkeys SET name = 'Garding, Kirchspiel', name_unique = 'Garding, Kirchspiel (Nordfriesland)' where rs = '010545417035';";
					System.out.println("officialkeys-Update individual, statement  ==="+updatesql_officialkeys+"===");
					stmt_updateofficialkeys.executeUpdate( updatesql_officialkeys );

					updatesql_officialkeys = "UPDATE officialkeys SET name = 'Garding, Stadt', name_unique = 'Garding, Stadt (Nordfriesland)' where rs = '010545417036';";
					System.out.println("officialkeys-Update individual, statement  ==="+updatesql_officialkeys+"===");
					stmt_updateofficialkeys.executeUpdate( updatesql_officialkeys );
					
					updatesql_officialkeys = "UPDATE officialkeys SET name = 'Neuenkirchen (bei Anklam)', name_unique = 'Neuenkirchen (bei Anklam, Vorpommern-Greifswald)' where rs = '130755553101';";
					System.out.println("officialkeys-Update individual, statement  ==="+updatesql_officialkeys+"===");
					stmt_updateofficialkeys.executeUpdate( updatesql_officialkeys );

					updatesql_officialkeys = "UPDATE officialkeys SET name = 'Neuenkirchen (bei Greifswald)', name_unique = 'Neuenkirchen (bei Greifswald, Vorpommern-Greifswald)' where rs = '130755555102';";
					System.out.println("officialkeys-Update individual, statement  ==="+updatesql_officialkeys+"===");
					stmt_updateofficialkeys.executeUpdate( updatesql_officialkeys );
				
				}
				catch( SQLException f) {
					System.out.println("Error occured during try of update officialkeys record individual with statement ==="+updatesql_officialkeys+"===");
					f.printStackTrace();
					return;
				}
				// -----------------------------------------------------------
				// Round 2 - individual corrections
				// ===========================================================


				filereader.close();
			} catch (FileNotFoundException e) {
		      e.printStackTrace();
		    } catch (IOException e) {
		      e.printStackTrace();
		    }

			con_listofstreets.close();
		}
		catch( SQLException e) {
			e.printStackTrace();
			return;
		}	
	}
}
