/*
 * 	V3.0, 2016-02-29, Dietmar Seifert
 * 		actual version from private repositories used for public version.
 * 		All SQL-Statements to prepared Statements transfered 
 * 		and last new country Luxembourg support added
*/

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.*;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.*;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import de.regioosm.listofstreetscore.util.Applicationconfiguration;
import de.regioosm.listofstreetscore.util.LogMessage;


/**
 * Examine OpenStreetMap administrative boundary relation for new imported municipality or for updates.
 * Municipality name, country and optionally a municipality reference id will be used 
 * to search in local osm2pgsql DB.
 * A found OSM relation id and the administrative hierarchy, starting at country level, 
 * down to level above municipality name, will be examined, too.
 * The found data
 *  - OSM polygon as binary postgis geometry
 *  - administrative parents
 *  will be stored, together with the state of polygon
 *  + ok=polygon found and stored
 *  + missing_polygon=no relation found for municipality;
 *  + error-relation-no-boundary-tag=OSM relation found, but not correctly tagged as boundary=administrative;
 *  + invalid-multipolygon=OSM relation is not valid as postgis polygon or multi polygon. Normally invalid in OSM, too.
 * 
 * This class will be used only on command line on evaluation server, up to now (2016/02).
 * Start it as java MuncipalityPolygons -h for a list of all program input parameters.
 * 
 * @author Dietmar Seifert
 * @version 3.0
 * 
 */
public class MunicipalityPolygons {
	static Connection con_listofstreets = null;
	static Connection con_mapnik = null;
	static Applicationconfiguration configuration = new Applicationconfiguration();
	private static final Logger log = Logger.getLogger( MunicipalityPolygons.class.getName() );


	/**
	 * return administrative hierarchy around given OSM relation (@param osm_relation_id) through
	 * 		DB Postgis queries in local osm2pgsql DB
	 *  
	 * @param relation_officialkey_key: Name of OSM key for municipality reference id (country specific), for example de:amtlicher_gemeindeschluessel in Germany
	 * @param osm_relation_id: Id of a administrative relation (must have negative prefix!!!), for which the surrounding admin polygon names should be found 
	 * @return: above administrative polygon names, starting from country name, down to one level up to municipality, separated each level name by colon (,)
	 */
	static String get_administrative_hierarchy_from_polygons(String relation_officialkey_key, String osm_relation_id)
	{
		String polygonHierarchySql = "";
		String officialkeyAgsSql = "";
		String local_hierarchy = "";
		String previous_osmid = "";
		try {
			polygonHierarchySql = "SELECT osm_id, tags->'name' AS name, tags->'name:prefix' AS name_prefix,";
			polygonHierarchySql += " tags->'name:suffix' AS name_suffix,";
			polygonHierarchySql += " tags->'admin_level' AS admin_level,";
			polygonHierarchySql += " tags->? AS gemeindeschluessel FROM";
			polygonHierarchySql += " planet_polygon AS polytable,";
			polygonHierarchySql += " (SELECT ST_Centroid(way) AS WAY FROM planet_polygon WHERE";
			polygonHierarchySql += " osm_id = ?) AS waysuburb";
			polygonHierarchySql += " WHERE ST_Contains(polytable.way,waysuburb.way)";
			polygonHierarchySql += " ORDER BY to_number(tags->'admin_level', '999') DESC;";

			log.log(Level.FINE, "polygonHierarchySql ==="+polygonHierarchySql+"===");
			log.log(Level.FINE, " where input-parameter osm_relation_id ==="+osm_relation_id+"===");


			PreparedStatement stmt_geometrie_bbox = con_mapnik.prepareStatement(polygonHierarchySql);
			stmt_geometrie_bbox.setString(1, relation_officialkey_key);
			stmt_geometrie_bbox.setLong(2, Long.parseLong(osm_relation_id));
			ResultSet rs_geometrie_bbox = stmt_geometrie_bbox.executeQuery();
			
			Integer count_adminlevel = 0;
			while( rs_geometrie_bbox.next() ) {
				log.log(Level.FINE, "id ==="+rs_geometrie_bbox.getString("osm_id")+"===    name ==="+rs_geometrie_bbox.getString("name")+"=== admin_level ==="+rs_geometrie_bbox.getString("admin_level")+"===  gemeindeschluessel ==="+rs_geometrie_bbox.getString("gemeindeschluessel")+"===");
				if(rs_geometrie_bbox.getString("osm_id").equals(previous_osmid))
					continue;
				previous_osmid = rs_geometrie_bbox.getString("osm_id");
				if( rs_geometrie_bbox.getString("admin_level") == null) {
					log.log(Level.FINE, "  object will be ignored, because admin_level not available");
					continue;
				}
				if( rs_geometrie_bbox.getString("admin_level") == "3") {
					//TODO ignore admin_level=3 should be roughly work to ignore, but it would be better to set active/inactive admin_levels for every country in DB table country
					log.log(Level.FINE, "  object will be ignored, because admin_level is 3. In most countries, this is not an official admin level");
					continue;
				}
				count_adminlevel++;
				if(count_adminlevel == 1) {
					log.log(Level.FINE, "  object will be ignored, because first-time admin_level available, should be the search objects self");
					continue;
				}

				if( ! local_hierarchy.equals(""))
					local_hierarchy = "," + local_hierarchy;
				String actname = "";
				if(rs_geometrie_bbox.getString("name_prefix") != null) { 
					if( ! actname.equals(""))
						actname += " ";
					actname += rs_geometrie_bbox.getString("name_prefix");
				}
				if(rs_geometrie_bbox.getString("name") != null) { 
					if( ! actname.equals(""))
						actname += " ";
					actname += rs_geometrie_bbox.getString("name");
				}
				if(rs_geometrie_bbox.getString("name_suffix") != null) { 
					if( ! actname.equals(""))
						actname += " ";
					actname += rs_geometrie_bbox.getString("name_suffix");
				}
				local_hierarchy = actname + local_hierarchy;
				log.log(Level.FINE, "build local_hierarchy up to now ==="+local_hierarchy+"===");

				if( (relation_officialkey_key != null) 
					&& ! relation_officialkey_key.equals("")
					&&	relation_officialkey_key.equals("de:amtlicher_gemeindeschluessel") 
					&& (rs_geometrie_bbox.getString("gemeindeschluessel") != null)) {

					officialkeyAgsSql = "SELECT hierarchy from officialkeys WHERE ags = ?;";
					PreparedStatement officialkeyAgsStmt = con_listofstreets.prepareStatement(officialkeyAgsSql);
					log.log(Level.FINE, "SQL-Statement officialkeys ==="+officialkeyAgsSql+"===");
					officialkeyAgsStmt.setString(1, rs_geometrie_bbox.getString("gemeindeschluessel"));
					ResultSet rs_officialkeys =  officialkeyAgsStmt.executeQuery();
					Integer count_officialkeys_records = 0;
					while(rs_officialkeys.next()) {
						if((rs_officialkeys.getString("hierarchy") != null) &&
							( ! rs_officialkeys.getString("hierarchy").equals(""))) {
							count_officialkeys_records++;
							if(count_officialkeys_records != 1) {
								log.log(Level.FINE, " cont. actual hierarchy is ==="+rs_officialkeys.getString("hierarchy")+"===");
								log.log(Level.FINE, " cont. sql-string was ==="+officialkeyAgsSql+"===");
							} else {
								log.log(Level.FINE, "found hierarchy from table officialkeys ==="+rs_officialkeys.getString("hierarchy")+"===");

								if( ! local_hierarchy.equals(""))
									local_hierarchy = "," + local_hierarchy;
								local_hierarchy = rs_officialkeys.getString("hierarchy") + local_hierarchy;
								log.log(Level.FINE, "build local_hierarchy final, when found municipality in table officialkeys ==="+local_hierarchy+"===");
								return local_hierarchy;
							}
						}
					}
					officialkeyAgsStmt.close();
				}
			}
			stmt_geometrie_bbox.close();
		}	// end of try DB-connect
		catch( SQLException e) {
			e.printStackTrace();
		}

		return local_hierarchy;

	}

