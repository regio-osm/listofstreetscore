package de.regioosm.listofstreetscore.util;
/*
testcode, um die Properties einer Seite zu holen. Aktueller Anlass am 14.04.2013 ist das löschen der BaWue-Listen, die vom 
	vom User ImporterVorhandeneListen
	am 2013-04-09T*
	importiert wurden mit einem von mehreren Kommentaren wie z.b.
	1 Version: Initialer Import der Baden-Württemberg Landesamt für Geoinformation und Landesentwicklung (LGL) restliche Listen aus LiKa-DB mit Stand 18.07.2011 2v4
	1 Version: Initialer Import der zentralen Straßenliste Baden-Württemberg vom Landesamt für Geoinformation und Landesentwicklung (LGL) vom 04.04.2013, Teil 1 von 3

	http://regio-osm.de/listofstreets_wiki/api.php?action=query&titles=Karlsruhe|Stuttgart|Ulm|Augsburg&format=xml&prop=revisions&rvprop=user|comment|ids|timestamp


	Dietmar Seifert, 12.08.2012
		Benutzung externer Programmcode wg. Multipart-Form Code, von http://www.devx.com/assets/sourcecode/7315.zip

	Dietmar Seifert, 11.08.2012
		Quelle: dokuwiki_generatepagesfrom_owlarchiv_gemeindestrassen.java, Dateidatum 24.11.2011 22:19:38

*/

import java.io.*;
import java.net.*;
import java.sql.*;
import java.text.*;
import java.util.*;

import com.myjavatools.web.ClientHttpRequest;

import de.regioosm.listofstreetscore.util.Mediawiki_Structure;
import de.regioosm.listofstreetscore.util.Applicationconfiguration;


public class Mediawiki_API {


	static Connection con_listofstreets = null;
	static Connection con_mapnik = null;
	static Applicationconfiguration configuration = new Applicationconfiguration();

	public String wiki_url = "";
	public String wiki_username = "";
	public String wiki_password = "";
	public String[] cookies = new String[20];
	public Integer cookies_anzahl = 0;
	public String login_token = "";
	public Integer maxlag = 0;				// mediawiki maxlag see http://www.mediawiki.org/wiki/Manual:Maxlag_parameter

	public static String EDIT_ACTION = "edit";

