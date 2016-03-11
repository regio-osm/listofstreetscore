package de.regioosm.listofstreetscore.util;
/*
		2013-03-21, Dietmar Seifert
			now a sub-class. It reads the wiki (complete, portions or a single muni) and
			gives now the municipality back to the class-caller or
			stores the wiki article in database (depends on parameter articlelist_destination)

		2012-10-09, Dietmar Seifert
			Production is on regio-osm.de active since about 2012-09-28.
OFFEN:
			einzelne Straßenzeile prüfen bzgl. ", ", dann ist meist/immer ein Ortsteil vorangestellt.
			Entweder direkt nutzen oder als Wiki-Formatfehler ausgeben
			Beispiel "Hirschfeld, Siedlung"

PROBLEM:
	some individual page request results in an error and in this cases, only HeaderField [Content-Type] seems to be parseable
	for checking the error.
	For only some seconds, a message comes, that the wiki-server or the database is not available
			
			
		2012-08-13, Dietmar Seifert
			bring program up to date to new experimental own mediawiki

			Source: get_streetlist_from_osmwiki_t.java, filedate 2012-02-14 10:56:00

-----------------------------------------------------------
		ToDo
			* a) mark street lists with source=osm-wiki (need new column to table municipality)
			* b) after realizing a), mark all rows with source=osm-wiki as "to-be-refreshed" and after import of all pages identify or delete non-actualized pages


		Version 0.1, 2011-06-23, Dietmar Seifert
			first time of documention, Code exists since 2011-06-18, started at Munic Hack Weekend 2011-06-18 to 2011-06-19

		Aim:	get all street list wiki page in wiki.openstreetmap.org, which have category "List_of_street_names"
					first get wiki page with category members
					second get every street list wiki page and import all streets in db

		Table municipality
			column "polygon_state" (new 2011-06-23)
				'ok'		polygon valid and actual - polygon should only be used with this state
				'missing'	start-value if row added. Polygon complete missing
				'invalid'	polygon is not complete or could not be created
				'old'			row was updated, so state of polygon is possible not correct/actual. Should not be used in this state
				'datamissing'		relation completely missing in osm source or relation-id wrong
				'dataincomplete'		relation only partly available, so owm source doesn't complete needed area
				'invalidgeometry'		boundary is not a valid polygon

*/

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import de.regioosm.listofstreetscore.util.LogMessage;
import de.regioosm.listofstreetscore.util.Municipality;


public class Streetlist_from_streetlistwiki {
	public String municipality_country;
	public String municipality_name;
  	public String municipality_administrationid = "";
  	public String municipality_osmrelationid = "";
	public String[] strassen;
	public String[] streetsuburbs;
	public Integer strassenanzahl;
	public String source_url = "";
	public String password_status = "";
	public String source_text = "";
	public String source_deliverydate = "";
	public String source_source = "";
	public String source_filedate = "";
	public Integer maxlag = 0;

	static Applicationconfiguration configuration = new Applicationconfiguration();

	private TreeMap<String, String> wiki_namespaces = new TreeMap<String, String>();

	public static final Logger logger = Logger.getLogger(Streetlist_from_streetlistwiki.class.getName());
	
	public Streetlist_from_streetlistwiki() {
		wiki_namespaces.put("Brasil", "BR");
		wiki_namespaces.put("Ísland", "IS");
		wiki_namespaces.put("Österreich", "AT");
		wiki_namespaces.put("România", "RO");
	}
	