	/**
	 * return administrative hierarchy around given OSM relation (@param osm_relation_id) through
	 * 		Nominatim https-Requests
	 *  
	 * @param objectname:		municipality name
	 * @param geometry_binary	OSM relation as binary postgis geometry
	 * @return: above administrative polygon names, starting from country name, down to one level up to municipality, separated each level name by colon (,)
	 */
	static String get_administrative_hierarchy_from_nominatim(String objectname, String geometry_binary)
	{
		URL                url; 
		URLConnection      urlConn; 
		BufferedReader     dis;


		String selectbefehl_geometrie_bbox = "SELECT ST_XMin(ST_GeomFromText(ST_AsText(?))) as xmin,";
		selectbefehl_geometrie_bbox += " ST_YMin(ST_GeomFromText(ST_AsTExt(?))) as ymin,";
		selectbefehl_geometrie_bbox += " ST_XMax(ST_GeomFromText(ST_AsTExt(?))) as xmax,";
		selectbefehl_geometrie_bbox += " ST_YMax(ST_GeomFromText(ST_AsTExt(?))) as ymax;";
		log.log(Level.FINE, "selectbefehl_geometrie_bbox==="+selectbefehl_geometrie_bbox+"===");
	

		try {
			PreparedStatement stmt_geometrie_bbox = con_listofstreets.prepareStatement(selectbefehl_geometrie_bbox);
			stmt_geometrie_bbox.setString(1, geometry_binary);
			stmt_geometrie_bbox.setString(2, geometry_binary);
			stmt_geometrie_bbox.setString(3, geometry_binary);
			stmt_geometrie_bbox.setString(4, geometry_binary);
			ResultSet rs_geometrie_bbox = stmt_geometrie_bbox.executeQuery();
			if( rs_geometrie_bbox.next() ) {
				log.log(Level.FINE, "ok, Polygon BBOX als xmin ==="+rs_geometrie_bbox.getString(1)+"===    ymin ==="+rs_geometrie_bbox.getString(2)+"===");
				log.log(Level.FINE, "                     xmax ==="+rs_geometrie_bbox.getString(3)+"===    ymax ==="+rs_geometrie_bbox.getString(4)+"===");
				String nominatim_search_url = "http://open.mapquestapi.com/nominatim/v1/search/";

				//nominatim_search_url += objectname;
				try {
					nominatim_search_url += URLEncoder.encode(objectname,"UTF-8");
				}
				catch (UnsupportedEncodingException e) {
					log.log(Level.SEVERE, "ERROR: failure to url-encode(utf8) this text ==="+objectname+"===");
					log.log(Level.SEVERE, e.toString());
					System.out.println("ERROR: failure to url-encode(utf8) this text ==="+objectname+"===");
					System.out.println(e.toString());
					String local_messagetext = "Nominatim Call-Error: utf-8 Codes couldn't be url-encoded for Nominatim-Request. utf-8 sequence was ===" + objectname + "===";
					new LogMessage(LogMessage.CLASS_ERROR, "create_municipality_polygons", "", -1L, local_messagetext);
					return "";
				}
				nominatim_search_url += "?format=xml";
				//nominatim_search_url += "&q="+objectname+"+";
				nominatim_search_url += "&addressdetails=1&limit=3";
				nominatim_search_url += "&viewbox=";
				nominatim_search_url += rs_geometrie_bbox.getString(1)+"%2C";
				nominatim_search_url += rs_geometrie_bbox.getString(2)+"%2C";
				nominatim_search_url += rs_geometrie_bbox.getString(3)+"%2C";
				nominatim_search_url += rs_geometrie_bbox.getString(4);
				log.log(Level.FINE, "Nominatim complete api url ==="+nominatim_search_url+"===");

				StringBuffer filecontent = new StringBuffer();
				try {
					url = new URL(nominatim_search_url);
	
					urlConn = url.openConnection(); 
					urlConn.setDoInput(true); 
					urlConn.setUseCaches(false);
	
					String inputline = "";
// am 3.10.2011 Absturz bei Röbel/Müritz, Stadt
				    dis = new BufferedReader(new InputStreamReader(urlConn.getInputStream(),"UTF-8"));
				    while ((inputline = dis.readLine()) != null)
				    { 
				    	log.log(Level.FINER, "zeile==="+inputline+"===");
				    	filecontent.append(inputline + "\n");
						String suchstring = "[title] => ";
						if(inputline.indexOf(suchstring) != -1) {
							Integer abpos = inputline.indexOf(suchstring);
							log.log(Level.FINER, "abpos beginn title: "+abpos);
							abpos += suchstring.length();
							log.log(Level.FINER, "abpos beginn echt-titel"+abpos);
							inputline = inputline.substring(abpos);
							log.log(Level.FINER, "gefundener Titel netto ==="+inputline);
							String inputline_vorher = inputline;
							inputline = inputline.replace(" ","_");
							if( ! inputline.equals(inputline_vorher)) {
								log.log(Level.FINER, "bei replace Änderung vorgekommen vorher ==="+inputline_vorher+"=== nachher ==="+inputline+"===");
							} else {
								log.log(Level.FINER, "bei replace identisch geblieben ==="+inputline+"===");
							}
						}
					} 
				    dis.close(); 
				    log.log(Level.FINE, "Filelength: "+filecontent.length());
				    log.log(Level.FINE, "File-Content ==="+filecontent.toString()+"===");
	
				} catch (MalformedURLException mue) {
					log.log(Level.SEVERE, "Error due to getting mapquest nominatim api-request (malformedurlexception). url was ==="+nominatim_search_url+"===");
					log.log(Level.SEVERE, "  cont. Input param municipality name ==="+objectname+"===");
					log.log(Level.SEVERE, mue.toString());
					System.out.println("Error due to getting mapquest nominatim api-request (malformedurlexception). url was ==="+nominatim_search_url+"===");					
					System.out.println("  cont. Input param municipality name ==="+objectname+"===");
					System.out.println(mue.toString());
					String local_messagetext = "Nominatim MalformedURLException: Nominatim URL-Request was ==="+nominatim_search_url+"=== and got Trace ==="+mue.toString()+"===";
					new LogMessage(LogMessage.CLASS_ERROR, "create_municipality_polygons", "", -1L, local_messagetext);
					return "";
				} catch (IOException ioe) {
					log.log(Level.SEVERE, "Error due to getting mapquest nominatim api-request (ioexception). url was ==="+nominatim_search_url+"===");
					log.log(Level.SEVERE, "  cont. Input param municipality name ==="+objectname+"===");
					log.log(Level.SEVERE, ioe.toString());
					System.out.println("Error due to getting mapquest nominatim api-request (ioexception). url was ==="+nominatim_search_url+"===");					
					System.out.println("  cont. Input param municipality name ==="+objectname+"===");
					System.out.println(ioe.toString());
					String local_messagetext = "Nominatim IOException: Nominatim URL-Request was ==="+nominatim_search_url+"=== and got Trace ==="+ioe.toString()+"===";
					new LogMessage(LogMessage.CLASS_ERROR, "create_municipality_polygons", "", -1L, local_messagetext);
					return "";
				}

				DocumentBuilderFactory factory = null;
				DocumentBuilder builder = null;
				Document xml_document = null;

				log.log(Level.FINE, "Info: got this xml-content from mapquest api-call ==="+filecontent.toString()+"===");

				try {
					factory = DocumentBuilderFactory.newInstance();
					try {
						builder = factory.newDocumentBuilder();
							// parse xml-document. got a few lines above as result from mapquest nominatim api-call
						xml_document = builder.parse(new InputSource(new StringReader(filecontent.toString())));
					} 
					catch (org.xml.sax.SAXException saxerror) {
						log.log(Level.SEVERE, "ERROR: SAX-Exception during parsing of xml-content ==="+filecontent.toString()+"===");
						log.log(Level.SEVERE, saxerror.toString());
						System.out.println("ERROR: SAX-Exception during parsing of xml-content ==="+filecontent.toString()+"===");
						System.out.println(saxerror.toString());
						String local_messagetext = "Nominatim result caused org.xml.sax.SAXException ==="+filecontent.toString()+"=== and got Trace ==="+saxerror.toString()+"===";
						new LogMessage(LogMessage.CLASS_ERROR, "create_municipality_polygons", "", -1L, local_messagetext);
						return "";
					} 
					catch (IOException ioerror) {
						log.log(Level.SEVERE, "ERROR: IO-Exception during parsing of xml-content ==="+filecontent.toString()+"===");
						log.log(Level.SEVERE, ioerror.toString());
						System.out.println("ERROR: IO-Exception during parsing of xml-content ==="+filecontent.toString()+"===");
						System.out.println(ioerror.toString());
						String local_messagetext = "Nominatim result caused IOException ==="+filecontent.toString()+"=== and got Trace ==="+ioerror.toString()+"===";
						new LogMessage(LogMessage.CLASS_ERROR, "create_municipality_polygons", "", -1L, local_messagetext);
						return "";
					}
				} 
				catch (ParserConfigurationException parseerror) {
					log.log(Level.SEVERE, "ERROR: fail to get new Instance from DocumentBuilderFactory");
					log.log(Level.SEVERE, parseerror.toString());
					System.out.println("ERROR: fail to get new Instance from DocumentBuilderFactory");
					System.out.println(parseerror.toString());
					String local_messagetext = "fail to get new Instance from DocumentBuilderFactory and got Trace ==="+parseerror.toString()+"===";
					new LogMessage(LogMessage.CLASS_ERROR, "create_municipality_polygons", "", -1L, local_messagetext);
				}
	
				//System.out.println("xml-document: first child xml-tag content==="+xml_document.getFirstChild().getTextContent()+"===");

					// get result record only, if boundary=administrative. In this case, get the content of attribute display_name
				NodeList sections = xml_document.getElementsByTagName("place");
			    int numSections = sections.getLength();
			    for (int i = 0; i < numSections; i++) {
		      //Element section = (Element) sections.item(i); // A <sect1>
				Node place_node = (Node) sections.item(i);

				log.log(Level.FINE, "numSections["+i+"] section   .Name ==="+place_node.getNodeName()+"     .getTextContent() ==="+place_node.getTextContent()+"====    .getNodeType() ==="+place_node.getNodeType()+"=== ...");

				String hierarchy_value = "";
				if(place_node.hasAttributes()) {
					NamedNodeMap attrs = place_node.getAttributes();  
					log.log(Level.FINER, " getfirstchild Element Attributes (Number: "+attrs.getLength()+") are:");
					String local_hierarchy_value = "";
					Boolean class_correct = false;
					Boolean type_correct = false;
					for(int attri = 0 ; attri<attrs.getLength() ; attri++) {
						Attr attribute = (Attr)attrs.item(attri);
						log.log(Level.FINER, " " + attribute.getName()+" = "+attribute.getValue());
						if(attribute.getName().equals("class")) {
							if(attribute.getValue().equals("boundary"))
								class_correct = true;
							else
								log.log(Level.WARNING, "WARNING: wrong attribute-value in class ==="+attribute.getValue()+"===");
						}
						if(attribute.getName().equals("type")) {
							if(attribute.getValue().equals("administrative"))
								type_correct = true;
							else
								log.log(Level.WARNING, "WARNING: wrong attribute-value in type ==="+attribute.getValue()+"===");
						}
						if(attribute.getName().equals("display_name")) {
							local_hierarchy_value = attribute.getValue();
							log.log(Level.FINER, "INFO: got attribute-value in display_name ==="+attribute.getValue()+"===");
						}
					}

						// check actual found point, if in requested area (because nominatim gives back also points outside of requested area
					Boolean point_is_in_area = false;


					String selectbefehl_point_is_in_geometrie = "SELECT ST_Contains(?,"
						+ " ST_Transform(ST_PointFromText('POINT(?, ?)',4326),900913)"+") AS point_contains";
					PreparedStatement stmt_point_is_in_geometry = con_listofstreets.prepareStatement(selectbefehl_point_is_in_geometrie);
					stmt_point_is_in_geometry.setString(1, geometry_binary);
					stmt_point_is_in_geometry.setString(2, attrs.getNamedItem("lon").getNodeValue());
					stmt_point_is_in_geometry.setString(3, attrs.getNamedItem("lat").getNodeValue());
					ResultSet rs_point_is_in_geometry = stmt_point_is_in_geometry.executeQuery();
					if( rs_point_is_in_geometry.next() ) {
						log.log(Level.FINE, " resultset ergebnis ==="+rs_point_is_in_geometry.getString("point_contains")+"===");
						if(rs_point_is_in_geometry.getString("point_contains").equals("t"))
							point_is_in_area = true;
						else
							point_is_in_area = false;
					}

					if(class_correct && type_correct && point_is_in_area) {
						log.log(Level.FINE, "yipieh: got hierarchy_value original ==="+local_hierarchy_value+"===");
						local_hierarchy_value = local_hierarchy_value.replace(", ",",");
						String[] local_hierarchy_value_array = local_hierarchy_value.split(",");
						for(Integer hierarchyindex=0; hierarchyindex<local_hierarchy_value_array.length;hierarchyindex++) {
							if(local_hierarchy_value_array[hierarchyindex].indexOf("Verwaltungsgemeinschaft ") != -1) {
															// do not add this hierarchy level
							} else if(	(local_hierarchy_value_array[hierarchyindex].indexOf("Europe,") == 0) ||
									(local_hierarchy_value_array[hierarchyindex].indexOf("Europa,") == 0)) {
															// do not add this hierarchy level
							} else if(local_hierarchy_value_array[hierarchyindex].equals("Deutschland")) {
								if( ! hierarchy_value.equals(""))
									hierarchy_value = "," + hierarchy_value;
								hierarchy_value = "Bundesrepublik Deutschland" + hierarchy_value;
							} else {
								if( ! hierarchy_value.equals(""))
									hierarchy_value = "," + hierarchy_value;
								hierarchy_value = local_hierarchy_value_array[hierarchyindex] + hierarchy_value;
							}
						}
						log.log(Level.FINE, "yipieh: got hierarchy_value swithed and normalized ==="+hierarchy_value+"===");
						return hierarchy_value;
					}
				}
		    }
			}
		}	// ende try DB-connect
		catch( SQLException e) {
			e.printStackTrace();
		}

		return "";

	}




