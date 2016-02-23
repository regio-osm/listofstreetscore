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

package de.regioosm.listofstreetscore.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;


/**
 * 
 * @author Dietmar Seifert
 * @version 1.0
 * 
 * Importer for german state wide central streetlists
 *
 */
public class Officialstreetlist_Filereader {

	private final int INPUTFILE_COLUMN_UNSET = -1;
	
//	String column_separator = ";";

	private int inputfile_column_municipalityname = INPUTFILE_COLUMN_UNSET;
	private int inputfile_column_streetref = INPUTFILE_COLUMN_UNSET;
	private int inputfile_column_id = INPUTFILE_COLUMN_UNSET;
	private int inputfile_column_id_first = INPUTFILE_COLUMN_UNSET;
	private int inputfile_column_id_last = INPUTFILE_COLUMN_UNSET;
	private int inputfile_column_streetname = INPUTFILE_COLUMN_UNSET;
	private int inputfile_column_suburbname = INPUTFILE_COLUMN_UNSET;
	private int inputfile_min_columnnumber_for_active_lines = INPUTFILE_COLUMN_UNSET;
	private int inputfile_column_filter = INPUTFILE_COLUMN_UNSET;						// special column to identify and analyse in Thüringen central streetlist
	private String inputfile_column_filtercontentmustbe = "";							// special column content to filter positive in Thüringen central streetlist
	private String inputfile_header_lastline = "";
	private String inputfile_columnsepartor = ";";	
	
	/**
	 * Country of the streetlist, must be set manually
	 */
	private String municipality_country;
	/**
	 * Name of the municipality, as read from inputfile
	 */
	private String municipality_name;
	/**
	 * country wide unique id of the municipality.
	 * 
	 * In Germany, it's the AGS, Amtlicher Gemeindeschlüssel
	 * In Austria, it's thd ÖSTAT, Österreichischer Gemeindeschlüssel
	 */
  	private String municipality_administrationid = "";
  	private String municipality_osmrelationid = "";
	private String[] strassen;
	private String[] streetsuburbs;
	private String[] streetrefs;
	private int strassenanzahl;
	private String source_url = "";
	private String password_status = "";
	private String source_text = "";
	private String source_deliverydate = "";
	private String source_source = "";
	private String source_filedate = "";
	/**
	 * informs call class about already read number of municipalities via calls of read_next_municipality
	 */
	private int number_municipality_already_read = 0;

	
	private static BufferedReader filereader = null;
	private static int file_lineno = 0;
	private static String streetlist_filename = "";
	private static String buffer_first_line_next_muni = "";
	private static boolean header_read = false;


	public Officialstreetlist_Filereader() {
		filereader = null;
	}

	/**
	 * @return the inputfile_column_municipalityname
	 */
	public int getInputfile_column_municipalityname() {
		return inputfile_column_municipalityname;
	}

	/**
	 * @param inputfile_column_municipalityname the inputfile_column_municipalityname to set
	 */
	public void setInputfile_column_municipalityname(
			int inputfile_column_municipalityname) {
		this.inputfile_column_municipalityname = inputfile_column_municipalityname;
	}

	/**
	 * @return the inputfile_column_id
	 */
	public int getInputfile_column_streetref() {
		return inputfile_column_streetref;
	}

	/**
	 * @param inputfile_column_id the inputfile_column_id to set
	 */
	public void setInputfile_column_streetref(int inputfile_column_streetref) {
		this.inputfile_column_streetref = inputfile_column_streetref;
	}
	
	/**
	 * @return the inputfile_column_id
	 */
	public int getInputfile_column_id() {
		return inputfile_column_id;
	}

	/**
	 * @return the inputfile_column_id
	 */
	public int getInputfile_column_id_first() {
		return inputfile_column_id_first;
	}

	/**
	 * @return the inputfile_column_id
	 */
	public int getInputfile_column_id_last() {
		return inputfile_column_id_last;
	}

	/**
	 * @param inputfile_column_id the inputfile_column_id to set
	 */
	public void setInputfile_column_id(int inputfile_column_id) {
		this.inputfile_column_id = inputfile_column_id;
	}

