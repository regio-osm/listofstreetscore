package de.regioosm.listofstreetscore.util;
import java.io.FileReader;
import java.io.Reader;
import java.util.Properties;
import java.util.logging.Level;


public class Applicationconfiguration {

	public String servername = "";
	public String application_homedir = "";
	public String application_datadir = "";
	public String db_structure = "osm2pgsql";
	public String db_application_url = "";
	public String db_application_username = "";
	public String db_application_password = "";
	public String db_osm2pgsql_url = "";
	public String db_osm2pgsql_username = "";
	public String db_osm2pgsql_password = "";
	public String db_osm2pgsqlfullhistoryodbl_url = "";
	public String db_osm2pgsqlfullhistoryodbl_username = "";
	public String db_osm2pgsqlfullhistoryodbl_password = "";
	public String db_osm2pgsqlfullhistoryccbysa_url = "";
	public String db_osm2pgsqlfullhistoryccbysa_username = "";
	public String db_osm2pgsqlfullhistoryccbysa_password = "";
	public String streetlistwiki_importeruseraccount = "";
	public String streetlistwiki_importeruserpassword = "";
	public String osmosis_laststatefile = "";
	public String logging_filename = "";
	public Level logging_console_level = Level.FINEST;
	public Level logging_file_level = Level.FINEST;

	/**
	 * use standard application configuration file
	 */
	public Applicationconfiguration () {
		this("listofstreetscore.properties");
	}
	