	/**
	 * @param args: for list of program arguments, please use java MunicipalityPolygons -h
	 */
	public static void main(String[] args) {

		try {
			Handler handler = new ConsoleHandler();
			handler.setLevel(configuration.logging_console_level);
			log.addHandler( handler );
			FileHandler fhandler = new FileHandler(configuration.logging_filename);
			fhandler.setLevel(configuration.logging_file_level);
			log.addHandler( fhandler );
			log.setLevel(configuration.logging_console_level);
		} 
		catch (IOException e) {
			System.out.println("Fehler beim Logging-Handler erstellen, ...");
			System.out.println(e.toString());
		}

		String setpolygonstate = "ok";
		
		for(int lfdnr=0;lfdnr<args.length;lfdnr++) {
			log.log(Level.INFO, "args["+lfdnr+"] ==="+args[lfdnr]+"===");
		}
		if((args.length >= 1) && (args[0].equals("-h"))) {
			System.out.println("-polygonstate missing|ok|old|blocked");
			System.out.println("-country country");
			System.out.println("-name xystadt");
			System.out.println("-gemeindeschluessel 4711234");
			System.out.println("-osmrelationid 4711");
			System.out.println("-municipalityhierarchy somestring");
			System.out.println("-setpolygonstate somestring");
			return;
		}


		String parameterOsmRelationId = "";
		String parameterOfficialkeysId = "";
		String parameterPolygonState = "";
		String parameterCountry = "";
		String parameterMunicipality = "";
		String parameterOsmHierarchy = "";
		
		if(args.length >= 1) {
			int args_ok_count = 0;
			for(int argsi=0;argsi<args.length;argsi+=2) {
				log.log(Level.FINE, " args pair analysing #: "+argsi+"  ==="+args[argsi]+"===   #+1: "+(argsi+1)+"   ==="+args[argsi+1]+"===");
				if(args[argsi].equals("-osmrelationid")) {
					parameterOsmRelationId = args[argsi+1];
					args_ok_count += 2;
				} else if(args[argsi].equals("-gemeindeschluessel")) {
					parameterOfficialkeysId = args[argsi+1];
					args_ok_count += 2;
				} else if(args[argsi].equals("-polygonstate")) {
					parameterPolygonState = args[argsi+1];
					args_ok_count += 2;
				} else if(args[argsi].equals("-country")) {
					try {
						parameterCountry = URLDecoder.decode(args[argsi+1], "UTF-8");
					} catch (UnsupportedEncodingException e) {
						System.out.println("Program input parameter -country could not be decoded, was ===" + args[argsi] + "===, will be ignored");
						System.out.println(e.toString());
					}
					args_ok_count += 2;
				} else if(args[argsi].equals("-name")) {
					try {
						parameterMunicipality = URLDecoder.decode(args[argsi+1], "UTF-8");
					} catch (UnsupportedEncodingException e) {
						System.out.println("Program input parameter -name could not be decoded, was ===" + args[argsi] + "===, will be ignored");
						System.out.println(e.toString());
					}
					args_ok_count += 2;
				} else if(args[argsi].equals("-municipalityhierarchy")) {
					parameterOsmHierarchy = "%" + args[argsi+1] + "%";						// wildcards as prefix and as suffix
					args_ok_count += 2;
				} else if(args[argsi].equals("-setpolygonstate")) {
					setpolygonstate = args[argsi+1];
					args_ok_count += 2;
				} else {
					System.out.println("Error: unknown program parameter ===" + args[argsi] + "===");
				}
			}
			if(args_ok_count != args.length) {
				System.out.println("ERROR: not all programm parameters were valid, STOP");
				return;
			}
		}

		DateFormat time_formatter = new SimpleDateFormat();

		Date time_program_startedtime = new Date();
		log.log(Level.INFO, "Program: Started Time: "+time_formatter.format(time_program_startedtime));

		try {
			log.log(Level.FINEST, "ok, jetzt Class.forName Aufruf ...");
			Class.forName("org.postgresql.Driver");
			log.log(Level.FINEST, "ok, nach Class.forName Aufruf!");
		}
		catch(ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		try {

				//Connection of own project-specific DB
			String url_listofstreets = configuration.db_application_url;
			con_listofstreets = DriverManager.getConnection(url_listofstreets, configuration.db_application_username, configuration.db_application_password);

			String url_mapnik = configuration.db_osm2pgsql_url;
			con_mapnik = DriverManager.getConnection(url_mapnik, configuration.db_osm2pgsql_username, configuration.db_osm2pgsql_password);


					// Main Query
					//FIX IMMER AND polygon_state <> 'blocked' ergaenzen im Standardfall ohne args-Parameter

							// complete new creation of all municipalites - seldom neccessary
			String selectbefehl_municipalities = "SELECT m.id AS id, m.name AS name, osm_relation_id, officialkeys_id,"
				+ " osmrelation_administration_key AS relation_officialkey_key,"
				+ " osmobjects_administration_key AS point_officialkey_key,"
				+ " c.country AS country"
				+ " FROM municipality as m, country as c";
			String selectbefehl_municipalities_whereclause = "";

			if(!parameterOsmRelationId.equals("")) {
				if( ! selectbefehl_municipalities_whereclause.equals(""))
					selectbefehl_municipalities_whereclause += " AND";
				selectbefehl_municipalities_whereclause += " osm_relation_id = ?";
			}
			if(!parameterOfficialkeysId.equals("")) {
				if( ! selectbefehl_municipalities_whereclause.equals(""))
					selectbefehl_municipalities_whereclause += " AND";
				selectbefehl_municipalities_whereclause += " officialkeys_id = ?";
			}
			if(!parameterPolygonState.equals("")) {
				if( ! selectbefehl_municipalities_whereclause.equals(""))
					selectbefehl_municipalities_whereclause += " AND";
				selectbefehl_municipalities_whereclause += " polygon_state = ?";
			}
			if(!parameterCountry.equals("")) {
				if( ! selectbefehl_municipalities_whereclause.equals(""))
					selectbefehl_municipalities_whereclause += " AND";
				selectbefehl_municipalities_whereclause += " country = ?";
			}
			if(!parameterMunicipality.equals("")) {
				if( ! selectbefehl_municipalities_whereclause.equals(""))
					selectbefehl_municipalities_whereclause += " AND";
				selectbefehl_municipalities_whereclause += " name like ?";
			}
			if(!parameterOsmHierarchy.equals("")) {
				if( ! selectbefehl_municipalities_whereclause.equals(""))
					selectbefehl_municipalities_whereclause += " AND";
				selectbefehl_municipalities_whereclause += " osm_hierarchy like ?";
			}
				// always filter municipalities, which are blocked by polygon_state
			if(!selectbefehl_municipalities_whereclause.equals(""))
				selectbefehl_municipalities_whereclause += " AND";
			selectbefehl_municipalities_whereclause += " polygon_state <> 'blocked'";

			if( ! selectbefehl_municipalities_whereclause.equals("")) {
				selectbefehl_municipalities += " WHERE " + selectbefehl_municipalities_whereclause;
				selectbefehl_municipalities += " AND m.country_id = c.id";
			} else {
				selectbefehl_municipalities += " WHERE m.country_id = c.id";
			}

//TODO BEGIN   temporary code
selectbefehl_municipalities += " AND country NOT like 'Neu-Meck-Vorp%'";
//TODO END     temporary code
			
			selectbefehl_municipalities += " ORDER BY osm_hierarchy, name;";

			log.log(Level.FINE, "\nMAIN Query for all municipalities is ==="+selectbefehl_municipalities+"===");

			PreparedStatement stmt_municipalities = con_listofstreets.prepareStatement(selectbefehl_municipalities);

			Integer statementIndex = 1;
			if(!parameterOsmRelationId.equals("")) {
				stmt_municipalities.setString(statementIndex++, parameterOsmRelationId);
			}
			if(!parameterOfficialkeysId.equals("")) {
				stmt_municipalities.setString(statementIndex++, parameterOfficialkeysId);
			}
			if(!parameterPolygonState.equals("")) {
				stmt_municipalities.setString(statementIndex++, parameterPolygonState);
			}
			if(!parameterCountry.equals("")) {
				stmt_municipalities.setString(statementIndex++, parameterCountry);
			}
			if(!parameterMunicipality.equals("")) {
				stmt_municipalities.setString(statementIndex++, parameterMunicipality);
			}
			if(!parameterOsmHierarchy.equals("")) {
				stmt_municipalities.setString(statementIndex++, parameterOsmHierarchy);
			}

			ResultSet rs_municipalities = stmt_municipalities.executeQuery();
			Integer count_municipalities = 0;
				// Loop over all municipaliy records
			while( rs_municipalities.next() ) {
				count_municipalities++;

				log.log(Level.FINE, "========================================================================================");
				log.log(Level.FINE, "\nBEGINN Verarbeitung lfd.-Nr. "+count_municipalities+", municapility id: "+rs_municipalities.getLong("id")+",  Gemeindeschlüssel ==="+rs_municipalities.getString("officialkeys_id")+"=== mit Relations-ID # "+rs_municipalities.getString("osm_relation_id")+"   Name ==="+rs_municipalities.getString("name")+"===    gemeindeschlÃ¼ssel ==="+rs_municipalities.getString("officialkeys_id")+"===");
				log.log(Level.FINE, "----------------------------------------------------------------------------------------");

				if(	(rs_municipalities.getString("osm_relation_id") != null) &&
					((rs_municipalities.getString("osm_relation_id").indexOf("existiert noch nicht") == 0) ||
					 (rs_municipalities.getString("osm_relation_id").indexOf("xxxxxx") == 0) ||
					 (rs_municipalities.getString("osm_relation_id").indexOf("nach nicht") == 0))) {
					log.log(Level.FINE, "Municipality will be ignored, because osm relation id contains invalid content ==="+rs_municipalities.getString("osm_relation_id")+"===");
					continue;
				}

				Date time_municipality_startedtime = new Date();



				String actual_polygon_part = "";
				String complete_polygon = "";
				String complete_polygon_idlist = "";

				String selectbefehl_relation = "SELECT osm_id AS id, name, tags -> ? AS gemeindeschluessel,"
					+ " boundary, tags->'is_in' AS is_in, tags->'is_in:state' AS is_in_state, tags->'is_in:country' AS is_in_country,"
					+ " tags->'is_in:region' AS is_in_region, tags->'is_in:county' AS is_in_county,"
					+ " way AS polygon_geometry"
					+ " FROM planet_polygon";

				String local_adminid1 = "";
				String local_adminid2 = "";
				if(		((rs_municipalities.getString("relation_officialkey_key") != null) && 
							( ! rs_municipalities.getString("relation_officialkey_key").equals("")))
					&&	((rs_municipalities.getString("officialkeys_id") != null) && 
							( ! rs_municipalities.getString("officialkeys_id").equals("")))
					) {
					selectbefehl_relation += " WHERE (tags @> '? => ?'";
						// only in Germany: some variants of AGS without suffix 0
					if(rs_municipalities.getString("relation_officialkey_key").equals("de:amtlicher_gemeindeschluessel")) {
						local_adminid1 = rs_municipalities.getString("officialkeys_id");
						while(( local_adminid1.length() >= 1) && (local_adminid1.substring(local_adminid1.length()-1,local_adminid1.length()).equals("0")))
							local_adminid1 = local_adminid1.substring(0,local_adminid1.length()-1);
						if( ! rs_municipalities.getString("officialkeys_id").equals(local_adminid1))
							selectbefehl_relation += "OR (tags @> 'de:amtlicher_gemeindeschluessel => ?') ";
							// Variant with some spacens "03 1 52 012" (for example Göttingen)
						local_adminid2 = rs_municipalities.getString("officialkeys_id");
						local_adminid2 = local_adminid2.substring(0,2) + " " + local_adminid2.substring(2,3) + " " + local_adminid2.substring(3,5) + " " + local_adminid2.substring(5);
						selectbefehl_relation += "OR (tags @> 'de:amtlicher_gemeindeschluessel => ?') ";
					}
				} else if(		(rs_municipalities.getString("osm_relation_id") != null)
							&&	(!rs_municipalities.getString("osm_relation_id").equals(""))) {
						// relation-ID on server has NEGATIVE VALUE
					selectbefehl_relation += " WHERE osm_id = -?";
				} else {
					log.log(Level.FINE, "\nWARNING: Municipality will be ignored (neither administrationid nor osm_relation_id are present) Data were Nr. "+count_municipalities+" with Relations-ID # "+rs_municipalities.getString("osm_relation_id")+"   Name ==="+rs_municipalities.getString("name")+"===    gemeindeschlÃ¼ssel ==="+rs_municipalities.getString("officialkeys_id")+"===");
					continue;
				}
				selectbefehl_relation += " ORDER BY osm_id;";


				PreparedStatement stmt_relation = con_mapnik.prepareStatement(selectbefehl_relation);
				statementIndex = 1;
				stmt_relation.setString(statementIndex++, rs_municipalities.getString("relation_officialkey_key"));
				if(		((rs_municipalities.getString("relation_officialkey_key") != null)
					&&	( ! rs_municipalities.getString("relation_officialkey_key").equals("")))
					&&	((rs_municipalities.getString("officialkeys_id") != null)
					&&	( ! rs_municipalities.getString("officialkeys_id").equals("")))) {
					stmt_relation.setString(statementIndex++, rs_municipalities.getString("relation_officialkey_key"));
					if(		rs_municipalities.getString("relation_officialkey_key").equals("de:amtlicher_gemeindeschluessel")) {
						if(!rs_municipalities.getString("officialkeys_id").equals(local_adminid1)) {
							stmt_relation.setString(statementIndex++, local_adminid1);
						}
						stmt_relation.setString(statementIndex++, local_adminid2);
					}
				} else if(		(rs_municipalities.getString("osm_relation_id") != null)
						&&	(!rs_municipalities.getString("osm_relation_id").equals(""))) {
					stmt_relation.setLong(statementIndex++, rs_municipalities.getLong("osm_relation_id"));
				}
				

				log.log(Level.FINE, "Ausgabe selectbefehl_relation ===" + selectbefehl_relation.toString() + "===");
				ResultSet rs_relation = stmt_relation.executeQuery();
				Integer count_relations = 0;
				Integer count_correct_relations = 0;
				String relation_wrong = "";
				String municipality_officialkeysid = "";
				int municipality_officialkeysid_origin_length = 0;		// origin found length (without spaces) for check longest original key as best
				String hierarchy_topdown = "";
				Integer hierarchy_topdown_level = 0;
				String found_osm_relation_id = "";
					// loop over all parts of polygon (in osm2pgsql scheme, every single closed polygon has its own row, but all share same osm relation id)
				while( rs_relation.next() ) {
					count_relations++;
					log.log(Level.FINE, "Relation #"+count_relations+":  id==="+rs_relation.getString("id")+"===  name ==="+rs_relation.getString("name")+"===   gemeindeschluessel ==="+rs_relation.getString("gemeindeschluessel")+"===  boundary ==="+rs_relation.getString("boundary")+"===");
					if((rs_relation.getString("boundary") == null) || ( ! rs_relation.getString("boundary").equals("administrative"))) {
						System.out.print("Warning: Relation-ID is invalid. It's not boundary=administrative, but ");
						if(rs_relation.getString("boundary") == null) {
							relation_wrong = "error-relation-no-boundary-tag";
							log.log(Level.FINE, "is missing");
							String local_messagetext = "Relation with osm-id "+rs_relation.getString("id")+" has no Tag 'boundary' and will be ignored";
							new LogMessage(LogMessage.CLASS_WARNING, "create_municipality_polygons", rs_municipalities.getString("name"), rs_municipalities.getLong("id"), local_messagetext);
						} else {
							relation_wrong = "error-relation-wrong-boundary-value";
							log.log(Level.WARNING, "has wrong value ==="+rs_relation.getString("boundary")+"===");
							String local_messagetext = "Relation with osm-id "+rs_relation.getString("id")+" has Tag 'boundary', but invalid value '"+rs_relation.getString("boundary")+"'";
							new LogMessage(LogMessage.CLASS_WARNING, "create_municipality_polygons", rs_municipalities.getString("name"), rs_municipalities.getLong("id"), local_messagetext);
						}
						continue;
					}


						// check actual administrationid and normalize it to length=8
					String local_actual_municipality_officialkeysid = "";
					int local_actual_municipality_officialkeysid_origin_length = 0;
					if(rs_relation.getString("gemeindeschluessel") != null) {
						local_actual_municipality_officialkeysid = rs_relation.getString("gemeindeschluessel");
						if(( (! local_actual_municipality_officialkeysid.equals("")) && local_actual_municipality_officialkeysid.indexOf(" ") != -1))
							local_actual_municipality_officialkeysid = local_actual_municipality_officialkeysid.replace(" ","");
						local_actual_municipality_officialkeysid_origin_length = local_actual_municipality_officialkeysid.length();
						if(rs_municipalities.getString("relation_officialkey_key").equals("de:amtlicher_gemeindeschluessel")) {
							if(( (! local_actual_municipality_officialkeysid.equals("")) && local_actual_municipality_officialkeysid.length() < 8)) {
								String local_string0 = "00000000";
								log.log(Level.FINE, "Warning: german gemeindeschluessel too short, will be appended by 0s  original ==="+local_actual_municipality_officialkeysid+"=== for municipality ==="+rs_municipalities.getString("name")+"=== in relation with osm_id ==="+rs_municipalities.getString("osm_relation_id")+"===");
								local_actual_municipality_officialkeysid += local_string0.substring(0,(8 - local_actual_municipality_officialkeysid.length()));
								log.log(Level.FINE, "  (cont.) german gemeindeschluessel after appended ==="+local_actual_municipality_officialkeysid+"=== for municipality ==="+rs_municipalities.getString("name")+"=== in relation with osm_id ==="+rs_municipalities.getString("osm_relation_id")+"===");
							}
							if(( (! local_actual_municipality_officialkeysid.equals("")) && (local_actual_municipality_officialkeysid.length() > 8))) {
								log.log(Level.WARNING, "ERROR german gemeindeschluessel too long ==="+local_actual_municipality_officialkeysid+"=== for municipality ==="+rs_municipalities.getString("name")+"=== in relation with osm_id ==="+rs_municipalities.getString("osm_relation_id")+"===");
								local_actual_municipality_officialkeysid = "";
								local_actual_municipality_officialkeysid_origin_length = 0;
								String local_messagetext = "Relation with osm-id "+rs_relation.getString("id")+" has too-long Gemeindeschluessel '"+local_actual_municipality_officialkeysid+"'";
								new LogMessage(LogMessage.CLASS_ERROR, "create_municipality_polygons", rs_municipalities.getString("name"), rs_municipalities.getLong("id"), local_messagetext);
							}
						}
					}




					if(found_osm_relation_id.equals("")) {
							// first usable relation-id
						found_osm_relation_id = rs_relation.getString("id");
						municipality_officialkeysid = local_actual_municipality_officialkeysid;
						municipality_officialkeysid_origin_length = local_actual_municipality_officialkeysid_origin_length;
					} else {
							// if not first found polygon-part and new relation (so really different boundary-polygon): check, if better than previous found one
						if(	( ! found_osm_relation_id.equals("")) && ( ! found_osm_relation_id.equals(rs_relation.getString("id")))) {
								// if actual administation-id is longer then before, use the actual, its more precise, hopefully
							if(local_actual_municipality_officialkeysid_origin_length > municipality_officialkeysid_origin_length) {
								log.log(Level.FINE, "ok, actual relation is origin-length longer ("+local_actual_municipality_officialkeysid_origin_length+") ");
								log.log(Level.FINE, "  (cont) than previous found ("+municipality_officialkeysid_origin_length+")");
								log.log(Level.FINE, "  (cont) previous found municipality was ==="+municipality_officialkeysid+"===");
								log.log(Level.FINE, "  (cont) actual municipality is ==="+local_actual_municipality_officialkeysid+"===");
								log.log(Level.FINE, "  (cont) previous found osm relation id was ==="+found_osm_relation_id+"===");
								log.log(Level.FINE, "  (cont) actual osm relation id is ==="+rs_relation.getString("id")+"===");
								log.log(Level.FINE, "  (cont) so get now the actual better ones!");
								municipality_officialkeysid = local_actual_municipality_officialkeysid;
								municipality_officialkeysid_origin_length = local_actual_municipality_officialkeysid_origin_length;
								found_osm_relation_id = rs_relation.getString("id");
							} else if(	(local_actual_municipality_officialkeysid_origin_length == municipality_officialkeysid_origin_length) &&
												 	(rs_municipalities.getString("name").equals(rs_relation.getString("name")))
											) {
								municipality_officialkeysid = local_actual_municipality_officialkeysid;
								found_osm_relation_id = rs_relation.getString("id");
								log.log(Level.FINE, "ok, actual relation is equal length in administrationid, but name is equals to municipality should-be-name, so i use now osm relation-id ==="+found_osm_relation_id+"===");
								count_correct_relations = 0;	// reset number correct relations to 0, so new start to get polygon(parts)
							} else {
								log.log(Level.FINE, "actual relation is other than before and orgin key length is not longer thant before, so actual one will be ignored");
								continue;
							}
						}
					}

					count_correct_relations++;

					actual_polygon_part = rs_relation.getString("polygon_geometry");

						// jetzt ab 19.02.2013 vorab bei deutschen Gemeinden die Admin-Hierarchie aus Tabelle officialkeys holen
					if(		rs_municipalities.getString("country").equals("Bundesrepublik Deutschland") &&
							hierarchy_topdown.equals("")) {			// check, if hierarchy has not found already (in relations with more than one outer-Role, the hierarchy was build n-outer times together
						if( ! local_actual_municipality_officialkeysid.equals("")) {
							String sqlbefehl_officialkeys = "SELECT hierarchy, level from officialkeys where ags = ?;";
							PreparedStatement stmt_officialkeys = con_listofstreets.prepareStatement(sqlbefehl_officialkeys);
							stmt_officialkeys.setString(1, local_actual_municipality_officialkeysid);
							log.log(Level.FINE, "SQL-Statement officialkeys ===" + stmt_officialkeys.toString() + "===");
							ResultSet rs_officialkeys =  stmt_officialkeys.executeQuery();
							Integer count_officialkeys_records = 0;
							while(rs_officialkeys.next()) {
								if((rs_officialkeys.getString("hierarchy") != null) &&
									( ! rs_officialkeys.getString("hierarchy").equals(""))) {
									count_officialkeys_records++;
									if(count_officialkeys_records != 1) {
										log.log(Level.FINE, " cont. actual hierarchy is ==="+rs_officialkeys.getString("hierarchy")+"===");
										log.log(Level.FINE, " cont. sql-string was ==="+sqlbefehl_officialkeys+"===");
									} else {
										log.log(Level.FINE, "found hierarchy from table officialkeys ==="+rs_officialkeys.getString("hierarchy")+"===");
	
										if( ! hierarchy_topdown.equals(""))
											hierarchy_topdown = "," + hierarchy_topdown;
										hierarchy_topdown = rs_officialkeys.getString("hierarchy") + hierarchy_topdown;
										if(rs_officialkeys.getInt("level") != 0) {
											hierarchy_topdown_level = rs_officialkeys.getInt("level");
											log.log(Level.FINE, "set hierarchy_topdown_level to "+hierarchy_topdown_level);
										}
										log.log(Level.FINE, "build hierarchy_topdown final, when found municipality in table officialkeys ==="+hierarchy_topdown+"===");
										break;
									}
								}
							}
							if(count_officialkeys_records == 0) {
								String local_messagetext = "Municipality with Gemeindeschluessel '"+local_actual_municipality_officialkeysid+"' wasn't found in official admin table for germany, please check Value for correctness";
								new LogMessage(LogMessage.CLASS_ERROR, "create_municipality_polygons", rs_municipalities.getString("name"), rs_municipalities.getLong("id"), local_messagetext);
							}
						} else {
							if( (rs_municipalities.getString("name").indexOf(" OT ") == -1) && 
								( rs_municipalities.getString("name").indexOf(" SB ") == -1) && 
								( rs_municipalities.getString("name").indexOf(" ST ") == -1)) {
								String local_messagetext = "German Municipality with Name '"+rs_municipalities.getString("name")+"' has no Gemeindeschluessel and seems not to be a suburb, please check, if missing Gemeindeschluessel is correct";
								new LogMessage(LogMessage.CLASS_ERROR, "create_municipality_polygons", rs_municipalities.getString("name"), rs_municipalities.getLong("id"), local_messagetext);
							}
						}
					}


					if( hierarchy_topdown.equals("")) {
							// if is_in is available, it has the complete hierarchy in standard OSM-Format low-up-to-high, so traverse hierarchy and store in hierarchy_topdown
						String local_is_in_key = "is_in";
						if(rs_relation.getString(local_is_in_key) != null) {
							String local_hierarchy_string = rs_relation.getString(local_is_in_key).replace(", ",",");
							String[] local_hierarchy_elements = local_hierarchy_string.split(",");
							for(Integer hierarchyi=0;hierarchyi<local_hierarchy_elements.length;hierarchyi++) {
								String act_hierarchy_element = local_hierarchy_elements[hierarchyi];
								if(act_hierarchy_element.equals("Europe") || act_hierarchy_element.equals("Europa")) {
									log.log(Level.FINE, "#"+hierarchyi+"    act_hierarchy_element ==="+act_hierarchy_element+"=== will be ignored at too-top-level-hierarchy");
									continue;
								}
								log.log(Level.FINE, "#"+hierarchyi+"    act_hierarchy_element ==="+act_hierarchy_element+"=== von gesamt ==="+local_hierarchy_string+"===");
								if( ! hierarchy_topdown.equals(""))
									hierarchy_topdown = "," + hierarchy_topdown;
								hierarchy_topdown = act_hierarchy_element + hierarchy_topdown;
								hierarchy_topdown_level++;
							}
							hierarchy_topdown_level++;	// 1 Level more for the municipality itself
							if(rs_municipalities.getString("name").indexOf(" OT ") != -1)
								hierarchy_topdown_level += 1;	// 1 Level more for the Sub-structure
							if(rs_municipalities.getString("name").indexOf(" ST ") != -1)
								hierarchy_topdown_level += 1;	// 1 Level more for the Sub-structure
							if(rs_municipalities.getString("name").indexOf(" SB ") != -1)
								hierarchy_topdown_level += 2;	// 2 Level more for the Sub-structure

							log.log(Level.FINE, "set hierarchy_topdown_level to (is_in) "+hierarchy_topdown_level);
							if( ! hierarchy_topdown.equals(""))
								log.log(Level.FINE, "Info: in relation found municipaliy_hierarchy information, Case is_in:  complete hierarchy ==="+hierarchy_topdown+"===");
						}
	
							// if is_in:... is in some special tags, get it in high-to-low serial action and store in hierarchy_topdown
						local_is_in_key = "is_in_country";
						if(rs_relation.getString(local_is_in_key) != null) {
							if( ! hierarchy_topdown.equals(""))
								hierarchy_topdown += ",";
							hierarchy_topdown += rs_relation.getString(local_is_in_key);
							hierarchy_topdown_level++;
								// now, 2011-10-01 inside is_in_:country. single is_in:state made wrong hierarchy
							local_is_in_key = "is_in_state";
							if(rs_relation.getString(local_is_in_key) != null) {
									if( ! hierarchy_topdown.equals(""))
										hierarchy_topdown += ",";
									hierarchy_topdown += rs_relation.getString(local_is_in_key);
									hierarchy_topdown_level++;
							}
							local_is_in_key = "is_in_region";
							if(rs_relation.getString(local_is_in_key) != null) {
								if( ! hierarchy_topdown.equals(""))
									hierarchy_topdown += ",";
								hierarchy_topdown += rs_relation.getString(local_is_in_key);
								hierarchy_topdown_level++;
							}
							local_is_in_key = "is_in_county";
							if(rs_relation.getString(local_is_in_key) != null) {
								if( ! hierarchy_topdown.equals(""))
									hierarchy_topdown += ",";
								hierarchy_topdown += rs_relation.getString(local_is_in_key);
								hierarchy_topdown_level++;
							}
							if( ! hierarchy_topdown.equals(""))
							if(rs_municipalities.getString("name").indexOf(" OT ") != -1)
								hierarchy_topdown_level += 1;	// 1 Level more for the Sub-structure
							if(rs_municipalities.getString("name").indexOf(" ST ") != -1)
								hierarchy_topdown_level += 1;	// 1 Level more for the Sub-structure
							if(rs_municipalities.getString("name").indexOf(" SB ") != -1)
								hierarchy_topdown_level += 2;	// 2 Level more for the Sub-structure
							log.log(Level.FINE, "Info: in relation found municipaliy_hierarchy information, Case is_in:...  :  complete hierarchy ==="+hierarchy_topdown+"===");
							hierarchy_topdown_level++;	// 1 Level more for the municipality itself
							log.log(Level.FINE, "set hierarchy_topdown_level to (is_in:...) "+hierarchy_topdown_level);
						}
					}

					if( rs_relation.getString("name") != null && (! rs_relation.getString("name").equals(rs_municipalities.getString("name")))) {
						String local_name_test = rs_relation.getString("name");
						if(local_name_test.indexOf(rs_municipalities.getString("name")) == 0) {
							String local_name_suffix = local_name_test.replace(rs_municipalities.getString("name"),"");
							if( ! local_name_suffix.equals("")) {
								log.log(Level.FINE, "place objects hat municipality name ==="+rs_municipalities.getString("name")+"=== but contains suffix ==="+local_name_suffix+"=== ...");
								boolean name_suffix_ok = false;
								if(local_name_suffix.indexOf(" im ") == 0)
									name_suffix_ok =true;
								if(local_name_suffix.indexOf(" ob ") == 0)
									name_suffix_ok =true;
								if(local_name_suffix.indexOf(" bei ") == 0)
									name_suffix_ok =true;
								if(local_name_suffix.indexOf(", Stadt") == 0)
									name_suffix_ok =true;
								if( ! name_suffix_ok) {
									log.log(Level.WARNING, "WARNING: Difference in name of relation ==="+rs_relation.getString("name")+"=== to name of municipality ==="+rs_municipalities.getString("name")+"===");
								}											
							}
						}
					}

					Date time_relationanalyse_endetime = new Date();
					log.log(Level.FINEST, "Time: time to find out, if relation-id is correct in ms: "+(time_relationanalyse_endetime.getTime()-time_municipality_startedtime.getTime()));

					if( ! municipality_officialkeysid.equals(""))
						log.log(Level.FINE, "Info: got municipality administrationid (german gemeindeschluessel) ==="+municipality_officialkeysid+"===");
					if(count_correct_relations == 1) {
							// polygon first or only part, safe in extra variable
						complete_polygon = actual_polygon_part;
						complete_polygon_idlist = rs_relation.getString("id");
						log.log(Level.FINE, "ok, got first part of relation");
					} else if(count_correct_relations > 1) {
						log.log(Level.FINE, "ok, got another part of relation");

						// polygon another part, now union with previous part(s) as a multipolygon
						log.log(Level.WARNING, "Warning: got more than one relation with relation id ==="+rs_municipalities.getString("osm_relation_id")+"=== Select statement was ==="+selectbefehl_relation+"===");


						String temp_create_multipolygon_sql = "SELECT ST_AsText(?) as completepoly, ST_AsText(?) as polypart;";
						log.log(Level.FINE, "temp_create_multipolygon_sql==="+temp_create_multipolygon_sql+"===");
						try {
							PreparedStatement temp_stmt_create_multipolygon = con_listofstreets.prepareStatement(temp_create_multipolygon_sql);
							temp_stmt_create_multipolygon.setString(1, complete_polygon);
							temp_stmt_create_multipolygon.setString(1, actual_polygon_part);
							ResultSet temp_rs_create_multipolygon = temp_stmt_create_multipolygon.executeQuery(  );
							if( temp_rs_create_multipolygon.next() ) {
								log.log(Level.FINE, "ok, completepoly ==="+temp_rs_create_multipolygon.getString("completepoly")+"===");
								log.log(Level.FINE, "ok, polypart ==="+temp_rs_create_multipolygon.getString("polypart")+"===");
							}
						}	// ende try DB-connect
						catch( SQLException e) {
							log.log(Level.SEVERE, "ERROR occured when tried to get both polygon-parts as text. SQL-Statement was ==="+temp_create_multipolygon_sql+"===");
							log.log(Level.SEVERE, e.toString());
							System.out.println("ERROR occured when tried to get both polygon-parts as text. SQL-Statement was ==="+temp_create_multipolygon_sql+"===");
							System.out.println(e.toString());
							relation_wrong = "invalid-multipolygon";
							String local_messagetext = "actual polygon-part with osm-id " + rs_relation.getString("id") + " could not be get as ST_AsText from database";
							new LogMessage(LogMessage.CLASS_ERROR, "create_municipality_polygons", rs_municipalities.getString("name"), rs_municipalities.getLong("id"), local_messagetext);
						}



						//production String create_multipolygon_sql = "SELECT ST_Union('"+complete_polygon+"','"+actual_polygon_part+"') as unionpolygon, ";
						String create_multipolygon_sql = "SELECT ST_Union(?, ?) as unionpolygon, ";
						create_multipolygon_sql += "ST_AsText(ST_Union(?, ?)) as justfordebug_unionpolygon_astext;";
						try {
							PreparedStatement stmt_create_multipolygon = con_listofstreets.prepareStatement(create_multipolygon_sql);
							stmt_create_multipolygon.setString(1, actual_polygon_part);
							stmt_create_multipolygon.setString(2, complete_polygon);
							stmt_create_multipolygon.setString(3, complete_polygon);
							stmt_create_multipolygon.setString(4, actual_polygon_part);
							log.log(Level.FINE, "create_multipolygon_sql===" + stmt_create_multipolygon.toString() + "===");
							ResultSet rs_create_multipolygon = stmt_create_multipolygon.executeQuery();
							if( rs_create_multipolygon.next() ) {
								boolean relationsidsvorhanden = false;
								String idlist_array[] = complete_polygon_idlist.split(",");
								for(Integer idlistindex = 0; idlistindex < idlist_array.length; idlistindex++) {
									if(idlist_array[idlistindex].equals(rs_relation.getString("id"))) {
										relationsidsvorhanden = true;
										break;
									}
								}
								if(!relationsidsvorhanden) {
									complete_polygon_idlist += "," + rs_relation.getString("id");	// add actual polygon osm-id to list of all unioned-polygons
								}
								log.log(Level.FINE, "ok, union polygon ==="+rs_create_multipolygon.getString("justfordebug_unionpolygon_astext")+"===");
								complete_polygon = rs_create_multipolygon.getString("unionpolygon");
							}
						}	// ende try DB-connect
						catch( SQLException e) {
							log.log(Level.SEVERE, "ERROR occured when tried to create ST_Union Polygon");
							log.log(Level.SEVERE, " (cont.) here original stack ==="+e.toString()+"===");
							log.log(Level.SEVERE, e.toString());
							System.out.println("ERROR occured when tried to create ST_Union Polygon");
							System.out.println(" (cont.) here original stack ==="+e.toString()+"===");
							System.out.println(e.toString());
							relation_wrong = "invalid-multipolygon";
							String local_messagetext = "actual polygon-part with osm-id " + rs_relation.getString("id") + " could not be build ST_Union with already list of polygon osm-ids " + complete_polygon_idlist;
							new LogMessage(LogMessage.CLASS_ERROR, "create_municipality_polygons", rs_municipalities.getString("name"), rs_municipalities.getLong("id"), local_messagetext);
						}
					}
				} // end over loop of all found relation-records (ends now earlier from 17.11.2011) - while( rs_relation.next() ) {

				if(complete_polygon.equals("")) {
					if((rs_municipalities.getString("country") != null) && 
							(	(rs_municipalities.getString("country").equals("Ísland"))
							||	(rs_municipalities.getString("country").equals("New Zealand"))
							)) {
						log.log(Level.WARNING, "Warnung, Island muncipality relation incomplete or not valid, will be ignored ==="+rs_municipalities.getString("osm_relation_id")+"===");
						System.out.println("Warnung, Island muncipality relation incomplete or not valid, will be ignored ==="+rs_municipalities.getString("osm_relation_id")+"===");
						continue;
					}

					log.log(Level.WARNING, "Error with relation id ==="+rs_municipalities.getString("osm_relation_id")+"===, ignored this municipality");
					System.out.println("Error with relation id ==="+rs_municipalities.getString("osm_relation_id")+"===, ignored this municipality");
					if(relation_wrong.equals(""))
						relation_wrong = "missing_polygon";
					log.log(Level.WARNING, "wrong description ==="+relation_wrong+"===");
					System.out.println("wrong description ==="+relation_wrong+"===");
					String local_messagetext = "Relation with osm-ids "+complete_polygon_idlist+" could not be build to a valid polygon, reason '"+relation_wrong+"'";
					new LogMessage(LogMessage.CLASS_ERROR, "create_municipality_polygons", rs_municipalities.getString("name"), rs_municipalities.getLong("id"), local_messagetext);
						// something went wrong with relation, abort actual municipality and report to record in table municipality
					String updatesql_municipality = "UPDATE municipality SET polygon_state = ? WHERE id = ?;";
					PreparedStatement stmt_updatemunicipality = con_listofstreets.prepareStatement(updatesql_municipality);
					stmt_updatemunicipality.setString(1, relation_wrong);
					stmt_updatemunicipality.setLong(2, rs_municipalities.getLong("id"));
					log.log(Level.FINE, "municipality-Update with set polygon_state to data_missing ==="+updatesql_municipality+"===");
					try {
						stmt_updatemunicipality.executeUpdate();
					}
					catch( SQLException f) {
						log.log(Level.SEVERE, "Error occured during try of update municipality record with id ==="+rs_municipalities.getString("id")+"=== and update string ==="+updatesql_municipality+"===");
						log.log(Level.SEVERE, f.toString());
						System.out.println("Error occured during try of update municipality record with id ==="+rs_municipalities.getString("id")+"=== and update string ==="+updatesql_municipality+"===");
						System.out.println(f.toString());
						return;
					}
					log.log(Level.FINE, "no boundary-geometry found for actual municipality, skip rest of work for this municipality");
						// ok, abort actual municipality
					continue;
				}


				Date time_hierarchyaction_startedtime = new Date();
				Date time_hierarchyaction_endedtime = null;



					// Prio 1: get hierarchy from admin polygons around the municipality
				if( hierarchy_topdown.equals("")) {
	
					Date time_last_step = null;
					Date time_now = null;
	
					time_last_step = new Date();
	
	
					hierarchy_topdown = get_administrative_hierarchy_from_polygons(rs_municipalities.getString("relation_officialkey_key"), complete_polygon_idlist);
					if( ! hierarchy_topdown.equals("")) {
						String[] local_hierarchy_parts = hierarchy_topdown.split(",");
						hierarchy_topdown_level = local_hierarchy_parts.length;
						hierarchy_topdown_level++;	// 1 Level more for the municipality itself
						log.log(Level.FINE, "set hierarchy_topdown_level to (get_...polygons:...) "+hierarchy_topdown_level);
					}
					log.log(Level.FINE, "in main after sub-fkt. get_administrative_hierarchy_from_polygons  var hierarchy_topdown set to "+hierarchy_topdown+"=== hierarchy level ==="+hierarchy_topdown_level);
	
					time_now = new Date();
					log.log(Level.FINEST, "TIME in ms. (get_administrative_hierarchy_from_polygons) "+(time_now.getTime()-time_last_step.getTime()));
					time_last_step = time_now;
				} // if no hierarchical info, tried to get from osm admin-polygon hierarchy - if( hierarchy_topdown.equals("")) {
	
				time_hierarchyaction_endedtime = new Date();
				log.log(Level.FINEST, "Time: time for getting polygon-hierarchy in ms: "+(time_hierarchyaction_endedtime.getTime()-time_hierarchyaction_startedtime.getTime()));
				time_hierarchyaction_startedtime = new Date();
				
				
				
				
				String sqlbefehl_places = "";
				

				
				
				
					// Prio 2: get hierarchy from place nodes with correct municipality id

				if( hierarchy_topdown.equals("")) {
						// get osm place-object (of type node) for getting administration hierarchy - first try with german gemeindeschluessel, if available
					sqlbefehl_places = "";
					if( ! municipality_officialkeysid.equals("")) {

						String local_municipality_officialkeysid_short = municipality_officialkeysid;
						if(rs_municipalities.getString("point_officialkey_key").equals("openGeoDB:community_identification_number")) {
							while( ! local_municipality_officialkeysid_short.equals("")) {
								Integer poslastchar = local_municipality_officialkeysid_short.length()-1;
								if(local_municipality_officialkeysid_short.substring(poslastchar,poslastchar+1).equals("0")) {
									local_municipality_officialkeysid_short = local_municipality_officialkeysid_short.substring(0,poslastchar);
								} else
									break;
							}
						}

						sqlbefehl_places = "SELECT osm_id AS id, name, tags->'is_in' AS is_in, tags->'place' AS place";
						sqlbefehl_places += " FROM planet_point";
						sqlbefehl_places += " WHERE ((tags @> '\"" + rs_municipalities.getString("point_officialkey_key") + "\"=>\""+municipality_officialkeysid+"\"') ";
						if(rs_municipalities.getString("point_officialkey_key").equals("openGeoDB:community_identification_number")) {
								// only in Germany: if regional number has suffix "0"s then query also short form of the number
							if( ! municipality_officialkeysid.equals(local_municipality_officialkeysid_short)) {
								sqlbefehl_places += "OR (tags @> 'openGeoDB:community_identification_number=>\""+local_municipality_officialkeysid_short+"\"') ";
							}
						}
						sqlbefehl_places += ") AND (tags ? 'place') and ( tags ? 'is_in') and ";
						sqlbefehl_places += "ST_Contains(?, way) ORDER BY name;";
						if( ! municipality_officialkeysid.equals(local_municipality_officialkeysid_short)) {
							log.log(Level.FINE, "Info: also searching for short form of regionalschluessel, in this case sql-string (sqlbefehl_places ==="+sqlbefehl_places+"===");
						}
						PreparedStatement stmt_places = con_mapnik.prepareStatement(sqlbefehl_places);
						stmt_places.setString(1, complete_polygon);
						log.log(Level.FINE, "sqlbefehl_places (osm nodes, regionalschluessel-search) ===" + stmt_places.toString() + "===");

						ResultSet rs_places = stmt_places.executeQuery();
						Integer count_result_places = 0;
						String hierarchy_string = "";
						while( rs_places.next() ) {
							count_result_places++;
							log.log(Level.FINE, "Datensatz #"+count_result_places+"    id: "+rs_places.getLong("id")+"  name ==="+rs_places.getString("name")+"===   is_in ==="+rs_places.getString("is_in")+"=== place ==="+rs_places.getString("place")+"===");
							if(count_result_places == 1) {
								hierarchy_string = rs_places.getString("is_in");
								log.log(Level.FINE, "in resultset place(object) used fix result #1, has name ==="+rs_places.getString("name")+"===  hierarchy_string was ==="+hierarchy_string+"===");
							}
							String local_municipality_name = rs_municipalities.getString("name");
							String local_actrecord_name = "";
							if(rs_places.getString("name") != null)
								local_actrecord_name = rs_places.getString("name");
							log.log(Level.FINE, "municipality_name ==="+local_municipality_name+"===   act place-name ==="+local_actrecord_name+"===");
							if(local_municipality_name.equals(local_actrecord_name)) {
								hierarchy_string = rs_places.getString("is_in");
								log.log(Level.FINE, "in resultset place(object) used result #"+count_result_places+", because has identical name ==="+local_actrecord_name+"===  hierarchy_string was ==="+hierarchy_string+"===");
							} else {
								String local_name_test = local_actrecord_name;
								if(local_actrecord_name.indexOf(local_municipality_name) == 0) {
									String local_name_suffix = local_name_test.replace(local_actrecord_name,"");
									if( ! local_name_suffix.equals("")) {
										log.log(Level.FINE, "place objects hat municipality name ==="+local_municipality_name+"=== but containts suffix ==="+local_name_suffix+"=== ...");
										boolean name_suffix_ok = false;
										if(local_name_suffix.indexOf(" im ") == 0)
											name_suffix_ok =true;
										if(local_name_suffix.indexOf(" ob ") == 0)
											name_suffix_ok =true;
										if(local_name_suffix.indexOf(" bei ") == 0)
											name_suffix_ok =true;
										if(name_suffix_ok) {
											hierarchy_string = rs_places.getString("is_in");
											log.log(Level.FINE, "in resultset place(object) used result #"+count_result_places+", because name ==="+local_actrecord_name+"=== has correct start and suffix ==="+local_name_suffix+"=== seems to be ok  hierarchy_string was ==="+hierarchy_string+"===");
										} else {
											log.log(Level.WARNING, "WARNING: place objects hat municipality name ==="+local_municipality_name+"=== but containts suffix ==="+local_name_suffix+"=== which doesnt hit");
										}											
									}
								}
							}
							
						}
						if(count_result_places >= 1) {
							if(count_result_places > 1)
								log.log(Level.WARNING, "WARNING: more than one place-objects found, please check, if used correct one!!!");
							hierarchy_string = hierarchy_string.replace(", ",",");
							String[] hierarchy_elements = hierarchy_string.split(",");
							for(Integer hierarchyi=0;hierarchyi<hierarchy_elements.length;hierarchyi++) {
								String act_hierarchy_element = hierarchy_elements[hierarchyi];
								if(act_hierarchy_element.equals("Europe")) {
									log.log(Level.FINE, "#"+hierarchyi+"    act_hierarchy_element ==="+act_hierarchy_element+"=== will be ignored at too-top-level-hierarchy");
									continue;
								}
								log.log(Level.FINE, "#"+hierarchyi+"    act_hierarchy_element ==="+act_hierarchy_element+"=== von gesamt ==="+hierarchy_string+"===");
								if( ! hierarchy_topdown.equals(""))
									hierarchy_topdown = "," + hierarchy_topdown;
								hierarchy_topdown = act_hierarchy_element + hierarchy_topdown;
								hierarchy_topdown_level++;
							}
							hierarchy_topdown_level++;	// 1 Level more for the municipality itself
							if(rs_municipalities.getString("name").indexOf(" OT ") != -1)
								hierarchy_topdown_level += 1;	// 1 Level more for the Sub-structure
							if(rs_municipalities.getString("name").indexOf(" ST ") != -1)
								hierarchy_topdown_level += 1;	// 1 Level more for the Sub-structure
							if(rs_municipalities.getString("name").indexOf(" SB ") != -1)
								hierarchy_topdown_level += 2;	// 2 Level more for the Sub-structure
							log.log(Level.FINE, "set hierarchy_topdown_level to (unknown typ:...) "+hierarchy_topdown_level);
						}
					}

					time_hierarchyaction_endedtime = new Date();
					log.log(Level.FINEST, "Time: time for 1. action in ms: "+(time_hierarchyaction_endedtime.getTime()-time_hierarchyaction_startedtime.getTime()));
					time_hierarchyaction_startedtime = new Date();
				}


					// Prio 3: get hierarchy from place nodes with identically name

				if( hierarchy_topdown.equals("")) {
					sqlbefehl_places = "SELECT osm_id AS id, name, tags->'is_in' AS is_in, tags->'place' AS place";
					sqlbefehl_places += " FROM planet_point";
					sqlbefehl_places += " WHERE ST_Contains(?, way) AND name = ?";
					sqlbefehl_places += " AND (tags ? 'place') AND (tags ? 'is_in')";
					sqlbefehl_places += " ORDER BY name;";

					PreparedStatement stmt_places = con_mapnik.prepareStatement(sqlbefehl_places);
					stmt_places.setString(1, complete_polygon);
					stmt_places.setString(2, rs_municipalities.getString("name"));
					log.log(Level.FINE, "sqlbefehl_places (osm nodes, name-search) ==="+sqlbefehl_places+"===");
					ResultSet rs_places = stmt_places.executeQuery( sqlbefehl_places );
					String hierarchy_string = "";
					Integer count_result_places = 0;
					while( rs_places.next() ) {
						count_result_places++;
						log.log(Level.FINE, "Datensatz #"+count_result_places+"    id: "+rs_places.getLong("id")+"  name ==="+rs_places.getString("name")+"===   is_in ==="+rs_places.getString("is_in")+"=== place ==="+rs_places.getString("place")+"===");
						if(count_result_places == 1)
							hierarchy_string = rs_places.getString("is_in");
					}
					if(count_result_places >= 1) {
						hierarchy_string = hierarchy_string.replace(", ",",");
						String[] hierarchy_elements = hierarchy_string.split(",");
						for(Integer hierarchyi=0;hierarchyi<hierarchy_elements.length;hierarchyi++) {
							String act_hierarchy_element = hierarchy_elements[hierarchyi];
							if(act_hierarchy_element.equals("Europe")) {
								log.log(Level.FINE, "#"+hierarchyi+"    act_hierarchy_element ==="+act_hierarchy_element+"=== will be ignored at too-top-level-hierarchy");
								continue;
							}
							log.log(Level.FINE, "#"+hierarchyi+"    act_hierarchy_element ==="+act_hierarchy_element+"=== von gesamt ==="+hierarchy_string+"===");
							if( ! hierarchy_topdown.equals(""))
								hierarchy_topdown = "," + hierarchy_topdown;
							hierarchy_topdown = act_hierarchy_element + hierarchy_topdown;
							hierarchy_topdown_level++;
						}
						hierarchy_topdown_level++;	// 1 Level more for the municipality itself
						log.log(Level.FINE, "set hierarchy_topdown_level to (unkown2:...) "+hierarchy_topdown_level);
					}
				} // end of 2nd try: finding place-objects with hierarchical info - if( hierarchy_topdown.equals("")) {

				time_hierarchyaction_endedtime = new Date();
				log.log(Level.FINEST, "Time: time for postgis polygon build in ms: "+(time_hierarchyaction_endedtime.getTime()-time_hierarchyaction_startedtime.getTime()));
				time_hierarchyaction_startedtime = new Date();



					// Prio 4: get hierarchy data from nominatim search api

				if( hierarchy_topdown.equals("")) {

					Date time_last_step = null;
					Date time_now = null;


					time_last_step = new Date();


					hierarchy_topdown = get_administrative_hierarchy_from_nominatim(rs_municipalities.getString("name"),complete_polygon);
					hierarchy_topdown = hierarchy_topdown.replace(", ",",");
					if( ! hierarchy_topdown.equals("")) {
						String[] local_hierarchy_parts = hierarchy_topdown.split(",");
						hierarchy_topdown_level = local_hierarchy_parts.length;
						hierarchy_topdown_level++;	// 1 Level more for the municipality itself
						log.log(Level.FINE, "set hierarchy_topdown_level to (get_...nominatim:...) "+hierarchy_topdown_level);
					}

					time_now = new Date();
					log.log(Level.FINEST, "TIME in ms. (get_administrative_hierarchy_from_nominatim) "+(time_now.getTime()-time_last_step.getTime()));
					time_last_step = time_now;
				} // end of tried to get admin-hierarchy from nominatim - if( hierarchy_topdown.equals("")) {

				time_hierarchyaction_endedtime = new Date();
				log.log(Level.FINEST, "Time: time for postgis polygon build in ms: "+(time_hierarchyaction_endedtime.getTime()-time_hierarchyaction_startedtime.getTime()));

					// In Luxembourg, no administrative hierarchy between municipality and country name itself should be used
				if(rs_municipalities.getString("country").equals("Luxembourg"))
					hierarchy_topdown = rs_municipalities.getString("country");

				String updatesql_municipality = "UPDATE municipality SET polygon = ?, ";
				updatesql_municipality += "polygon_state = ?, ";
				updatesql_municipality += "osm_hierarchy = ?";
				if(hierarchy_topdown_level > 0) {
					updatesql_municipality += ", osm_hierarchy_level = ?";
					log.log(Level.FINE, "update municipality for set hierarchy_topdown_level to "+hierarchy_topdown_level);
				} else {
					log.log(Level.FINE, "update municipality hierarchy_topdown_level not set !!!");
				}
				// if municipality_officialkeysid is empty in DB and found, then insert
				if((rs_municipalities.getString("officialkeys_id") == null)  || ( rs_municipalities.getString("officialkeys_id").equals(""))) {
					if( ! municipality_officialkeysid.equals(""))
						updatesql_municipality += ", officialkeys_id = ?";
				}
				if(	( ! found_osm_relation_id.equals("")) && ( ! found_osm_relation_id.equals("indifferent"))) {
					if( found_osm_relation_id.substring(0,1).equals("-"))
					 	found_osm_relation_id = found_osm_relation_id.substring(1);
					updatesql_municipality += ", osm_relation_id = ?";
				}

				updatesql_municipality += " WHERE id = "+rs_municipalities.getString("id")+";";
				PreparedStatement stmt_updatemunicipality = con_listofstreets.prepareStatement(updatesql_municipality);
				statementIndex = 1;
				stmt_updatemunicipality.setString(statementIndex++, complete_polygon);
				stmt_updatemunicipality.setString(statementIndex++, setpolygonstate);
				stmt_updatemunicipality.setString(statementIndex++, hierarchy_topdown);
				if(hierarchy_topdown_level > 0)
					stmt_updatemunicipality.setInt(statementIndex++, hierarchy_topdown_level);
				if((rs_municipalities.getString("officialkeys_id") == null)  || ( rs_municipalities.getString("officialkeys_id").equals(""))) {
					if( ! municipality_officialkeysid.equals("")) {
						stmt_updatemunicipality.setString(statementIndex++, municipality_officialkeysid);
					}
				}
				if(	( ! found_osm_relation_id.equals("")) && ( ! found_osm_relation_id.equals("indifferent"))) {
					stmt_updatemunicipality.setString(statementIndex++, found_osm_relation_id);
				}
				log.log(Level.FINEST, "Gebiet-Updatebefehl ===" + stmt_updatemunicipality.toString() + "===");
				try {
					stmt_updatemunicipality.executeUpdate( updatesql_municipality );
				}
				catch( SQLException f) {
					log.log(Level.SEVERE, "ERROR: sql-exception occured during update sql-statement ==="+updatesql_municipality+"===  IGNORE this municipality");
					log.log(Level.SEVERE, f.toString());
					System.out.println("ERROR: sql-exception occured during update sql-statement ==="+updatesql_municipality+"===  IGNORE this municipality");
					System.out.println(f.toString());
					continue;
				}

				Date time_municipality_endedtime = new Date();
				log.log(Level.FINE, "ENDE Verarbeitung Municipality ==="+rs_municipalities.getString("name")+"=== Duration in sek: "+((time_municipality_endedtime.getTime()-time_municipality_startedtime.getTime())/1000));
				log.log(Level.FINE, "========================================================================================");
			}	// end loop over all Municipality

			Date time_program_endedtime = new Date();
			log.log(Level.INFO, "Program: Ended Time: "+time_formatter.format(time_program_endedtime.getTime()));
			log.log(Level.FINEST, "Program: Duration in ms: "+(time_program_endedtime.getTime()-time_program_startedtime.getTime())/1000);

		}	// ende try DB-connect
		catch( SQLException  e) {
			e.printStackTrace();
			return;
		}
	}
}