	/**
	 * @param inputfile_column_ids the inputfile_column_id_start and  inputfile_column_id_end to set
	 */
	public void setInputfile_column_ids(int inputfile_column_id_first, int inputfile_column_id_last) {
		this.inputfile_column_id_first = inputfile_column_id_first;
		this.inputfile_column_id_last = inputfile_column_id_last;
	}
	
	/**
	 * @return the inputfile_column_streetname
	 */
	public int getInputfile_column_streetname() {
		return inputfile_column_streetname;
	}

	/**
	 * @param inputfile_column_streetname the inputfile_column_streetname to set
	 */
	public void setInputfile_column_streetname(int inputfile_column_streetname) {
		this.inputfile_column_streetname = inputfile_column_streetname;
	}

	/**
	 * @return the inputfile_column_suburbname
	 */
	public int getInputfile_column_suburbname() {
		return inputfile_column_suburbname;
	}

	/**
	 * @param inputfile_column_suburbname the inputfile_column_suburbname to set
	 */
	public void setInputfile_column_suburbname(int inputfile_column_suburbname) {
		this.inputfile_column_suburbname = inputfile_column_suburbname;
	}

	/**
	 * @return the inputfile_min_columnnumber_for_active_lines
	 */
	public int getInputfile_min_columnnumber_for_active_lines() {
		return inputfile_min_columnnumber_for_active_lines;
	}

	/**
	 * @param inputfile_min_columnnumber_for_active_lines the inputfile_min_columnnumber_for_active_lines to set
	 */
	public void setInputfile_min_columnnumber_for_active_lines(
			int inputfile_min_columnnumber_for_active_lines) {
		this.inputfile_min_columnnumber_for_active_lines = inputfile_min_columnnumber_for_active_lines;
	}

	/**
	 * @return the inputfile_column_filter
	 */
	public int getInputfile_column_filter() {
		return inputfile_column_filter;
	}

	/**
	 * @param inputfile_column_filter the inputfile_column_filter to set
	 */
	public void setInputfile_column_filter(int inputfile_column_filter) {
		this.inputfile_column_filter = inputfile_column_filter;
	}

	/**
	 * @return the inputfile_column_filtercontentmustbe
	 */
	public String getInputfile_column_filtercontentmustbe() {
		return inputfile_column_filtercontentmustbe;
	}

	/**
	 * @param inputfile_column_filtercontentmustbe the inputfile_column_filtercontentmustbe to set
	 */
	public void setInputfile_column_filtercontentmustbe(
			String inputfile_column_filtercontentmustbe) {
		this.inputfile_column_filtercontentmustbe = inputfile_column_filtercontentmustbe;
	}

	/**
	 * @return the inputfile_header_lastline
	 */
	public String getInputfile_header_lastline() {
		return inputfile_header_lastline;
	}

	/**
	 * @param inputfile_header_lastline the inputfile_header_lastline to set
	 */
	public void setInputfile_header_lastline(String inputfile_header_lastline) {
		this.inputfile_header_lastline = inputfile_header_lastline;
	}

	/**
	 * @return the inputfile_columnsepartor
	 */
	public String getInputfile_columnsepartor() {
		return inputfile_columnsepartor;
	}

	/**
	 * @param inputfile_columnsepartor the inputfile_columnsepartor to set
	 */
	public void setInputfile_columnsepartor(String inputfile_columnsepartor) {
		this.inputfile_columnsepartor = inputfile_columnsepartor;
	}

	/**
	 * @return the municipality_country
	 */
	public String getMunicipality_country() {
		return municipality_country;
	}

	/**
	 * @param municipality_country the municipality_country to set
	 */
	public void setMunicipality_country(String municipality_country) {
		this.municipality_country = municipality_country;
	}

	/**
	 * @return the municipality_name
	 */
	public String getMunicipality_name() {
		return municipality_name;
	}

	/**
	 * @param municipality_name the municipality_name to set
	 */
	private void setMunicipality_name(String municipality_name) {
		this.municipality_name = municipality_name;
	}

	/**
	 * @return the municipality_administrationid
	 */
	public String getMunicipality_administrationid() {
		return municipality_administrationid;
	}