	TreeMap<String, String> wiki_namespaces = new TreeMap<String, String>();

	
	public Mediawiki_API() {
		System.out.println("in Mediawiki_API constructor ...");
		wiki_namespaces.put("Bundesrepublik Deutschland", "");
		wiki_namespaces.put("Brasil", "BR");
		wiki_namespaces.put("Ísland", "IS");
		wiki_namespaces.put("Österreich", "AT");
		wiki_namespaces.put("România", "RO");
		wiki_namespaces.put("Russia", "RU");
	}
	public boolean login(String wiki_url, String wiki_username, String wiki_password) {
		String upload_string = "";

		this.wiki_url = wiki_url;
		this.wiki_username = wiki_username;
		this.wiki_password = wiki_password;
		this.login_token = "";

		try {

			// 1. LOGIN, ohne Token

																// api.php?action=login&lgname=Bob&lgpassword=secret
			URL api_url = new URL(wiki_url + "/api.php?action=login&format=xml");
			System.out.println("");
			System.out.println("*** 1. Login, ohne Token, mit Url: "+api_url);
	
			HttpURLConnection connection = (HttpURLConnection) api_url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("Content-Language", "en-US");
			upload_string = URLEncoder.encode("lgname","UTF-8") + "=" + URLEncoder.encode(wiki_username,"UTF-8");
			upload_string += "&" + URLEncoder.encode("lgpassword","UTF-8") + "=" + URLEncoder.encode(wiki_password,"UTF-8");
	
			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			System.out.println("upload_string==="+upload_string+"===");
			writer.write(upload_string);
			writer.flush();
	
	
			Integer headeri = 1;
			System.out.println("HTTP-Header Fields-Ausgabe ...");
			while(connection.getHeaderFieldKey(headeri) != null) {
				if(connection.getHeaderFieldKey(headeri).equals("Set-Cookie")) {
					this.cookies_anzahl++;
					this.cookies[this.cookies_anzahl] = connection.getHeaderField(headeri);
					System.out.println("Cookie-Zeile gesichert ==="+this.cookies[this.cookies_anzahl]+"===");
				}
				System.out.println("  Header # "+headeri+":  ["+connection.getHeaderFieldKey(headeri)+"] ==="+connection.getHeaderField(headeri)+"===");
				headeri++;
			}
			System.out.println("cookies_string nach Ausgabe Headerfields ...");
			for(Integer cookiei=1;cookiei <= this.cookies_anzahl; cookiei++) {
				System.out.println("gespeicherter Cookie-String ["+cookiei+"] ==="+this.cookies[cookiei]+"===");
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			for(String line; (line = reader.readLine()) != null;) {
				System.out.println(line);
				if(line.indexOf("token=") != -1) {
					Integer startpos = line.indexOf("\"",line.indexOf("token=")+1) + 1;
					Integer endpos = line.indexOf("\"",startpos+1);
					this.login_token = line.substring(startpos,endpos);
				}
			}
			System.out.println("gefundener LOGIN-Token ===" + this.login_token + "===");
			writer.close();
			reader.close();
			connection.disconnect();
	
	
				// 2. LOGIN, zur Token-Bestätigung vom 1. Login
	
			api_url = new URL(wiki_url + "/api.php?action=login&format=xml");
			System.out.println("");
			System.out.println("*** 2. Login, mit Token, mit Url: "+api_url);
	
			connection = (HttpURLConnection) api_url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("Content-Language", "en-US");
			String list_cookies = "";
			for(Integer cookiei=1;cookiei <= this.cookies_anzahl; cookiei++) {
				if( ! list_cookies.equals(""))
					list_cookies += ",";
				list_cookies += this.cookies[cookiei];
			}
			if( ! list_cookies.equals("")) {
				connection.setRequestProperty("Cookie", list_cookies);
				System.out.println("Add Cookie-Strings to Header ==="+list_cookies+"===");
			}
	
			upload_string = URLEncoder.encode("lgname","UTF-8") + "=" + URLEncoder.encode(wiki_username,"UTF-8");
			upload_string += "&" + URLEncoder.encode("lgpassword","UTF-8") + "=" + URLEncoder.encode(wiki_password,"UTF-8");
			upload_string += "&" + URLEncoder.encode("lgtoken","UTF-8") + "=" + URLEncoder.encode(this.login_token,"UTF-8");
	
			writer = new OutputStreamWriter(connection.getOutputStream());
			System.out.println("upload_string==="+upload_string+"===");
			writer.write(upload_string);
			writer.flush();
	
			//cookies_anzahl = 0;
	
			headeri = 1;
			System.out.println("Header-Fields Ausgabe ...");
			while(connection.getHeaderFieldKey(headeri) != null) {
				if(connection.getHeaderFieldKey(headeri).equals("Set-Cookie")) {
					this.cookies_anzahl++;
					this.cookies[this.cookies_anzahl] = connection.getHeaderField(headeri);
					System.out.println("Cookie-Zeile gesichert ==="+this.cookies[this.cookies_anzahl]+"===");
				}
				System.out.println("  Header # "+headeri+":  ["+connection.getHeaderFieldKey(headeri)+"] ==="+connection.getHeaderField(headeri)+"===");
				headeri++;
			}
			System.out.println("cookies_string nach Ausgabe Headerfields ...");
			for(Integer cookiei=1;cookiei <= this.cookies_anzahl; cookiei++) {
				System.out.println("gespeicherter Cookie-String ["+cookiei+"] ==="+this.cookies[cookiei]+"===");
			}
	
			this.login_token = "";
	
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String result = "";
			for(String line; (line = reader.readLine()) != null;) {
				System.out.println(line);
				if(line.indexOf("lgtoken=") != -1) {
					Integer startpos = line.indexOf("\"",line.indexOf("lgtoken=")+1) + 1;
					Integer endpos = line.indexOf("\"",startpos+1);
					this.login_token = line.substring(startpos,endpos);
				}
				if(line.indexOf("result=") != -1) {
					Integer startpos = line.indexOf("\"",line.indexOf("result=")+1) + 1;
					Integer endpos = line.indexOf("\"",startpos+1);
					result = line.substring(startpos,endpos);
				}
			}
			System.out.println("gefundenes RESULT ===" + result + "===");
			writer.close();
			reader.close();
			connection.disconnect();
	
			if( ! result.equals("Success")) {
				System.out.println("Anmeldung beim Server fehlgeschlagen, Grund: "+result);
				return false;
			}
		}
		catch (MalformedURLException murle) { 
			System.out.println("ERROR MalformedURLException, Details ...");
			murle.printStackTrace();
		}
		catch (ProtocolException pe) {
			System.out.println("ERROR: ProtocolException, Details ...");
			pe.printStackTrace();
		} catch (IOException ioe) {
			System.out.println("ERROR: IOException, Details ...");
			ioe.printStackTrace();
		}
		return true;
	}

	public HashMap<String, String> get_page_info(
			String country,
			String wiki_pagetitle) {

		String upload_string = "";
		HashMap<String, String> output_metadata = new HashMap<String,String>(); 

		
		try {

			if(!country.equals("Bundesrepublik Deutschland")) {
				if(wiki_namespaces.get(country) != null)
					wiki_pagetitle = wiki_namespaces.get(country) + ":" + wiki_pagetitle;
				else
					wiki_pagetitle = "NN" + ":" + wiki_pagetitle;
			}

			// Query prop=info zum holen Metadaten
	
			String url_string = this.wiki_url + "/api.php?action=query&format=xml";
			if(this.maxlag > 0)
				url_string += "&maxlag=" + this.maxlag;
			URL api_url = new URL(url_string);
			System.out.println("");
			System.out.println("*** Query prop=info ..., mit Url: "+api_url);
		
			HttpURLConnection connection = (HttpURLConnection) api_url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("Content-Language", "en-US");
			String list_cookies = "";
			for(Integer cookiei=1;cookiei <= this.cookies_anzahl; cookiei++) {
				if( ! list_cookies.equals(""))
					list_cookies += ",";
				list_cookies += this.cookies[cookiei];
			}
			if( ! list_cookies.equals("")) {
				connection.setRequestProperty("Cookie", list_cookies);
				System.out.println("Add Cookie-Strings to Header ==="+list_cookies+"===");
			}
		
		
			upload_string = URLEncoder.encode("prop","UTF-8") + "=" + URLEncoder.encode("info|revisions","UTF-8");		// revisions nötig, damit letzter Editor und Kommentar kommen
			//upload_string += "&" + URLEncoder.encode("intoken","UTF-8") + "=" + URLEncoder.encode(actiontype,"UTF-8");
			upload_string += "&" + URLEncoder.encode("titles","UTF-8") + "=" + URLEncoder.encode(wiki_pagetitle, "UTF-8");
			upload_string += "&" + URLEncoder.encode("token","UTF-8") + "=" + URLEncoder.encode(this.login_token,"UTF-8");
		
			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			System.out.println("upload_string==="+upload_string+"===");
			writer.write(upload_string);
			writer.flush();
		
			Integer headeri = 1;
			System.out.println("Header-Fields Ausgabe ...");
			while(connection.getHeaderFieldKey(headeri) != null) {
				if(connection.getHeaderFieldKey(headeri).equals("Set-Cookie")) {
					this.cookies_anzahl++;
					this.cookies[this.cookies_anzahl] = connection.getHeaderField(headeri);
					System.out.println("Cookie-Zeile gesichert ==="+this.cookies[this.cookies_anzahl]+"===");
				}
				System.out.println("  Header # "+headeri+":  ["+connection.getHeaderFieldKey(headeri)+"] ==="+connection.getHeaderField(headeri)+"===");
				headeri++;
			}
			System.out.println("cookies_string nach Ausgabe Headerfields ...");
			for(Integer cookiei=1;cookiei <= this.cookies_anzahl; cookiei++) {
				System.out.println("gespeicherter Cookie-String ["+cookiei+"] ==="+this.cookies[cookiei]+"===");
			}
		
		
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuffer xml_content = new StringBuffer();
			String fileline = "";
			while((fileline = reader.readLine()) != null) {
				System.out.println(fileline);
				xml_content.append(fileline + "\n");
			}
			writer.close();
			reader.close();
			connection.disconnect();
	
			Mediawiki_Structure xml_content_structure = new Mediawiki_Structure();
			HashMap<String, String> page_keyvalues = xml_content_structure.attributes(xml_content.toString(),"page"); 
			HashMap<String, String> revisions_keyvalues = xml_content_structure.attributes(xml_content.toString(),"rev");
			output_metadata.putAll(page_keyvalues);
			output_metadata.putAll(revisions_keyvalues);
		}
		catch (MalformedURLException mue) {
			System.out.println("ERROR: MalformedURLException, Details ...");
			mue.printStackTrace();
			return null;
		} 
		catch (IOException ioe) {
			System.out.println("ERROR: IOException, Details ...");
			ioe.printStackTrace();
			return null;
		}
		
		
		return output_metadata;
	}

	public boolean action(String actiontype, String country, String wiki_pagetitle, String wiki_pagecontent, String wiki_summary) {
		String upload_string = "";
		
		try {
				// Query prop=info zum holen action-Token

			if(!country.equals("Bundesrepublik Deutschland")) {
				if(wiki_namespaces.get(country) != null)
					wiki_pagetitle = wiki_namespaces.get(country) + ":" + wiki_pagetitle;
				else
					wiki_pagetitle = "NN" + ":" + wiki_pagetitle;
			}

			String url_string = this.wiki_url + "/api.php?action=query&format=xml";
			if(this.maxlag > 0)
				url_string += "&maxlag=" + this.maxlag;
			URL api_url = new URL(url_string);
			System.out.println("");
			System.out.println("*** 3. Query prop=info ..., mit Url: "+api_url);
		
			HttpURLConnection connection = (HttpURLConnection) api_url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("Content-Language", "en-US");
			String list_cookies = "";
			for(Integer cookiei=1;cookiei <= this.cookies_anzahl; cookiei++) {
				if( ! list_cookies.equals(""))
					list_cookies += ",";
				list_cookies += this.cookies[cookiei];
			}
			if( ! list_cookies.equals("")) {
				connection.setRequestProperty("Cookie", list_cookies);
				System.out.println("Add Cookie-Strings to Header ==="+list_cookies+"===");
			}
		
		
			upload_string = URLEncoder.encode("prop","UTF-8") + "=" + URLEncoder.encode("info|revisions","UTF-8");		// revisions nötig, damit letzter Editor und Kommentar kommen
			upload_string += "&" + URLEncoder.encode("intoken","UTF-8") + "=" + URLEncoder.encode(actiontype,"UTF-8");
			upload_string += "&" + URLEncoder.encode("titles","UTF-8") + "=" + URLEncoder.encode(wiki_pagetitle, "UTF-8");
			upload_string += "&" + URLEncoder.encode("token","UTF-8") + "=" + URLEncoder.encode(this.login_token,"UTF-8");
		
			OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
			System.out.println("upload_string==="+upload_string+"===");
			writer.write(upload_string);
			writer.flush();
		
			Integer headeri = 1;
			System.out.println("Header-Fields Ausgabe ...");
			while(connection.getHeaderFieldKey(headeri) != null) {
				if(connection.getHeaderFieldKey(headeri).equals("Set-Cookie")) {
					this.cookies_anzahl++;
					this.cookies[this.cookies_anzahl] = connection.getHeaderField(headeri);
					System.out.println("Cookie-Zeile gesichert ==="+this.cookies[this.cookies_anzahl]+"===");
				}
				System.out.println("  Header # "+headeri+":  ["+connection.getHeaderFieldKey(headeri)+"] ==="+connection.getHeaderField(headeri)+"===");
				headeri++;
			}
			System.out.println("cookies_string nach Ausgabe Headerfields ...");
			for(Integer cookiei=1;cookiei <= this.cookies_anzahl; cookiei++) {
				System.out.println("gespeicherter Cookie-String ["+cookiei+"] ==="+this.cookies[cookiei]+"===");
			}
		
			String actiontoken = "";
			String page_lastchanged = "";
			String page_lastuser = "";
			String page_comment = "";
		
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			for(String line; (line = reader.readLine()) != null;) {
				System.out.println(line);
				if(line.indexOf(actiontype + "token=") != -1) {
					Integer startpos = line.indexOf("\"",line.indexOf(actiontype + "token=")+1) + 1;
					Integer endpos = line.indexOf("\"",startpos);
					actiontoken = line.substring(startpos,endpos);
				}
				if(line.indexOf("timestamp=") != -1) {
					Integer startpos = line.indexOf("\"",line.indexOf("timestamp=")+1) + 1;
					Integer endpos = line.indexOf("\"",startpos);
					page_lastchanged = line.substring(startpos,endpos);
				}
				if(line.indexOf("user=") != -1) {
					Integer startpos = line.indexOf("\"",line.indexOf("user=")+1) + 1;
					Integer endpos = line.indexOf("\"",startpos);
					page_lastuser = line.substring(startpos,endpos);
				}
if(line.indexOf("comment=\"\"") != -1)
	System.out.println("leer kommentar");

				if(line.indexOf("comment=") != -1) {
					Integer startpos = line.indexOf("\"",line.indexOf("comment=")+1) + 1;
					Integer endpos = line.indexOf("\"",startpos);
					page_comment = "";
					if(endpos >= 0)
						page_comment = line.substring(startpos,endpos);
				}
			}
			System.out.println("gefundener ACTION-Token ===" + actiontoken + "===");
			writer.close();
			reader.close();
			connection.disconnect();

			if(actiontype.equals(EDIT_ACTION)) {
					// edit action wiki-Content über Post

				//geht if(! page_lastuser.equals("ImporterVorhandeneListen")) {
				//	System.out.println("WARNUNG WARNUNG: Abbruch action="+actiontype+" , weil letzter User nicht ===ImporterVorhandeneListen===");
				//	return false;
				//}

				//if( ! page_comment.equals("")) {
				//	
				//}
				
					// http://www.mediawiki.org/wiki/API:Edit

				url_string = this.wiki_url + "/api.php?action="+actiontype+"&format=xml";
				if(this.maxlag > 0)
					url_string += "&maxlag=" + this.maxlag;
				api_url = new URL(url_string);
				System.out.println("");
				System.out.println("*** 4. Import XML-Content, mit Url: "+api_url);
	
				HttpURLConnection rconnection = (HttpURLConnection) api_url.openConnection();
				rconnection.setRequestMethod("POST");
				rconnection.setDoInput(true);
				rconnection.setDoOutput(true);
				rconnection.setUseCaches(false);
				rconnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				rconnection.setRequestProperty("Content-Language", "en-US");
	
				try {
					System.out.println(" 4711-1 Header Content-Type vor  setzen ==="+rconnection.getRequestProperty("Content-Type")+"===");
					rconnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + "---------------------------1byy9zky6a9zbswv6roes1cpt-1eru9dxnn0gvf");
					System.out.println("4711-1 set content-type");
					System.out.println(" 4711-1 Header Content-Type nach setzen ==="+rconnection.getRequestProperty("Content-Type")+"===");
				}
				catch (IllegalStateException iex) {
					System.out.println("4711-1: Exception IllegalStateException, also schon  connected");
					iex.printStackTrace();
				}
					
	
				list_cookies = "";
				for(Integer cookiei=1;cookiei <= this.cookies_anzahl; cookiei++) {
					if( ! list_cookies.equals(""))
						list_cookies += ",";
					list_cookies += this.cookies[cookiei];
				}
				if( list_cookies.equals("")) {
					System.out.println("FEHLER: keine Cookies verfügbar, ABBRUCH der action");
					return false;
				}
	
	
	
				try {
					rconnection.setRequestProperty("Cookie", list_cookies);
					System.out.println("4711-2 Add Cookie-Strings to Header ==="+list_cookies+"===");
				}
				catch (IllegalStateException iex) {
					System.out.println("4711-2: Exception IllegalStateException, also schon  connected");
					iex.printStackTrace();
				}
	
				ClientHttpRequest postr = new ClientHttpRequest(rconnection);
				
				postr.setParameter("basetimestamp", page_lastchanged);		// Zeitstempel aktuelle Version der Datei, um Bearbeitungskonflitk zu vermeiden
				postr.setParameter("summary", wiki_summary);				// Kommentar
				postr.setParameter("text", wiki_pagecontent);
				postr.setParameter("title", wiki_pagetitle);
				postr.setParameter("token", actiontoken);					// token must be LAST Parameter
	
				System.out.println("BEGINN Header-Fields ClientHttpRequest.connection Ausgabe nach Versendung...");
				headeri = 1;
				//while(postr.connection.getHeaderFieldKey(headeri) != null) {
				//System.out.println("  Header # "+headeri+":  ["+postr.connection.getHeaderFieldKey(headeri)+"] ==="+postr.connection.getHeaderField(headeri)+"===");
				//headeri++;
				//}
				System.out.println("ENDE Header-Fields ClientHttpRequest.connection Ausgabe nach Versendung...");
	
				System.out.println("BEGINN Header-Fields connection Ausgabe nach Versendung...");
				headeri = 1;
				while(rconnection.getHeaderFieldKey(headeri) != null) {
					System.out.println("  Header # "+headeri+":  ["+rconnection.getHeaderFieldKey(headeri)+"] ==="+rconnection.getHeaderField(headeri)+"===");
					headeri++;
				}
				System.out.println("ENDE Header-Fields connection Ausgabe nach Versendung...");
	
				reader = new BufferedReader(new InputStreamReader(postr.post()));
	
	
	
				for(String line; (line = reader.readLine()) != null;) {
						// geändert gespeichert: <edit result="Success" pageid="10020" title="Testgemeinde" oldrevid="27854" newrevid="27856" newtimestamp="2013-11-23T22:56:48Z" />
						// keine inhaltliche änderung nochange=""
					System.out.println(line);
				}
				writer.close();
				reader.close();
				rconnection.disconnect();
				return true;
			} else {
				System.out.println("FEHLER actiontype ==="+actiontype+"=== ist noch nicht programmiert");
				return false;
			}
	
		}
		catch (MalformedURLException mue) {
			System.out.println("ERROR: MalformedURLException, Details ...");
			mue.printStackTrace();
			return false;
		} 
		catch (IOException ioe) {
			System.out.println("ERROR: IOException, Details ...");
			ioe.printStackTrace();
			return false;
		}
	}


	
	