	public static void waiting (int n) {
		long t0, t1;
		t0 =  System.currentTimeMillis();
		System.out.println("waiting starts for "+n+" Seconds");
		do{
			t1 = System.currentTimeMillis();
		}
		while ((t1 - t0) < (n * 1000));
		System.out.println("waiting ended");
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



		/**
		  *	reads the osm-listofstreets wiki pages and checks for every page, if it is newer than
		  *	the version in database. The newer wiki page content will be stored in database,
		  *	if param articlelist isn't set 
		  * @param wikireadmode
		  *          "complete" get all pages
		  *          "single" get one page
		  * @param municipalityname
		  * 		  restricts access to one or some pages (wildcard * possible)
		  * @param articlelist_destination
		  * 		  if set to "methodoutput", the wiki page will not be updated directly in database,
		  *          but will be sent back to caller
		  */
	public String read (String wikireadmode, String municipalityname, String country, String articlelist_destination) throws Exception {
		String				url_string = "";
		String				act_url_string = "";
		HttpURLConnection		urlConn = null; 
		URL					url_streetlists; 
		HttpURLConnection 		urlConn_streetlists = null; 
		BufferedReader		dis;
		InputStreamReader	isir;
		String				active_url;
		TreeMap<String, String> articles_gemeindeschluessel = new TreeMap<String, String>();
		
		try {

			Handler handler = new ConsoleHandler();
			handler.setLevel(configuration.logging_console_level);
			logger.addHandler( handler );
			FileHandler fhandler = new FileHandler(configuration.logging_filename);
			fhandler.setLevel(configuration.logging_file_level);
			logger.addHandler( fhandler );
			logger.setLevel(configuration.logging_console_level);
		} 
		catch (IOException e) {
			System.out.println("Fehler beim Logging-Handler erstellen, ...");
			System.out.println(e.toString());
		}
		
		this.municipality_country = "";
		this.municipality_name = "";
		this.municipality_administrationid = "";
		this.municipality_osmrelationid = "";
		this.strassenanzahl = 0;
		this.source_url = "";
		this.password_status = "";
		this.source_text = "";
		this.source_deliverydate = "";
		this.source_source = "";
		this.source_filedate = "";

		String default_wiki_timestamp = "2012-09-28T00:01:02Z";	// default time, if no time will be get from website request page

		if(!country.equals("Bundesrepublik Deutschland") && (!country.equals(""))) {
			if(wiki_namespaces.get(country) != null) {
				municipalityname = wiki_namespaces.get(country) + ":" + municipalityname;
				System.out.println("expanded input param municipalityname with country prefix to ==="+municipalityname+"===");
			} else {
				System.out.println("FATAL ERROR in Streetlist_from_streetlistwiki: requested country ===" + country + "=== has no Wiki Abbreviation, sorry");
				return "";
			}
		}
		
		StringBuffer recentchangescontent = new StringBuffer();
		String inputline = "";

		//Calendar cal = Calendar.getInstance();
		java.util.Date time_program_startedtime = new java.util.Date();
		DateFormat time_formatter = new SimpleDateFormat();
		System.out.println("Program: Started Time: "+time_formatter.format(time_program_startedtime));
			// Source-Format: [Last-modified] ===Fri, 28 Sep 2012 22:02:15 GMT===
		DateFormat time_formatter_weekday_mesz = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",new Locale("en"));
		//java.util.Date temp_time = new java.util.Date();
		//System.out.println("test zeit weekday_mesz ==="+time_formatter_weekday_mesz.format(temp_time)+"===");

		
		// Destination-format: ===2012-09-28T00:01:02Z===
		DateFormat time_formatter_iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"); 
		
		
		String[] titelliste = new String[20000];
		String[] timestampliste = new String[20000];
		Integer liste_anzahl = 0;

		try
		{
			if(	configuration.servername.equals("regio-osm.de") || 
				configuration.servername.equals("nb11")) {
				if(wikireadmode.equals("complete"))
					url_string = "http://regio-osm.de/listofstreets_wiki/api.php?action=query&format=xml&cmlimit=50&list=categorymembers&cmtitle=Category:Stra%C3%9Fenliste";
				else
					if( ! municipalityname.equals("")) {
						url_string = "http://regio-osm.de/listofstreets_wiki/api.php?action=query&format=xml&&titles="
								+ municipalityname.replace(" ","_");
					} else {
						url_string = "http://regio-osm.de/listofstreets_wiki/api.php?action=query&list=recentchanges&format=xml&rcdir=older&rctoponly&rcnamespace=0&rclimit=500";
					}
			} else {
				if(wikireadmode.equals("complete"))
					url_string = "http://localhost/wiki/api.php?action=query&format=xml&cmlimit=50&list=categorymembers&cmtitle=Category:Stra%C3%9Fenliste";
				else
					url_string = "http://localhost/wiki/api.php?action=query&list=recentchanges&format=xml&rcdir=older&rctoponly&rcnamespace=0&rclimit=500";
			}
			if(this.maxlag > 0)
				url_string  += "&maxlag=" + this.maxlag;
			System.out.println("url string for getting overview ==="+url_string+"===");

				// Übersicht neueste 500 Artikel
				// http://localhost/wiki/api.php?action=query&list=recentchanges&format=xml&rcdir=newer&rctoponly&rclimit=500

				// Demo-URL zum ermitteln Details zur einzelnen Seite (Revision, last changed etc.)
				//http://localhost/wiki/api.php?action=query&format=txt&titles=Teststadt4&prop=info

			String request_continue_string = "";
			act_url_string = "";
			URL act_url = null;
			while (1 == 1) {
				act_url_string = url_string;
				if( ! request_continue_string.equals(""))
					act_url_string += "&cmcontinue=" + request_continue_string;
				logger.log(Level.INFO, "act_url_string ==="+act_url_string+"===");
				act_url = new URL(act_url_string);
				urlConn = (HttpURLConnection) act_url.openConnection();
				urlConn.setDoInput(true); 
				urlConn.setUseCaches(false);
				urlConn.setRequestProperty("User-Agent", "OSM-Hausnummernauswertung Wikimodul (http:regio-osm.de/hausnummerauswertung)"); // Do as if you're using Firefox 3.6.3.
				urlConn.setRequestProperty("Keep-Alive", "timeout=1, max=2");
				urlConn.setRequestProperty("Connection", "Keep-Alive");
				waiting(this.maxlag);
				recentchangescontent.delete(0, recentchangescontent.length());
				dis = new BufferedReader(new InputStreamReader(urlConn.getInputStream(),"UTF-8"));
				while ((inputline = dis.readLine()) != null)
				{ 
					recentchangescontent.append(inputline + "\n");
				} 
				dis.close();
				urlConn.disconnect();
				logger.log(Level.FINE, "File Recentchanges length: "+recentchangescontent.length());

				DocumentBuilderFactory factory = null;
				DocumentBuilder builder = null;
				Document xml_document = null;

				logger.log(Level.FINEST, "Info: got this xml-content from wiki api-call ==="+recentchangescontent.toString()+"===");

				try {
					factory = DocumentBuilderFactory.newInstance();
					builder = factory.newDocumentBuilder();
					// parse xml-document. got a few lines above as result from mapquest quickodbl api-call
					xml_document = builder.parse(new InputSource(new StringReader(recentchangescontent.toString())));
						// BEGIN temporary output of all response headerfield
					logger.log(Level.FINEST, ".getXMLEncoding ==="+xml_document.getXmlEncoding()+"===");
					logger.log(Level.FINEST, ".getTextContent ==="+xml_document.getTextContent()+"===");
					logger.log(Level.FINEST, ".getInputEncoding ==="+xml_document.getInputEncoding() +"===");
					logger.log(Level.FINEST, ".getXmlVersion==="+xml_document.getXmlVersion() +"===");
					logger.log(Level.FINEST, ".getBaseURI==="+xml_document.getBaseURI() +"===");
					logger.log(Level.FINEST, ".getDocumentURI==="+xml_document.getDocumentURI() +"===");
					logger.log(Level.FINEST, ".getLocalName==="+xml_document.getLocalName() +"===");
					logger.log(Level.FINEST, ".getNamespaceURI==="+xml_document.getNamespaceURI() +"===");
						// END temporary output of all response headerfield
				} 
				catch (org.xml.sax.SAXException saxerror) {
					logger.log(Level.SEVERE, "ERROR: SAX-Exception during parsing of xml-content ==="+recentchangescontent.toString()+"===");
					logger.log(Level.SEVERE, saxerror.toString());
					System.out.println("ERROR: SAX-Exception during parsing of xml-content ==="+recentchangescontent.toString()+"===");
					saxerror.printStackTrace();
					throw new org.xml.sax.SAXException();
				} 
				catch (IOException ioerror) {
					logger.log(Level.SEVERE, "ERROR: IO-Exception during parsing of xml-content ==="+recentchangescontent.toString()+"===");
					logger.log(Level.SEVERE, ioerror.toString());
					System.out.println("ERROR: IO-Exception during parsing of xml-content ==="+recentchangescontent.toString()+"===");
					ioerror.printStackTrace();
					return "";
				}
				catch (ParserConfigurationException parseerror) {
					logger.log(Level.SEVERE, "ERROR: fail to get new Instance from DocumentBuilderFactory");
					logger.log(Level.SEVERE, parseerror.toString());
					System.out.println("ERROR: fail to get new Instance from DocumentBuilderFactory");
					parseerror.printStackTrace();
					return "";
				}
	
				NodeList rc_objects;

				if(wikireadmode.equals("complete"))
					rc_objects = xml_document.getElementsByTagName("cm");		// cm-object when used query categorymembers
				else {
					if( ! municipalityname.equals("")) {
						rc_objects = xml_document.getElementsByTagName("page");		// page-object when used name
					} else {
						rc_objects = xml_document.getElementsByTagName("rc");		// rc-object when used recentchanges
					}
				}


				int number_rc_objects = rc_objects.getLength();
					// go over every found rc-object
				for (int i = 0; i < number_rc_objects; i++) {
					Node rc_node = (Node) rc_objects.item(i);
					NamedNodeMap rc_node_attrs = rc_node.getAttributes();  

					String act_title = "";
					String act_timestamp = default_wiki_timestamp;	// default time, if no time will be get from website request page
					for(int attri = 0 ; attri<rc_node_attrs.getLength() ; attri++) {
						Attr act_attribute = (Attr) rc_node_attrs.item(attri);
						if(act_attribute.getName().equals("title")) {
							act_title = act_attribute.getValue();
						}
						if(act_attribute.getName().equals("timestamp")) {
							act_timestamp = act_attribute.getValue();
						}
					}

					if( 	(! act_title.equals("")) || 
							(! act_timestamp.equals(""))) {
						logger.log(Level.FINE, "+ Titel ==="+act_title+"===      Timestamp ==="+act_timestamp+"===");
						if(	! municipalityname.equals("")) {
							if( ! act_title.equals(municipalityname)) {
								logger.log(Level.FINEST, "Info: Title in recentchanges-List will be ignored, because title ==="+act_title+"=== doesnt fit to commandline -name value ==="+municipalityname+"===");
								if((wiki_namespaces.get(country) != null) && act_title.substring(0,2).equals(wiki_namespaces.get(country)))
									logger.log(Level.FINEST, "correct country, but wrong title");
								continue;
							}
						}
						titelliste[liste_anzahl] = act_title;
						timestampliste[liste_anzahl] = act_timestamp;
						liste_anzahl++;
					}
				}	// end of loop over all objects in url-response

				NodeList category_objects;

				request_continue_string = "";
					// ----- now check in mode wikireadmode=complete only, if there are more pages to request 
					// (at least in wikireadmode=complete, more than one page has to get)
					// the actual page has <categorymembers> two-times.
					// In second one there is the information abount continuation pages
				if(wikireadmode.equals("complete")) {
					category_objects = xml_document.getElementsByTagName("categorymembers");		// cm-object when used query categorymembers
					int number_category_objects = category_objects.getLength();
						// go over every found rc-object
					for (int i = 0; i < number_category_objects; i++) {
						Node category_node = (Node) category_objects.item(i);
						NamedNodeMap category_node_attrs = category_node.getAttributes();  
						logger.log(Level.FINEST, " category-node  Attributes (Number: "+category_node_attrs.getLength()+") are:");
						for(int attri = 0 ; attri<category_node_attrs.getLength() ; attri++) {
							Attr act_attribute = (Attr) category_node_attrs.item(attri);
							logger.log(Level.FINEST, "* [" + act_attribute.getName()+"] ==="+act_attribute.getValue()+"===");
							if(act_attribute.getName().equals("cmcontinue")) {
								request_continue_string = act_attribute.getValue();
							}
						}
					}	// end of loop over all objects in url-response
				}
//ToDo sonst im recentchanges Fall Element beachten: <query-continue><recentchanges rcstart="2013-11-25T18:14:05Z"/></query-continue>
				if(request_continue_string.equals("")) {
					logger.log(Level.INFO, "there wasn't a continue-attribut in response, so stop requests");
					break;
				} else
					logger.log(Level.INFO, "found continue-string ==="+request_continue_string+"===");


			}	// end of loop over all url-request (more than once, if not all objects fitted in one request

			Pattern wildcard_pattern = Pattern.compile("\\s");

			Municipality municipality_import = new Municipality();
			municipality_import.hallo();

			for(Integer titelindex=0;titelindex<liste_anzahl;titelindex++) {

				logger.log(Level.FINE, "========================================================================================");
				logger.log(Level.FINE, "========================================================================================");
				logger.log(Level.FINE, "   Job #"+(titelindex+1)+"   =" + titelliste[titelindex] +"=  ...");
				logger.log(Level.FINE, "========================================================================================");

				if(	configuration.servername.equals("regio-osm.de") || 
					configuration.servername.equals("nb11")) {
					active_url = "http://regio-osm.de/listofstreets_wiki/index.php?title="+titelliste[titelindex].replace(" ","_");
				} else {
					active_url = "http://localhost/wiki/index.php?title="+titelliste[titelindex].replace(" ","_");
				}
				if(this.maxlag > 0)
					active_url += "&maxlag=" + this.maxlag;
			  	url_streetlists = new URL(active_url +"&action=raw");
			  	logger.log(Level.FINE, "request url ===" + url_streetlists + "===");
				try {
					urlConn_streetlists = (HttpURLConnection) url_streetlists.openConnection(); 
				    urlConn_streetlists.setDoInput(true); 
				    urlConn_streetlists.setUseCaches(false);
				    urlConn_streetlists.setRequestProperty("User-Agent", "OSM-Hausnummernauswertung Wikimodul (http:regio-osm.de/hausnummerauswertung)"); // Do as if you're using Firefox 3.6.3.
				    urlConn_streetlists.setRequestProperty("Keep-Alive", "timeout=1, max=2");
				    urlConn_streetlists.setRequestProperty("Connection", "Keep-Alive");
					waiting(this.maxlag);
				
					String zustand = "";

				  	String[] strassen = new String[20000];
				  	String[] streetsuburbs = new String[20000];
				  	Integer strassenanzahl = 0;
//TODO Land
				  	String namespace_prefix = "";
				  	String municipality_country = "Bundesrepublik Deutschland";		// default, if missing on page
				  	String municipality_name = "";
				  	municipality_name = titelliste[titelindex];
				  	if(municipality_name.indexOf(":") == 2) {
				  		namespace_prefix = municipality_name.substring(0,2).toUpperCase();
				  		boolean country_found = false;
						for (Map.Entry<String,String> entry : wiki_namespaces.entrySet()) {
							String value = entry.getValue();
							String key = entry.getKey();
						  	if(namespace_prefix.equals(value)) {
						  		municipality_country = key;
						  		country_found = true;
						  		break;
						  	}
						}
					  	if(!country_found) {
					  		logger.log(Level.SEVERE, "ERROR: unsupported Wiki namespace suffix ==="+namespace_prefix+"===");
					  		System.out.println("ERROR: unsupported Wiki namespace suffix ==="+namespace_prefix+"===");
							String local_messagetext = "ERROR: unsupported Wiki namespace suffix ==="+namespace_prefix+"===";
							new LogMessage(LogMessage.CLASS_ERROR, "Streetlist_from_streetlistwiki", municipality_name, -1L, local_messagetext);									
					  	}
					  	municipality_name = titelliste[titelindex].substring(3);
				  	}
				  	String municipality_submunicipality_permanent = ""; // will be set, if found a sub-heading or other defined special line 
				  	String municipality_submunicipality = "";
				  	String municipality_gemeindeschluessel = "";
				  	String municipality_osmrelationid = "";
				  	Integer firstheadinglevel = 0;
				  	Integer actheadinglevel = 0;

					boolean wikipage_ok = true;

					isir = new InputStreamReader(urlConn_streetlists.getInputStream(),"UTF-8");
					logger.log(Level.FINEST, "Encoding ist mit utf8angabe==="+isir.getEncoding()+"===");
						// BEGIN temporary output all html response header lines
					Integer headeri = 0;
					while(urlConn_streetlists.getHeaderField(headeri) != null) {
						logger.log(Level.FINEST, "Headerfield #"+headeri+":  ["+urlConn_streetlists.getHeaderFieldKey(headeri)+"] ==="+urlConn_streetlists.getHeaderField(headeri)+"===");

							// if wiki-timestamp from actual muni hasn't set yet, 
							// take now url respone headerfield [Last-modified], it contains really last wiki change timestamp
						if(	(urlConn_streetlists.getHeaderFieldKey(headeri) != null) && 
							(urlConn_streetlists.getHeaderFieldKey(headeri).equals("Last-modified")) && 
							(timestampliste[titelindex].equals(default_wiki_timestamp))) {
								// Source-Format: [Last-modified] ===Fri, 28 Sep 2012 22:02:15 GMT===
								// Destination-format: ===2012-09-28T00:01:02Z===
							String local_server_source_timestring = "";
							try {
								local_server_source_timestring = urlConn_streetlists.getHeaderField(headeri);
								logger.log(Level.FINEST, "original Server Respone timestamp ==="+local_server_source_timestring+"===");
								java.util.Date local_server_time = time_formatter_weekday_mesz.parse(local_server_source_timestring);
								String local_server_destination_timestring = time_formatter_iso8601.format(local_server_time);
								logger.log(Level.FINEST, "destination Server Respone timestamp ==="+local_server_destination_timestring+"===");
								timestampliste[titelindex] = local_server_destination_timestring;
							}
							catch (Exception e) {
								logger.log(Level.SEVERE, "ERROR: failed to parse timestamp ==="+local_server_source_timestring+"===");
								logger.log(Level.SEVERE, e.toString());
								System.out.println("ERROR: failed to parse timestamp ==="+local_server_source_timestring+"===");
								e.printStackTrace();
								if(urlConn != null)
									urlConn.disconnect();
								if(urlConn_streetlists != null) 
									urlConn_streetlists.disconnect();
								isir.close();
								return "";
							}
						
						}

						if(	(urlConn_streetlists.getHeaderFieldKey(headeri) != null) && 
							(urlConn_streetlists.getHeaderFieldKey(headeri).equals("Content-Type"))) {
							String local_contenttype = urlConn_streetlists.getHeaderField(headeri);
							if(local_contenttype.indexOf("text/html") == 0) {	
								Integer count_secs = 20; 
								logger.log(Level.FINE, "WARNING: got contenttype ==="+local_contenttype+"===   so wait now "+count_secs+" Seconds ...");
								waiting(count_secs);
								logger.log(Level.FINE, " ok, wake up after sleep");
								wikipage_ok = false;
							}						
						}

						headeri++;
						if(headeri > 999) {
							logger.log(Level.SEVERE,"Lost connection to streetlist wiki, it didn't responded after 999 tries.");
							System.out.println("ERROR: stopped endless loop");
							break;
						}
					}			// END temporary output all html response header lines
				
					if( ! wikipage_ok) {
						StringBuffer complete_wikipage = new StringBuffer();
						dis = new BufferedReader(isir);
						while ((inputline = dis.readLine()) != null) {
							complete_wikipage.append(inputline + "\n");
						}
						dis.close();
						isir.close();
						logger.log(Level.WARNING, "didn't get wiki page correctly (html-page instead of wiki-format), so ignore actual wiki article, content ==="+complete_wikipage.toString()+"===");
						continue;
					}

					// ok, lets analyse file-records of actual municipality
				boolean kategorie_vorhanden = false;
				Integer count_empty_lines_during_list = 0;
				boolean found_streets_in_loop = false;
				StringBuffer complete_wikipage = new StringBuffer();
				dis = new BufferedReader(isir);
				while ((inputline = dis.readLine()) != null) {
					complete_wikipage.append(inputline + "\n");
					logger.log(Level.FINEST, "zeile ==="+inputline+"===    bei zustand: "+zustand);
					if(inputline.equals("")) {
						if(zustand.equals("listeaktiv") && (strassenanzahl > 0)) {
							count_empty_lines_during_list++;
							if(count_empty_lines_during_list >= 2) {
								zustand = "listenende";
							}
						} else if(zustand.equals("listenende"))
							zustand = "";
						continue;
					} else {
						count_empty_lines_during_list = 0;
					}

					if((inputline.indexOf("[[Kategorie:Straßenliste]]") != -1) || (inputline.indexOf("[[Category:Straßenliste]]") != -1))
						kategorie_vorhanden = true;
						
						// if active line is a heading, get heading level
					if(	(inputline.indexOf("= ") == 0) || 
						(inputline.indexOf("== ") == 0) ||
						(inputline.indexOf("=== ") == 0) ||
						(inputline.indexOf("==== ") == 0) ||
						(inputline.indexOf("===== ") == 0)) {
				  		String temp_line = inputline;
				  		while(temp_line.substring(0,1).equals("=")) {
				  			actheadinglevel++;
				  			temp_line = temp_line.substring(1);
				  		}
				  		logger.log(Level.FINEST, "found heading-level ="+actheadinglevel+"=  line ==="+inputline+"===");
					} else {
						actheadinglevel = 0;
					}

					// if active line contains a heading
					// and active line contains same or higher level than first one the page
					if(	(actheadinglevel != 0) &&		
						(actheadinglevel <= firstheadinglevel)) {
						// then stop reading street-lines

						if(strassenanzahl >= 1) {
							logger.log(Level.FINE, "jetzt bei actheadinglevel <= firstheadinglevel  Straßenanzahl >= 1, konkret: "+strassenanzahl);
							logger.log(Level.FINE, "             municipality_country ==="+municipality_country+"===");
							logger.log(Level.FINE, "                municipality_name ==="+municipality_name+"===");
							logger.log(Level.FINE, "  municipality_gemeindeschluessel ==="+municipality_gemeindeschluessel+"===");
							logger.log(Level.FINE, "      municipality_osmrelationid  ==="+municipality_osmrelationid+"===");
							if(		country.equals("Bundesrepublik Deutschland")
								&& (! municipality_gemeindeschluessel.equals("")) 
								&& (municipality_gemeindeschluessel.length() == 8)) {
									// check and add found gemeindeschluessel in Map. If already existing, report Error in Wiki
								if(articles_gemeindeschluessel.get(municipality_gemeindeschluessel) == null) {
									articles_gemeindeschluessel.put(municipality_gemeindeschluessel, municipality_name);
								} else {
									String local_messagetext = "identical regionalschluessel ==="+municipality_gemeindeschluessel+"=== at more than one artical, acutal article-titel ==="+municipality_name+"===, already found previously ==="+articles_gemeindeschluessel.get(municipality_gemeindeschluessel)+"===";
									logger.log(Level.WARNING, local_messagetext);
									new LogMessage(LogMessage.CLASS_ERROR, "Streetlist_from_streetlistwiki", municipality_name, -1L, local_messagetext);									
									articles_gemeindeschluessel.put(municipality_gemeindeschluessel, articles_gemeindeschluessel.get(municipality_gemeindeschluessel)+"|"+municipality_name);
								}
								
								logger.log(Level.FINE, "checking muni ags ==="+municipality_gemeindeschluessel+"=== ...");
								String local_municipalitynameunique = new GermanyOfficialkeys().get_unique_municipalityname(municipality_gemeindeschluessel);
								if( !local_municipalitynameunique.equals(municipality_name)) {
									logger.log(Level.WARNING, "checked-fail muni ags ==="+municipality_gemeindeschluessel+"===");
									String local_messagetext = "Wiki article has wrong title ==="+municipality_name+"=== instead of database version ==="+local_municipalitynameunique+"===. Article-URL is ==="+active_url+"===";
									new LogMessage(LogMessage.CLASS_ERROR, "Streetlist_from_streetlistwiki", municipality_name, -1L, local_messagetext);
								} else {
									logger.log(Level.FINE, "checked-ok muni ags ==="+municipality_gemeindeschluessel+"===");
								}
							} else {
								logger.log(Level.FINE, "NOT checked muni ags ==="+municipality_gemeindeschluessel+"===");
							}
							if(articlelist_destination.equals("methodoutput")) {
								logger.log(Level.FINEST, "found streetlist will be send back to method-caller ...");
								this.municipality_country = municipality_country;
								this.municipality_name = municipality_name;
								this.municipality_administrationid = municipality_gemeindeschluessel;
								this.municipality_osmrelationid = municipality_osmrelationid;
								this.strassen = strassen;
								this.streetsuburbs = streetsuburbs;
								this.strassenanzahl = strassenanzahl;
								this.source_url = active_url;
								this.password_status = "nein";		//ToDo why ignore possible set password metafield?
								this.source_text = "";
								this.source_deliverydate = "";
								this.source_source = "";
								this.source_filedate = timestampliste[titelindex];
//ToDo don't stop so bad in mode methodoutput, better to end after loop
									// read remain file content
								if(inputline != null) {
									while ((inputline = dis.readLine()) != null) {
										complete_wikipage.append(inputline + "\n");
									}
								}
								dis.close();
								isir.close();
								if(urlConn != null)
									urlConn.disconnect();
								if(urlConn_streetlists != null) 
									urlConn_streetlists.disconnect();
								return complete_wikipage.toString();
							} else {
								municipality_import.save(municipality_country, municipality_name, municipality_gemeindeschluessel, municipality_osmrelationid, strassen, streetsuburbs, strassenanzahl, active_url, "nein", "", "", "", timestampliste[titelindex]);
							}
							//break; aktiv bis 13.10.2012
							municipality_name = "";
							municipality_osmrelationid = "";
							strassenanzahl = 0;
				  			municipality_submunicipality = "";
				  			municipality_gemeindeschluessel = "";
							found_streets_in_loop = true;
							zustand = "listenende";
						} else {
							logger.log(Level.WARNING, "ERROR: no streets found on page, when found another heading line ==="+inputline+"=== which has same or higher heading level than first one on page");
						}
					}
						// if active line contaings a heading
						// and is lower-level then first one on page, take it as suburb / hamlet 
					if(	(actheadinglevel > 0) &&
						(firstheadinglevel > 0) && 
						(actheadinglevel > firstheadinglevel)) {
						municipality_submunicipality_permanent = inputline.substring(inputline.indexOf(" ")+1);
	
						municipality_submunicipality_permanent = inputline;
						logger.log(Level.FINE, "Sub-Municipality found in a line, brutto original ==="+municipality_submunicipality_permanent+"===");
						municipality_submunicipality_permanent = municipality_submunicipality_permanent.substring(municipality_submunicipality_permanent.indexOf(" ")+1);
						if(municipality_submunicipality_permanent.indexOf(" =") != -1) {
							municipality_submunicipality_permanent = municipality_submunicipality_permanent.substring(0,municipality_submunicipality_permanent.indexOf(" ="));
						}
						logger.log(Level.FINE, "Sub-Municipality found in a linve, netto ==="+municipality_submunicipality_permanent+"===");
					}

						// if active line contaings a special comment, take it as suburb / hamlet (old syntax from Florian Lohoff pages)
					if(	inputline.indexOf("; Ortsteil") != -1) {
						municipality_submunicipality_permanent = inputline.substring(0,inputline.indexOf("; Ortsteil")-1);
						municipality_submunicipality_permanent = municipality_submunicipality_permanent.trim();
						logger.log(Level.FINE, "Sub-Municipality found in a line, brutto original ==="+inputline+"===");
						logger.log(Level.FINE, "Sub-Municipality found in a linve, netto ==="+municipality_submunicipality_permanent+"===");
					}
						// if active line contaings a heading
						// and is first heading on page
						// !!! must be last check actheading and firstheading, because firstheading will be set inside
					if(	(actheadinglevel > 0) &&
						(firstheadinglevel == 0)) {

						firstheadinglevel = actheadinglevel;
						if(strassenanzahl >= 1) {
							if(municipality_osmrelationid.equals("") && municipality_gemeindeschluessel.equals("")) {
								String local_messagetext = "Wiki article has neither Gemeindeschluessel nor OSM-Relation-ID. Article-URL is ==="+active_url+"===";
								logger.log(Level.WARNING, local_messagetext);
								new LogMessage(LogMessage.CLASS_ERROR, "Streetlist_from_streetlistwiki", municipality_name, -1L, local_messagetext);
							}

							logger.log(Level.FINE, "jetzt bei ansteigender Heading Straßenanzahl >= 1, konkret: "+strassenanzahl);
							logger.log(Level.FINE, "             municipality_country ==="+municipality_country+"===");
							logger.log(Level.FINE, "                municipality_name ==="+municipality_name+"===");
							logger.log(Level.FINE, "  municipality_gemeindeschluessel ==="+municipality_gemeindeschluessel+"===");
							logger.log(Level.FINE, "      municipality_osmrelationid  ==="+municipality_osmrelationid+"===");
							if(articlelist_destination.equals("methodoutput")) {
								logger.log(Level.FINE, "found streetlist will be send back to method-caller ...");
								this.municipality_country = municipality_country;
								this.municipality_name = municipality_name;
								this.municipality_administrationid = municipality_gemeindeschluessel;
								this.municipality_osmrelationid = municipality_osmrelationid;
								this.strassen = strassen;
								this.streetsuburbs = streetsuburbs;
								this.strassenanzahl = strassenanzahl;
								this.source_url = active_url;
								this.password_status = "nein";
								this.source_text = "";
								this.source_deliverydate = "";
								this.source_source = "";
								this.source_filedate = timestampliste[titelindex];
//ToDo don't stop so bad in mode methodoutput, better to end after loop
									// read remain file content
								if(inputline != null) {
									while ((inputline = dis.readLine()) != null) {
										complete_wikipage.append(inputline + "\n");
									}
								}
								dis.close();
								isir.close();
								if(urlConn != null)
									urlConn.disconnect();
								if(urlConn_streetlists != null) 
									urlConn_streetlists.disconnect();
								return complete_wikipage.toString(); 
							} else {
								municipality_import.save(municipality_country, municipality_name, municipality_gemeindeschluessel, municipality_osmrelationid, strassen, streetsuburbs, strassenanzahl, active_url, "nein", "", "", "", timestampliste[titelindex]);
							}
							logger.log(Level.WARNING, "WARNING: alread read "+strassenanzahl+" Street lines, when found first time a heading-line ==="+inputline+"===");
							municipality_name = "";
							municipality_osmrelationid = "";
							strassenanzahl = 0;
				  			municipality_submunicipality = "";
				  			municipality_gemeindeschluessel = "";
							found_streets_in_loop = true;
						  	//actual_street = "";
						} else {
						  	// municipality_name is no longer in heading, but will be simply used from artical title
							//municipality_name = inputline;
							//System.out.println("Municipality brutto original ==="+municipality_name+"===");
							//municipality_name = municipality_name.substring(3);
							//if(municipality_name.indexOf(" ==") != -1) {
							//	municipality_name = municipality_name.substring(0,municipality_name.indexOf(" =="));
							//}
							//System.out.println("Municipality netto ==="+municipality_name+"===");
							// "== " und " ==" entfernen
							zustand = "listenanfang";
						}
					}

					if(inputline.indexOf("Land:") == 0) {
						inputline = inputline.replace("Land:","");
						inputline = inputline.trim();
						if( ! inputline.equals("")) {
							logger.log(Level.FINE, "Land ==="+inputline+"===");
							municipality_country = inputline;
						}
					}
					if(inputline.indexOf("OSM-Relation:") == 0) {
						inputline = inputline.replace("OSM-Relation:","");
						inputline = inputline.trim();
						logger.log(Level.FINE, "Relatons-ID ==="+inputline+"===");
						municipality_osmrelationid = inputline;
					}
					if(inputline.indexOf("Relation:") == 0) {
						inputline = replace(inputline, "Relation: ","");
						inputline = replace(inputline, "Relation:","");
						logger.log(Level.FINE, "Relatons-ID ==="+inputline+"===");
						municipality_osmrelationid = inputline;
					}
					if(	(inputline.indexOf("Regionalschlüssel:") == 0) ||
						(inputline.indexOf("Gemeindeschlüssel:") == 0)) {
						inputline = inputline.substring(inputline.indexOf(":")+1);
						inputline = inputline.trim();
						logger.log(Level.FINE, "Regionalschlüssel am Anfang ==="+inputline+"===");

						Matcher match = wildcard_pattern.matcher(inputline);
						StringBuffer sb = new StringBuffer();
						boolean match_find = match.find();
						while(match_find) {
							match.appendReplacement(sb,"");
							match_find = match.find();
						}
						match.appendTail(sb);
						if( ! inputline.equals(sb.toString())) {
							String local_messagetext = "Regionalschluessel has whitespaces and was normalised from origin ==="+inputline+"=== to ==="+sb.toString()+"===. Article-URL is ==="+active_url+"===";
							new LogMessage(LogMessage.CLASS_WARNING, "Streetlist_from_streetlistwiki", municipality_name, -1L, local_messagetext);
							inputline = sb.toString();
						}
						inputline = sb.toString();

						logger.log(Level.FINE, "Regionalschlüssel am Ende ==="+inputline+"===");
						municipality_gemeindeschluessel = inputline;
					}
					if(inputline.indexOf("Land = ") != -1) {
						inputline = inputline.substring(inputline.indexOf("=")+1);
						inputline = inputline.trim();
						if( ! inputline.equals("")) {
							logger.log(Level.FINE, "Land ==="+inputline+"===");
							municipality_country = inputline;
						}
					}
					if(	(inputline.indexOf("Regionalschlüssel = ") != -1) ||
						(inputline.indexOf("Gemeindeschlüssel = ") != -1)) {
						inputline = inputline.substring(inputline.indexOf("=")+2);
						inputline = inputline.trim();
						logger.log(Level.FINE, "Regionalschlüssel am Anfang ==="+inputline+"===");

						Matcher match = wildcard_pattern.matcher(inputline);
						StringBuffer sb = new StringBuffer();
						boolean match_find = match.find();
						while(match_find) {
							match.appendReplacement(sb,"");
							match_find = match.find();
						}
						match.appendTail(sb);
						if( ! inputline.equals(sb.toString())) {
							String local_messagetext = "Gemeindeschluessel has whitespaces and was normalised from origin ==="+inputline+"=== to ==="+sb.toString()+"===. Article-URL is ==="+active_url+"===";
							new LogMessage(LogMessage.CLASS_WARNING, "Streetlist_from_streetlistwiki", municipality_name, -1L, local_messagetext);
							inputline = sb.toString();
						}

						logger.log(Level.FINE, "Regionalschlüssel am Ende ==="+inputline+"===");
						municipality_gemeindeschluessel = inputline;
					}
					if(	(inputline.indexOf("OSM-Relation = ") != -1) ||
						(inputline.indexOf("OSM Relation = ") != -1)) {
						inputline = inputline.substring(inputline.indexOf("=")+2);
						inputline = inputline.trim();
						logger.log(Level.FINE, "OSM-Relation-ID ==="+inputline+"===");
					  	municipality_osmrelationid = inputline;
					}
					if(	(inputline.indexOf("List:") == 0) ||
							(inputline.indexOf("Liste:") == 0) ||
							(inputline.indexOf("= Liste =") != -1)) {
						logger.log(Level.FINE, "Listenstart gefunden ==="+inputline+"===");
						zustand = "listeaktiv";
					}
					if(	((inputline.indexOf("* ") == 0) || (inputline.indexOf("*\t") == 0)) && zustand.equals("listeaktiv")) {
						String local_street = inputline.substring(2);
						if(local_street.indexOf(";") != -1) {
							local_street = local_street.substring(0,local_street.indexOf(";"));
							logger.log(Level.FINE, "local_street reduziert um gefundenen Kommentar, dann ==="+local_street+"===");
						}
						while((local_street.length() >= 1) && (local_street.substring(local_street.length() - 1).equals(" "))) {
							local_street = local_street.substring(0,local_street.length() - 1);
							logger.log(Level.FINE, "local_street nach leerzeichen am Ende entfernt ==="+local_street+"===");
						}

						municipality_submunicipality = "";
						int klammer_ab = local_street.indexOf("(");
						int klammer_bis = local_street.indexOf(")");
						if((klammer_ab != -1) && (klammer_bis != -1)) {
							municipality_submunicipality = local_street.substring(klammer_ab + 1, klammer_bis);
							logger.log(Level.FINE, "gefundener Ortsteil ==="+municipality_submunicipality+"===");

							local_street = local_street.substring(0,klammer_ab);
							while((local_street.length() >= 1) && (local_street.substring(local_street.length() - 1).equals(" "))) {
								local_street = local_street.substring(0,local_street.length() - 1);
								logger.log(Level.FINE, "local_street reduziert und nach leerzeichen am Ende entfernt ==="+local_street+"===");
							}
							logger.log(Level.FINE, "local_street jetzt ohne Ortszusatz ==="+local_street+"===");

							if(municipality_submunicipality.substring(0,1).equals("\""))
								municipality_submunicipality = municipality_submunicipality.substring(1);
							if(municipality_submunicipality.substring(municipality_submunicipality.length()-1,municipality_submunicipality.length()).equals("\""))
								municipality_submunicipality = municipality_submunicipality.substring(0,municipality_submunicipality.length()-1);
							while((municipality_submunicipality.length() > 0) && (municipality_submunicipality.substring(0,1).equals(" ")))
								municipality_submunicipality = municipality_submunicipality.substring(1);
							while((municipality_submunicipality.length() > 0) && (municipality_submunicipality.substring(municipality_submunicipality.length()-1,municipality_submunicipality.length()).equals(" ")))
								municipality_submunicipality = municipality_submunicipality.substring(0,municipality_submunicipality.length()-1);
							logger.log(Level.FINE, "Municipality submunicipality netto ==="+municipality_submunicipality+"===");
		
							if(municipality_submunicipality.equals(municipality_name))
								municipality_submunicipality = "";
							if(municipality_submunicipality.equals(municipality_name+", Stadt"))
								municipality_submunicipality = "";
							if(municipality_submunicipality.equals(municipality_name+", Gemeinde"))
								municipality_submunicipality = "";
						}
						if( ! municipality_submunicipality_permanent.equals(""))
							municipality_submunicipality = municipality_submunicipality_permanent;

						if(local_street.indexOf(", ") != -1) {
							String local_messagetext = "Street in Wiki article has text sequence ', ', seems to be a suburb-Information, but will not be supported, street originally ==="+inputline+"=== in Article-URL is ==="+active_url+"===";
							new LogMessage(LogMessage.CLASS_WARNING, "Streetlist_from_streetlistwiki", municipality_name, -1L, local_messagetext);
						}

						strassen[strassenanzahl] = local_street;
						streetsuburbs[strassenanzahl] = municipality_submunicipality;
						strassenanzahl++;
						logger.log(Level.FINE, "aktuelle Straße==="+local_street+"===  Ortszusatz ==="+municipality_submunicipality+"===");
					}
				} 
				dis.close();
				isir.close();

				logger.log(Level.INFO, "restliche Anzahl Straßen nach lesen aller Zeilen: "+strassenanzahl);

				if(strassenanzahl >= 1) {
					if(municipality_osmrelationid.equals("") && municipality_gemeindeschluessel.equals("")) {
						String local_messagetext = "Wiki article has neither Gemeindeschluessel nor OSM-Relation-ID. Article-URL is ==="+active_url+"===";
						new LogMessage(LogMessage.CLASS_ERROR, "Streetlist_from_streetlistwiki", municipality_name, -1L, local_messagetext);
					}
					logger.log(Level.FINE, "jetzt am Ende Straßenanzahl >= 1, konkret: "+strassenanzahl);
					logger.log(Level.FINE, "                municipality_country ==="+municipality_country+"===");
					logger.log(Level.FINE, "                municipality_name ==="+municipality_name+"===");
					logger.log(Level.FINE, "  municipality_gemeindeschluessel ==="+municipality_gemeindeschluessel+"===");
					logger.log(Level.FINE, "      municipality_osmrelationid  ==="+municipality_osmrelationid+"===");
					if((! municipality_gemeindeschluessel.equals("")) && (municipality_gemeindeschluessel.length() == 8)) {
							// check and add found gemeindeschluessel in Map. If already existing, report Error in Wiki
						if(articles_gemeindeschluessel.get(municipality_gemeindeschluessel) == null) {
							articles_gemeindeschluessel.put(municipality_gemeindeschluessel, municipality_name);
						} else {
							String local_messagetext = "identical regionalschluessel ==="+municipality_gemeindeschluessel+"=== at more than one artical, acutal article-titel ==="+municipality_name+"===, already found previously ==="+articles_gemeindeschluessel.get(municipality_gemeindeschluessel)+"===";
							System.out.println(local_messagetext);
							new LogMessage(LogMessage.CLASS_ERROR, "Streetlist_from_streetlistwiki", municipality_name, -1L, local_messagetext);									
							articles_gemeindeschluessel.put(municipality_gemeindeschluessel, articles_gemeindeschluessel.get(municipality_gemeindeschluessel)+"|"+municipality_name);
						}

						logger.log(Level.FINE, "checking muni ags ==="+municipality_gemeindeschluessel+"=== ...");
						String local_municipalitynameunique = new GermanyOfficialkeys().get_unique_municipalityname(municipality_gemeindeschluessel);
						if( !local_municipalitynameunique.equals(municipality_name)) {
							System.out.println("checked-fail muni ags ==="+municipality_gemeindeschluessel+"===");
							String local_messagetext = "Wiki article has wrong title ==="+municipality_name+"=== instead of database version ==="+local_municipalitynameunique+"===. Article-URL is ==="+active_url+"===";
							logger.log(Level.WARNING, local_messagetext);
							new LogMessage(LogMessage.CLASS_ERROR, "Streetlist_from_streetlistwiki", municipality_name, -1L, local_messagetext);
						} else {
							logger.log(Level.FINE, "checked-ok muni ags ==="+municipality_gemeindeschluessel+"===");
						}
					} else {
						logger.log(Level.FINE, "NOT checked muni ags ==="+municipality_gemeindeschluessel+"===");
					}
					if(articlelist_destination.equals("methodoutput")) {
						logger.log(Level.FINE, "found streetlist will be send back to method-caller ...");
						this.municipality_country = municipality_country;
						this.municipality_name = municipality_name;
						this.municipality_administrationid = municipality_gemeindeschluessel;
						this.municipality_osmrelationid = municipality_osmrelationid;
						this.strassen = strassen;
						this.streetsuburbs = streetsuburbs;
						this.strassenanzahl = strassenanzahl;
						this.source_url = active_url;
						this.password_status = "nein";
						this.source_text = "";
						this.source_deliverydate = "";
						this.source_source = "";
						this.source_filedate = timestampliste[titelindex];
//ToDo don't stop so bad in mode methodoutput, better to end after loop
							// read remain file content
						if(inputline != null) {
							while ((inputline = dis.readLine()) != null) {
								complete_wikipage.append(inputline + "\n");
							}
						}
						dis.close();
						isir.close();
						if(urlConn != null)
							urlConn.disconnect();
						if(urlConn_streetlists != null) 
							urlConn_streetlists.disconnect();
						return complete_wikipage.toString();
					} else {
						municipality_import.save(municipality_country, municipality_name, municipality_gemeindeschluessel, municipality_osmrelationid, strassen, streetsuburbs, strassenanzahl, active_url, "nein", "", "", "", timestampliste[titelindex]);
					}
		 		}

				if( ! kategorie_vorhanden) {
					String local_messagetext = "Wiki article has not a streetlist-category entry. Article-URL is ==="+active_url+"===";
					new LogMessage(LogMessage.CLASS_WARNING, "Streetlist_from_streetlistwiki", municipality_name, -1L, local_messagetext);
				} else {
					if((strassenanzahl == 0) && ( ! found_streets_in_loop)) {
						String local_messagetext = "Wiki article has no streets in article. Article-URL is ==="+active_url+"===";
						new LogMessage(LogMessage.CLASS_WARNING, "Streetlist_from_streetlistwiki", municipality_name, -1L, local_messagetext);
					}
				}

				if((strassenanzahl > 0) && ( found_streets_in_loop)) {
					String local_messagetext = "Wiki article has more than one list of streets, please check import. Article-URL is ==="+active_url+"===";
					new LogMessage(LogMessage.CLASS_WARNING, "Streetlist_from_streetlistwiki", municipality_name, -1L, local_messagetext);
				}

				
				} catch (FileNotFoundException fileerror) {
					logger.log(Level.SEVERE, "Wiki-Page not found, ignoring url ==="+active_url+"=== details follows ...");
					logger.log(Level.SEVERE, fileerror.toString());
					continue;
				}
				catch (IOException ioe) {
					logger.log(Level.SEVERE, "ERROR: fehler aufgetreten beim holen eines Dokuments über http");
					logger.log(Level.SEVERE, ioe.toString());
					System.out.println("ERROR: fehler aufgetreten beim holen eines Dokuments über http");
					ioe.printStackTrace();
					continue;
				} 
				
			} // end of loop over all wiki street pages

			java.util.Date time_program_endedtime = new java.util.Date();
			logger.log(Level.INFO, "Program: Ended Time: "+time_formatter.format(time_program_endedtime));
			logger.log(Level.INFO, "Program: Duration in ms: "+(time_program_endedtime.getTime()-time_program_startedtime.getTime())/1000);
			System.out.println("Program: Ended Time: "+time_formatter.format(time_program_endedtime));
			System.out.println("Program: Duration in ms: "+(time_program_endedtime.getTime()-time_program_startedtime.getTime())/1000);

		} catch (MalformedURLException mue) {
			logger.log(Level.SEVERE, "Error due to getting recentchanges api-request (malformedurlexception). url was ==="+act_url_string+"===");
			logger.log(Level.SEVERE, "Details: " + mue.toString());
			System.out.println("Error due to getting recentchanges api-request (malformedurlexception). url was ==="+act_url_string+"===");					
			System.out.println("Details: " + mue.toString());
			mue.printStackTrace();
			return "";
		} catch (IOException ioe) {
			logger.log(Level.SEVERE, "Error due to getting recentchanges api-request (ioexception). url was ==="+act_url_string+"===");
			logger.log(Level.SEVERE, "Details: " + ioe.toString());
			System.out.println("Error due to getting recentchanges api-request (ioexception). url was ==="+act_url_string+"===");
			System.out.println("Details: " + ioe.toString());
			ioe.printStackTrace();
			return "";
		}
		return "";
	}
}