	/**
	 * @param municipality_administrationid the municipality_administrationid to set
	 */
	private void setMunicipality_administrationid(
			String municipality_administrationid) {
		this.municipality_administrationid = municipality_administrationid;
	}

	/**
	 * @return the municipality_osmrelationid
	 */
	public String getMunicipality_osmrelationid() {
		return municipality_osmrelationid;
	}

	/**
	 * @param municipality_osmrelationid the municipality_osmrelationid to set
	 */
	private void setMunicipality_osmrelationid(String municipality_osmrelationid) {
		this.municipality_osmrelationid = municipality_osmrelationid;
	}

	/**
	 * @return the streetsuburbs
	 */
	public String[] getStreetsuburbs() {
		return streetsuburbs;
	}

	/**
	 * @param streetsuburbs the streetsuburbs to set
	 */
	private void setStreetsuburbs(String[] streetsuburbs) {
		this.streetsuburbs = streetsuburbs;
	}

	/**
	 * @return the streets
	 */
	public String[] getStreets() {
		return strassen;
	}

	/**
	 * @param streets the streets to set
	 */
	public void setStreets(String[] streets) {
		this.strassen = streets;
	}

	/**
	 * @return the street refs
	 */
	public String[] getStreetrefs() {
		return streetrefs;
	}

	/**
	 * @param streets the streets to set
	 */
	public void setStreetrefs(String[] streetrefs) {
		this.streetrefs = streetrefs;
	}
	
	/**
	 * @return the strassenanzahl
	 */
	public int getStreetcount() {
		return strassenanzahl;
	}

	/**
	 * @param strassenanzahl the strassenanzahl to set
	 */
	private void setStreetcount(int streetcount) {
		this.strassenanzahl = streetcount;
	}

	/**
	 * @return the source_url
	 */
	public String getSource_url() {
		return source_url;
	}

	/**
	 * @param source_url the source_url to set
	 */
	private void setSource_url(String source_url) {
		this.source_url = source_url;
	}

	/**
	 * @return the password_status
	 */
	public String getPassword_status() {
		return password_status;
	}

	/**
	 * @param password_status the password_status to set
	 */
	private void setPassword_status(String password_status) {
		this.password_status = password_status;
	}

	/**
	 * @return the source_text
	 */
	public String getSource_text() {
		return source_text;
	}

	/**
	 * @param source_text the source_text to set
	 */
	private void setSource_text(String source_text) {
		this.source_text = source_text;
	}

	/**
	 * @return the source_deliverydate
	 */
	public String getSource_deliverydate() {
		return source_deliverydate;
	}

	/**
	 * @param source_deliverydate the source_deliverydate to set
	 */
	private void setSource_deliverydate(String source_deliverydate) {
		this.source_deliverydate = source_deliverydate;
	}

	/**
	 * @return the source_source
	 */
	public String getSource_source() {
		return source_source;
	}

	/**
	 * @param source_source the source_source to set
	 */
	private void setSource_source(String source_source) {
		this.source_source = source_source;
	}

	/**
	 * @return the source_filedate
	 */
	public String getSource_filedate() {
		return source_filedate;
	}

	/**
	 * @param source_filedate the source_filedate to set
	 */
	private void setSource_filedate(String source_filedate) {
		this.source_filedate = source_filedate;
	}

	/**
	 * @return the number_municipality_already_read
	 */
	public int getNumber_municipality_already_read() {
		return number_municipality_already_read;
	}

	/**
	 * @param number_municipality_already_read the number_municipality_already_read to set
	 */
	private void setNumber_municipality_already_read(
			int number_municipality_already_read) {
		this.number_municipality_already_read = number_municipality_already_read;
	}


	
	