	/**
	 * 
	 * @param configurationFilename  use explicit given application configuration file for special purposes
	 */
	public Applicationconfiguration (String configurationFilename) {

		final String dir = System.getProperty("user.dir");
		System.out.println("current dir = " + dir);

		try {
			Reader reader = new FileReader( configurationFilename );
			Properties prop = new Properties();
			prop.load( reader );
			prop.list( System.out );
		

			if( prop.getProperty("servername") != null)
				this.servername = prop.getProperty("servername");
			if( prop.getProperty("db_structure") != null)
				this.db_structure = prop.getProperty("db_structure");
			if( prop.getProperty("application_homedir") != null)
				this.application_homedir = prop.getProperty("application_homedir");
			if( prop.getProperty("application_datadir") != null)
				this.application_datadir = prop.getProperty("application_datadir");
			if( prop.getProperty("db_application_url") != null)
				this.db_application_url = prop.getProperty("db_application_url");
			if( prop.getProperty("db_application_username") != null)
				this.db_application_username = prop.getProperty("db_application_username");
			if( prop.getProperty("db_application_password") != null)
				this.db_application_password = prop.getProperty("db_application_password");
			if( prop.getProperty("db_osm2pgsql_url") != null)
				this.db_osm2pgsql_url = prop.getProperty("db_osm2pgsql_url");
			if( prop.getProperty("db_osm2pgsql_username") != null)
				this.db_osm2pgsql_username = prop.getProperty("db_osm2pgsql_username");
			if( prop.getProperty("db_osm2pgsql_password") != null)
				this.db_osm2pgsql_password = prop.getProperty("db_osm2pgsql_password");
			if( prop.getProperty("db_osm2pgsqlfullhistoryodbl_url") != null)
				this.db_osm2pgsqlfullhistoryodbl_url = prop.getProperty("db_osm2pgsqlfullhistoryodbl_url");
			if( prop.getProperty("db_osm2pgsqlfullhistoryodbl_username") != null)
				this.db_osm2pgsqlfullhistoryodbl_username = prop.getProperty("db_osm2pgsqlfullhistoryodbl_username");
			if( prop.getProperty("db_osm2pgsqlfullhistoryodbl_password") != null)
				this.db_osm2pgsqlfullhistoryodbl_password = prop.getProperty("db_osm2pgsqlfullhistoryodbl_password");
			if( prop.getProperty("db_osm2pgsqlfullhistoryccbysa_url") != null)
				this.db_osm2pgsqlfullhistoryccbysa_url = prop.getProperty("db_osm2pgsqlfullhistoryccbysa_url");
			if( prop.getProperty("db_osm2pgsqlfullhistoryccbysa_username") != null)
				this.db_osm2pgsqlfullhistoryccbysa_username = prop.getProperty("db_osm2pgsqlfullhistoryccbysa_username");
			if( prop.getProperty("db_osm2pgsqlfullhistoryccbysa_password") != null)
				this.db_osm2pgsqlfullhistoryccbysa_password = prop.getProperty("db_osm2pgsqlfullhistoryccbysa_password");
			if( prop.getProperty("streetlistwiki_importeruseraccount") != null)
				this.streetlistwiki_importeruseraccount = prop.getProperty("streetlistwiki_importeruseraccount");
			if( prop.getProperty("streetlistwiki_importeruserpassword") != null)
				this.streetlistwiki_importeruserpassword = prop.getProperty("streetlistwiki_importeruserpassword");
			if( prop.getProperty("osmosis_laststatefile") != null)
				this.osmosis_laststatefile = prop.getProperty("osmosis_laststatefile");
			if( prop.getProperty("logging_filename") != null)
				this.logging_filename = prop.getProperty("logging_filename");
			if( prop.getProperty("logging_console_level") != null)
				this.logging_console_level = Level.parse(prop.getProperty("logging_console_level"));
			if( prop.getProperty("logging_file_level") != null)
				this.logging_file_level = Level.parse(prop.getProperty("logging_file_level"));

			
			System.out.println(" .servername                              ==="+this.servername+"===");
			System.out.println(" .application_homedir                     ==="+this.application_homedir+"===");
			System.out.println(" .application_datadir                     ==="+this.application_datadir+"===");
			System.out.println(" .db_application_url                      ==="+this.db_application_url+"===");
			System.out.println(" .db_application_username                 ==="+this.db_application_username+"===");
			System.out.println(" .db_application_password                 ==="+this.db_application_password+"===");
			System.out.println(" .db_osm2pgsql_url                        ==="+this.db_osm2pgsql_url+"===");
			System.out.println(" .db_osm2pgsql_username                   ==="+this.db_osm2pgsql_username+"===");
			System.out.println(" .db_osm2pgsql_password                   ==="+this.db_osm2pgsql_password+"===");
			System.out.println(" .db_osm2pgsqlfullhistoryodbl_url         ==="+this.db_osm2pgsqlfullhistoryodbl_url+"===");
			System.out.println(" .db_osm2pgsqlfullhistoryodbl_username    ==="+this.db_osm2pgsqlfullhistoryodbl_username+"===");
			System.out.println(" .db_osm2pgsqlfullhistoryodbl_password    ==="+this.db_osm2pgsqlfullhistoryodbl_password+"===");
			System.out.println(" .db_osm2pgsqlfullhistoryccbysa_url       ==="+this.db_osm2pgsqlfullhistoryccbysa_url+"===");
			System.out.println(" .db_osm2pgsqlfullhistoryccbysa_username  ==="+this.db_osm2pgsqlfullhistoryccbysa_username+"===");
			System.out.println(" .db_osm2pgsqlfullhistoryccbysa_password  ==="+this.db_osm2pgsqlfullhistoryccbysa_password+"===");
			System.out.println(" .streetlistwiki_importeruseraccount      ==="+this.streetlistwiki_importeruseraccount+"===");
			System.out.println(" .streetlistwiki_importeruserpassword     ==="+this.streetlistwiki_importeruserpassword+"===");
			System.out.println(" .osmosis_laststatefile                   ==="+this.osmosis_laststatefile+"===");
			System.out.println(" .logging_filename                        ==="+this.logging_filename +"===");
			System.out.println(" .logging_console_level                   ==="+this.logging_console_level.toString() +"===");
			System.out.println(" .logging_file_level                      ==="+this.logging_file_level.toString() +"===");

		} catch (Exception e) {
			System.out.println("ERROR: failed to read file ==="+configurationFilename+"===");
			System.out.println(" (cont.)   in directory    ===" + System.getProperty("user.dir") + "===");
			e.printStackTrace();
			return;
		}
	}
}