	public boolean ___INAKTIV_NICHT_PRODUKTIV_ZUSTAND_UNBEKANNT___Udelete_municipality(String municipalityname) {

		Boolean output_change_state = false;


		java.util.Date time_program_startedtime = new java.util.Date();
		DateFormat time_formatter = new SimpleDateFormat();
		System.out.println("Program: Started Time: "+time_formatter.format(time_program_startedtime));

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

			try
			{
					String upload_string = "";

						// 1. LOGIN, ohne Token

				String wiki_username = configuration.streetlistwiki_importeruseraccount; //"WikiBot";
				String wiki_password = configuration.streetlistwiki_importeruserpassword;

				URL api_url = new URL("http://regio-osm.de/listofstreets_wiki/api.php?action=login&format=xml");
				System.out.println("");
				System.out.println("*** 1. Login, ohne Token, mit Url: "+api_url);

				HttpURLConnection connection = (HttpURLConnection) api_url.openConnection();
				connection.setRequestMethod("POST");
				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setUseCaches(false);
				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				connection.setRequestProperty("Content-Language", "en-US");
				upload_string = URLEncoder.encode("lgname","UTF-8") + "=" + URLEncoder.encode(wiki_username,"UTF-8");
				upload_string += "&" + URLEncoder.encode("lgpassword","UTF-8") + "=" + URLEncoder.encode(wiki_password,"UTF-8");

				OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
				System.out.println("upload_string==="+upload_string+"===");
				writer.write(upload_string);
				writer.flush();

				String[] cookies_string = new String[20];
				Integer cookies_anzahl = 0;

				Integer headeri = 1;
				System.out.println("Header-Fields Ausgabe ...");
				while(connection.getHeaderFieldKey(headeri) != null) {
					if(connection.getHeaderFieldKey(headeri).equals("Set-Cookie")) {
						cookies_anzahl++;
						cookies_string[cookies_anzahl] = connection.getHeaderField(headeri);
						System.out.println("Cookie-Zeile gesichert ==="+cookies_string[cookies_anzahl]+"===");
					}
					System.out.println("  Header # "+headeri+":  ["+connection.getHeaderFieldKey(headeri)+"] ==="+connection.getHeaderField(headeri)+"===");
					headeri++;
				}
				System.out.println("cookies_string nach Ausgabe Headerfields ...");
				for(Integer cookiei=1;cookiei <= cookies_anzahl; cookiei++) {
					System.out.println("gespeicherter Cookie-String ["+cookiei+"] ==="+cookies_string[cookiei]+"===");
				}

				String logintoken = "";

				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				for(String line; (line = reader.readLine()) != null;) {
					System.out.println(line);
					if(line.indexOf("token=") != -1) {
						Integer startpos = line.indexOf("\"",line.indexOf("token=")+1) + 1;
						Integer endpos = line.indexOf("\"",startpos+1);
						logintoken = line.substring(startpos,endpos);
					}
				}
				System.out.println("gefundener LOGIN-Token ===" + logintoken + "===");
				writer.close();
				reader.close();


					// 2. LOGIN, zur Token-Bestätigung vom 1. Login

				api_url = new URL("http://regio-osm.de/listofstreets_wiki/api.php?action=login&format=xml");
				System.out.println("");
				System.out.println("*** 2. Login, mit Token, mit Url: "+api_url);

				connection = (HttpURLConnection) api_url.openConnection();
				connection.setRequestMethod("POST");
				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setUseCaches(false);
				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				connection.setRequestProperty("Content-Language", "en-US");
				String list_cookies = "";
				for(Integer cookiei=1;cookiei <= cookies_anzahl; cookiei++) {
					if( ! list_cookies.equals(""))
						list_cookies += ",";
					list_cookies += cookies_string[cookiei];
				}
				if( ! list_cookies.equals("")) {
					connection.setRequestProperty("Cookie", list_cookies);
					System.out.println("Add Cookie-Strings to Header ==="+list_cookies+"===");
				}
    
				upload_string = URLEncoder.encode("lgname","UTF-8") + "=" + URLEncoder.encode(wiki_username,"UTF-8");
				upload_string += "&" + URLEncoder.encode("lgpassword","UTF-8") + "=" + URLEncoder.encode(wiki_password,"UTF-8");
				upload_string += "&" + URLEncoder.encode("lgtoken","UTF-8") + "=" + URLEncoder.encode(logintoken,"UTF-8");

				writer = new OutputStreamWriter(connection.getOutputStream());
				System.out.println("upload_string==="+upload_string+"===");
				writer.write(upload_string);
				writer.flush();

				//cookies_anzahl = 0;

				headeri = 1;
				System.out.println("Header-Fields Ausgabe ...");
				while(connection.getHeaderFieldKey(headeri) != null) {
					if(connection.getHeaderFieldKey(headeri).equals("Set-Cookie")) {
						cookies_anzahl++;
						cookies_string[cookies_anzahl] = connection.getHeaderField(headeri);
						System.out.println("Cookie-Zeile gesichert ==="+cookies_string[cookies_anzahl]+"===");
					}
					System.out.println("  Header # "+headeri+":  ["+connection.getHeaderFieldKey(headeri)+"] ==="+connection.getHeaderField(headeri)+"===");
					headeri++;
				}
				System.out.println("cookies_string nach Ausgabe Headerfields ...");
				for(Integer cookiei=1;cookiei <= cookies_anzahl; cookiei++) {
					System.out.println("gespeicherter Cookie-String ["+cookiei+"] ==="+cookies_string[cookiei]+"===");
				}

				logintoken = "";

				reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String result = "";
				for(String line; (line = reader.readLine()) != null;) {
					System.out.println(line);
					if(line.indexOf("lgtoken=") != -1) {
						Integer startpos = line.indexOf("\"",line.indexOf("lgtoken=")+1) + 1;
						Integer endpos = line.indexOf("\"",startpos+1);
						logintoken = line.substring(startpos,endpos);
					}
					if(line.indexOf("result=") != -1) {
						Integer startpos = line.indexOf("\"",line.indexOf("result=")+1) + 1;
						Integer endpos = line.indexOf("\"",startpos+1);
						result = line.substring(startpos,endpos);
					}
				}
				System.out.println("gefundenes RESULT ===" + result + "===");
				writer.close();
				reader.close();

				if( ! result.equals("Success")) {
					System.out.println("Anmeldung beim Server fehlgeschlagen, Grund: "+result);
					return output_change_state;
				}


					// 3. Query query article zum ermitteln der korrekten Property-Einstellungen und im positiven Fall zum holen action-Token

				String api_url_string = "http://regio-osm.de/listofstreets_wiki/api.php?action=query";
				api_url_string += "&titles=" + URLEncoder.encode(municipalityname,"UTF-8");
				api_url_string += "&format=xml&prop=revisions";
				api_url_string += "&rvprop=user|comment|ids|timestamp";

				api_url = new URL(api_url_string);
//				vom User ImporterVorhandeneListen
//				am 2013-04-09T*
//				importiert wurden mit einem von mehreren Kommentaren wie z.b.
//				1 Version: Initialer Import der Baden-Württemberg Landesamt für Geoinformation und Landesentwicklung (LGL) restliche Listen aus LiKa-DB mit Stand 18.07.2011 2v4
//				1 Version: Initialer Import der zentralen Straßenliste Baden-Württemberg vom Landesamt für Geoinformation und Landesentwicklung (LGL) vom 04.04.2013, Teil 1 von 3

				System.out.println("");
				System.out.println("*** 3. Query query to check correct properties ..., mit Url: "+api_url);

				connection = (HttpURLConnection) api_url.openConnection();
				connection.setRequestMethod("POST");
				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setUseCaches(false);
				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				connection.setRequestProperty("Content-Language", "en-US");
				list_cookies = "";
				for(Integer cookiei=1;cookiei <= cookies_anzahl; cookiei++) {
					if( ! list_cookies.equals(""))
						list_cookies += ",";
					list_cookies += cookies_string[cookiei];
				}
				if( ! list_cookies.equals("")) {
					connection.setRequestProperty("Cookie", list_cookies);
					System.out.println("Add Cookie-Strings to Header ==="+list_cookies+"===");
				}


				upload_string = URLEncoder.encode("prop","UTF-8") + "=" + URLEncoder.encode("info","UTF-8");
				upload_string += "&" + URLEncoder.encode("intoken","UTF-8") + "=" + URLEncoder.encode("delete","UTF-8");
				upload_string += "&" + URLEncoder.encode("titles","UTF-8") + "=" + URLEncoder.encode(municipalityname,"UTF-8");
				upload_string += "&" + URLEncoder.encode("token","UTF-8") + "=" + URLEncoder.encode(logintoken,"UTF-8");

				writer = new OutputStreamWriter(connection.getOutputStream());
				System.out.println("upload_string==="+upload_string+"===");
				writer.write(upload_string);
				writer.flush();

				headeri = 1;
				System.out.println("Header-Fields Ausgabe ...");
				while(connection.getHeaderFieldKey(headeri) != null) {
					if(connection.getHeaderFieldKey(headeri).equals("Set-Cookie")) {
						cookies_anzahl++;
						cookies_string[cookies_anzahl] = connection.getHeaderField(headeri);
						System.out.println("Cookie-Zeile gesichert ==="+cookies_string[cookies_anzahl]+"===");
					}
					System.out.println("  Header # "+headeri+":  ["+connection.getHeaderFieldKey(headeri)+"] ==="+connection.getHeaderField(headeri)+"===");
					headeri++;
				}
				System.out.println("cookies_string nach Ausgabe Headerfields ...");
				for(Integer cookiei=1;cookiei <= cookies_anzahl; cookiei++) {
					System.out.println("gespeicherter Cookie-String ["+cookiei+"] ==="+cookies_string[cookiei]+"===");
				}
			
				String actiontoken = "";

				reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				for(String line; (line = reader.readLine()) != null;) {
					System.out.println(line);
					if(line.indexOf("deletetoken=") != -1) {
						Integer startpos = line.indexOf("\"",line.indexOf("deletetoken=")+1) + 1;
						Integer endpos = line.indexOf("\"",startpos+1);
						actiontoken = line.substring(startpos,endpos);
					}
				}
				System.out.println("gefundener ACTION-Token ===" + actiontoken + "===");
				writer.close();
				reader.close();


					
					// 4. delete article

				api_url = new URL("http://regio-osm.de/listofstreets_wiki/api.php?action=delete&format=xml");
				System.out.println("");
				System.out.println("*** 4. Import XML-Content, mit Url: "+api_url);

				connection = (HttpURLConnection) api_url.openConnection();
				connection.setRequestMethod("POST");
				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setUseCaches(false);
				connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				connection.setRequestProperty("Content-Language", "en-US");
				list_cookies = "";
				for(Integer cookiei=1;cookiei <= cookies_anzahl; cookiei++) {
					if( ! list_cookies.equals(""))
						list_cookies += ",";
					list_cookies += cookies_string[cookiei];
				}
				if( ! list_cookies.equals("")) {
					connection.setRequestProperty("Cookie", list_cookies);
					System.out.println("Add Cookie-Strings to Header ==="+list_cookies+"===");
				}
				
				upload_string = URLEncoder.encode("title","UTF-8") + "=" + URLEncoder.encode(municipalityname,"UTF-8");
							// no section, so complete article upload_string += "&" + URLEncoder.encode("section","UTF-8") + "=" + URLEncoder.encode("1","UTF-8");
				upload_string += "&" + URLEncoder.encode("reason","UTF-8") + "=" + URLEncoder.encode("unvollständige Liste, wird neu importiert","UTF-8");
				upload_string += "&" + URLEncoder.encode("token","UTF-8") + "=" + URLEncoder.encode(actiontoken,"UTF-8");

				writer = new OutputStreamWriter(connection.getOutputStream());
				System.out.println("upload_string==="+upload_string+"===");
				writer.write(upload_string);
				writer.flush();

				headeri = 1;
				System.out.println("Header-Fields Ausgabe ...");
				while(connection.getHeaderFieldKey(headeri) != null) {
					if(connection.getHeaderFieldKey(headeri).equals("Set-Cookie")) {
						cookies_anzahl++;
						cookies_string[cookies_anzahl] = connection.getHeaderField(headeri);
						System.out.println("Cookie-Zeile gesichert ==="+cookies_string[cookies_anzahl]+"===");
					}
					System.out.println("  Header # "+headeri+":  ["+connection.getHeaderFieldKey(headeri)+"] ==="+connection.getHeaderField(headeri)+"===");
					headeri++;
				}
				System.out.println("cookies_string nach Ausgabe Headerfields ...");
				for(Integer cookiei=1;cookiei <= cookies_anzahl; cookiei++) {
					System.out.println("gespeicherter Cookie-String ["+cookiei+"] ==="+cookies_string[cookiei]+"===");
				}
				output_change_state = true;

				writer.close();
				reader.close();
			}
			catch (MalformedURLException mue) {
				mue.printStackTrace();
				System.exit(4711);
			} 
			catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(4712);
			}



			java.util.Date time_program_endedtime = new java.util.Date();
			System.out.println("Program: Ended Time: "+time_formatter.format(time_program_endedtime));
			System.out.println("Program: Duration in s: "+(time_program_endedtime.getTime()-time_program_startedtime.getTime())/1000);

		}
		catch( SQLException e) {
			e.printStackTrace();
			return output_change_state;
		}
		return output_change_state;
	}

}