	/**
	 * 
	 * @param filename		inputfile path and name
	 * @param charset		encoding of inputfile. Take value StandardCharsets.UTF_8 for example
	 * @return				successfully opened file (true) or failed (false)
	 */
	public boolean openFile(String filename, Charset charset) {
		boolean file_successfully_opened = false;
		boolean ready_to_open_file = true;

		if(inputfile_column_municipalityname == INPUTFILE_COLUMN_UNSET) {
			ready_to_open_file = false;
			System.out.println("ERROR: inputfile column municipalityname is not set. Must be set, before openfile. CANCEL");
		}
		if(	(getInputfile_column_id() == INPUTFILE_COLUMN_UNSET)
			&& ((getInputfile_column_id_first() == INPUTFILE_COLUMN_UNSET) || (getInputfile_column_id_last() == INPUTFILE_COLUMN_UNSET))
			)	{
			ready_to_open_file = false;
			System.out.println("ERROR: inputfile column id is not set. Must be set, before openfile. CANCEL");
		}
		if(inputfile_column_streetname == INPUTFILE_COLUMN_UNSET) {
			ready_to_open_file = false;
			System.out.println("ERROR: inputfile column streetname is not set. Must be set, before openfile. CANCEL");
		}

		if( ! ready_to_open_file) {
//ToDo is return enough or should an error been thrown?
			System.out.println("ERROR: missing one or more paramets to identfy columns in inputfile, which must set prior to openfile. CANCEL");
			return false;
		}
		
		file_lineno = 0;
		setNumber_municipality_already_read(0);
		buffer_first_line_next_muni = "";
		streetlist_filename = filename;

		if((charset == null) || charset.equals(""))
			charset = StandardCharsets.UTF_8;
		try {
			filereader = new BufferedReader(new InputStreamReader(new FileInputStream(filename),charset));
			file_successfully_opened = true;
	    } catch (FileNotFoundException e) {
	    	System.out.println("Error occured, file not found at file ==="+filename+"===   will be ignored");
	      e.printStackTrace();
			file_successfully_opened = false;
	    }
		return file_successfully_opened;
	}
	

	/**
	 * 
	 * @return	return state, if file was opened previously and had been closed correctly
	 */
	public boolean closeFile() {
		boolean file_successfully_closed = false;

		try {
			filereader.close();
			file_successfully_closed = true;
	    } catch (FileNotFoundException e) {
	    	System.out.println("Error occured, file not found at file ==="+streetlist_filename+"===   will be ignored");
	      e.printStackTrace();
			file_successfully_closed = false;
	    } catch (IOException e) {
	    	System.out.println("Error occured, i/o fail at file ==="+streetlist_filename+"===   will be ignored");
	    	e.printStackTrace();
			file_successfully_closed = false;
	    }
		return file_successfully_closed;
	}

	/**
	 * 
	 * @return	reads the next municipality, if one more is available in inputfile (true). If inputfile has been read completly it returns false
	 */
	public boolean read_next_municipality()
	{
//ToDo   strassen und streetsuburbs in List<String> umdefinieren
		String[] strassen = new String[15000];
		String[] streetrefs = new String[15000];
		String[] streetsuburbs = new String[15000];
		int strassenanzahl = 0;

		String municipality_name = "";
		String municipality_submunicipalityname = "";
		String municipality_gemeindeschluessel = "";

		String actual_street = "";
		String actual_streetref = "";
		String dateizeile = "";

		try {
				// if not first line of file, then possible a file record is in buffer from last readline
			if(buffer_first_line_next_muni.equals("")) {
				System.out.println("ok, start of next muni. buffer is empty ...");
				if((dateizeile = filereader.readLine()) == null)
					return false;
				System.out.println("ok, because of empty buffer, read next file records content ==="+dateizeile+"===");
			} else { 
				dateizeile = buffer_first_line_next_muni;
				buffer_first_line_next_muni = "";		// ok, buffer will be used, then empty it, for only one-time using
				file_lineno--;	// correct number of read file records
				System.out.println("ok, start of next muni. found full buffer ==="+buffer_first_line_next_muni+"===");
			}



			if((inputfile_header_lastline == null) || inputfile_header_lastline.equals(""))
				header_read = true;
			do {
					//muesste ans loop ende und hier am anfang puffern erste zeile nächste muni, weil diese ja gelesen wird, um das ende der aktuelle muni festzustellen
					// diese erste neue muni-zeile muss dann für die nächste muni auswertung genommen werden
				file_lineno++;
				System.out.println("file line #"+file_lineno+"   file_line ==="+dateizeile+"===");
				if(dateizeile.equals(""))
					continue;
				if( ! header_read) {
					if(dateizeile.indexOf(getInputfile_header_lastline()) == 0) {
						System.out.println("ignore header line ==="+dateizeile+"===");
						header_read = true;
					}
					continue;
				}

				String[] act_line_columns = dateizeile.split(getInputfile_columnsepartor());

				System.out.println(" aktuelle Dateizeile #"+file_lineno+":  ==="+dateizeile+"===");
				for(int inti=0;inti<act_line_columns.length;inti++)
					System.out.println("Spalte ["+inti+"] ==="+act_line_columns[inti]+"===");

				if(act_line_columns.length < getInputfile_min_columnnumber_for_active_lines()) {
					System.out.println("WARNING: less than minimum number ("+getInputfile_min_columnnumber_for_active_lines()+") in actual columns ("+act_line_columns.length+"), line was ==="+dateizeile+"===  IGNORED");
					continue;
				}

				String actline_municipalityid = "";
				if(getInputfile_column_id() != INPUTFILE_COLUMN_UNSET) {
					actline_municipalityid = act_line_columns[getInputfile_column_id()];
				} else if( (getInputfile_column_id_first() != INPUTFILE_COLUMN_UNSET) && (getInputfile_column_id_last() != INPUTFILE_COLUMN_UNSET)) {
					for(int actidcol = getInputfile_column_id_first(); actidcol <= getInputfile_column_id_last(); actidcol++) {
						actline_municipalityid += act_line_columns[actidcol];
					}
				}
				if( (! municipality_gemeindeschluessel.equals(actline_municipalityid)) && (! municipality_gemeindeschluessel.equals(""))) {
					System.out.println("Information: changed municipality in line "+file_lineno+"  from gemeindeschluessel ==="+municipality_gemeindeschluessel+"=== to ==="+actline_municipalityid+"===");
						// actual file records doesn't belong to read municipality. Store for later file records reading
					buffer_first_line_next_muni = dateizeile;
					System.out.println("ok, found next muni and stored file record to buffer ==="+buffer_first_line_next_muni+"===");

					if(strassenanzahl > 0) {

							// sort streets according to suburbs
						for(int actouterstrindex = 0; actouterstrindex < strassenanzahl; actouterstrindex++) {
							for(int actstrindex = actouterstrindex+1; actstrindex < strassenanzahl; actstrindex++) {
								String local_prev_street = strassen[actouterstrindex];
								String local_prev_streetref = streetrefs[actouterstrindex];
								String local_prev_suburb = streetsuburbs[actouterstrindex];
		
								String local_act_street = strassen[actstrindex];
								String local_act_streetref = streetrefs[actstrindex];
								String local_act_suburb = streetsuburbs[actstrindex];
								boolean exchange = false;
								if( local_prev_suburb.compareTo(local_act_suburb) > 0) {
									exchange = true;
								} else if(local_prev_suburb.equals(local_act_suburb)) {
									if(local_prev_street.compareTo(local_act_street) > 0)
										exchange = true;
								}
								if(exchange) {
									strassen[actstrindex] = local_prev_street;
									streetrefs[actstrindex] = local_prev_streetref;
									streetsuburbs[actstrindex] = local_prev_suburb;
		
									strassen[actouterstrindex] = local_act_street;
									streetrefs[actouterstrindex] = local_act_streetref;
									streetsuburbs[actouterstrindex] = local_act_suburb;
								}
							}
							//System.out.println(" outer-loop am Ende #"+actouterstrindex+"  wurde ==="+strassen[actouterstrindex]+" ("+streetsuburbs[actouterstrindex]+")===");
						}

						this.setMunicipality_name(municipality_name);
						this.setMunicipality_administrationid(municipality_gemeindeschluessel);
						this.setMunicipality_osmrelationid("");
						this.setStreets(strassen);
						this.setStreetrefs(streetrefs);
						this.setStreetsuburbs(streetsuburbs);
						this.setStreetcount(strassenanzahl);
						this.setNumber_municipality_already_read(this
								.getNumber_municipality_already_read() + 1);

						return true;
				 	} // end of found streets when regionalkey changed: if(strassenanzahl > 0) {
					// no continue here, because in this actual line first new street of next municipality
				}

//ToDo set better, for example with a regular expression to analyse column
					// special filter for Thüringen inputfile, because, in this column, the typ
				if(getInputfile_column_filter() != INPUTFILE_COLUMN_UNSET) {
					System.out.println("searchstring==="+act_line_columns[getInputfile_column_filter()]+"=== pattern ==="+getInputfile_column_filtercontentmustbe()+"===");
					if( ! Pattern.matches(getInputfile_column_filtercontentmustbe(), act_line_columns[getInputfile_column_filter()])) {		// complete check
						System.out.println("Info: actual line doesn't fit filter column in line #"+file_lineno+"  column-value unexpected ==="+act_line_columns[getInputfile_column_filter()]+"===  line content ==="+dateizeile+"===");
						continue;
					}
				}

					// work with current file line
				municipality_gemeindeschluessel = "";
				if(getInputfile_column_id() != INPUTFILE_COLUMN_UNSET) {
					municipality_gemeindeschluessel = act_line_columns[getInputfile_column_id()];
				} else if( (getInputfile_column_id_first() != INPUTFILE_COLUMN_UNSET) && (getInputfile_column_id_last() != INPUTFILE_COLUMN_UNSET)) {
					for(int actidcol = getInputfile_column_id_first(); actidcol <= getInputfile_column_id_last(); actidcol++) {
						municipality_gemeindeschluessel += act_line_columns[actidcol];
					}
				}
				municipality_name = act_line_columns[getInputfile_column_municipalityname()];
				municipality_submunicipalityname = "";
				if(getInputfile_column_suburbname() != INPUTFILE_COLUMN_UNSET) {
					municipality_submunicipalityname = act_line_columns[getInputfile_column_suburbname()];
				}
				if(	( ! municipality_name.equals("")) && 
					(	( municipality_name.indexOf(" GT ") != -1) || 
						(municipality_name.indexOf(" OT ") != -1) || 
						(municipality_name.indexOf(" ST ") != -1))) {
					int pos = -1;
						// if contains ST (Stadtteil), then remove ST Suffix for Municipality Name
					if(municipality_name.indexOf(" ST ") != -1) {
						pos = municipality_name.indexOf(" ST ");
						municipality_submunicipalityname = municipality_name.substring(pos+4);
						municipality_name = municipality_name.substring(0,pos);
						System.out.println(" municipality_name contained OT or ST, now reduced ==="+municipality_name+"=== and submunicipality is now ==="+municipality_submunicipalityname+"===");
					}
						// if contains OT (Ortsteil), then build both municipality name and submunicipality name
					if(municipality_name.indexOf(" OT ") != -1) {
						pos = municipality_name.indexOf(" OT ");
						System.out.println("municipality contains OT, so build both muni and submuni ...");
						municipality_submunicipalityname = municipality_name.substring(pos+4);
						municipality_name = municipality_name.substring(0,pos);
						System.out.println(" builded muni ==="+municipality_name+"=== and submuni ==="+municipality_submunicipalityname+"===");
					}
						// if contains GT (Gemeindeteil), then build both municipality name and submunicipality name
					if(municipality_name.indexOf(" GT ") != -1) {
						pos = municipality_name.indexOf(" GT ");
						System.out.println("municipality contains GT, so build both muni and submuni ...");
						municipality_submunicipalityname = municipality_name.substring(pos+4);
						municipality_name = municipality_name.substring(0,pos);
						System.out.println(" builded muni ==="+municipality_name+"=== and submuni ==="+municipality_submunicipalityname+"===");
					}
				}
				
				
				if(municipality_submunicipalityname.equals(municipality_name))
					municipality_submunicipalityname = "";
				if(act_line_columns.length > getInputfile_column_streetname()) {
					actual_street = act_line_columns[getInputfile_column_streetname()];
				} else {
					continue;
				}

				if(actual_street.indexOf("str.") != -1) {
					actual_street = actual_street.replace("str.","strasse");
					System.out.println("Info: changed Street name because of abbrev str. from ==="+act_line_columns[getInputfile_column_streetname()]+"=== to ==="+actual_street+"===");
				}
				if(actual_street.indexOf("Str.") != -1) {
					actual_street = actual_street.replace("Str.","Strasse");
					System.out.println("Info: changed Street name because of abbrev Str. from ==="+act_line_columns[getInputfile_column_streetname()]+"=== to ==="+actual_street+"===");
				}

				actual_streetref = "";
				if(getInputfile_column_streetref() != INPUTFILE_COLUMN_UNSET) {
					actual_streetref = act_line_columns[getInputfile_column_streetref()];
				}

					// check first, if actual street  is already in array (identical street name)
				boolean actual_street_doubled = false;
				for(int readi=0; readi < strassenanzahl; readi++) {
					if(	strassen[readi].equals(actual_street)
						&&	streetsuburbs[readi].equals(municipality_submunicipalityname)
						&&	streetrefs[readi].equals(municipality_submunicipalityname)) {
						actual_street_doubled = true;
						break;
					}
				}
				if( ! actual_street_doubled ) {
					strassen[strassenanzahl] = actual_street;
					streetrefs[strassenanzahl] = actual_streetref;
					streetsuburbs[strassenanzahl] = municipality_submunicipalityname;
					strassenanzahl++;
				}

				System.out.println("# "+file_lineno+"  mun..name ==="+municipality_name+"=== street ==="+actual_street+"===  gemeindeschluessel ==="+municipality_gemeindeschluessel+"===");
			} while ( (dateizeile = filereader.readLine()) != null );  // end of do


				// After reading file complete: if streets still in buffer, import them now
			if(strassenanzahl > 0) {

					// sort streets according to suburbs
				for(int actouterstrindex = 0; actouterstrindex < strassenanzahl; actouterstrindex++) {
					for(int actstrindex = actouterstrindex+1; actstrindex < strassenanzahl; actstrindex++) {
						String local_prev_street = strassen[actouterstrindex];
						String local_prev_suburb = streetsuburbs[actouterstrindex];

						String local_act_street = strassen[actstrindex];
						String local_act_suburb = streetsuburbs[actstrindex];
						//System.out.println("vergleich  ==="+local_prev_street+" ("+local_prev_suburb+")===   mit ==="+local_act_street+" ("+local_act_suburb+") ===");
						boolean exchange = false;
						if( local_prev_suburb.compareTo(local_act_suburb) > 0) {
							exchange = true;
						} else if(local_prev_suburb.equals(local_act_suburb)) {
							if(local_prev_street.compareTo(local_act_street) > 0)
								exchange = true;
						}
						if(exchange) {
							//System.out.println(" - changen");
							strassen[actstrindex] = local_prev_street;
							streetsuburbs[actstrindex] = local_prev_suburb;

							strassen[actouterstrindex] = local_act_street;
							streetsuburbs[actouterstrindex] = local_act_suburb;
						}
					}
					//System.out.println(" outer-loop am Ende #"+actouterstrindex+"  wurde ==="+strassen[actouterstrindex]+" ("+streetsuburbs[actouterstrindex]+")===");
				}

				this.setMunicipality_name(municipality_name);
				this.setMunicipality_administrationid(municipality_gemeindeschluessel);
				this.setMunicipality_osmrelationid("");
				this.setStreets(strassen);
				this.setStreetsuburbs(streetsuburbs);
				this.setStreetcount(strassenanzahl);
				this.setNumber_municipality_already_read(this
						.getNumber_municipality_already_read() + 1);

				return true;

		
		 	} // end of found streets when regionalkey changed: if(strassenanzahl > 0) {

			return false;

	    } catch (FileNotFoundException e) {
	    	System.out.println("Error occured, file not found at file ==="+streetlist_filename+"===   will be ignored");
	      e.printStackTrace();
	    } catch (IOException e) {
	    	System.out.println("Error occured, i/o fail at file ==="+streetlist_filename+"===   will be ignored");
	      e.printStackTrace();
	    }
		return false;

	}

}

