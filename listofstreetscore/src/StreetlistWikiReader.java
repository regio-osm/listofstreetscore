/*
		V1.0, 2013-03-21, Dietmar Seifert
			new Method to compare new streetlist with existing one in streetlist wiki
*/

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import de.regioosm.listofstreetscore.util.Applicationconfiguration;
import de.regioosm.listofstreetscore.util.Streetlist_from_streetlistwiki;


public class StreetlistWikiReader {

	public static void main(String args[]) {
		String				args_output = "db";
		String				args_name = "";
		String				args_mode = "complete";
		String				args_country = "";
		String parameterConfiguration = "";
		Applicationconfiguration configuration = null;
		

		for(int lfdnr=0;lfdnr<args.length;lfdnr++) {
			System.out.println("args["+lfdnr+"] ==="+args[lfdnr]+"===");
		}
		if((args.length >= 1) && (args[0].equals("-h"))) {
			System.out.println("-mode complete|recentchanges");
			System.out.println("-name municipalityname");
			System.out.println("-country country");
			System.out.println("-configuration filenameorabsolutepathandfilename");
			return;
		}

		java.util.Date time_program_starttime = new java.util.Date();
		java.util.Date time_program_endtime;
		System.out.println("Program update_new_streetlist_with_existing.java: Start Time: "+time_program_starttime.toString());

		try {
			if(args.length >= 1) {
				int args_ok_count = 0;
				for(int argsi=0;argsi<args.length;argsi+=2) {
					if(args[argsi].equals("-output")) {
						args_output = args[argsi+1];
						args_ok_count += 2;
					} else if(args[argsi].equals("-name")) {
							args_name = URLDecoder.decode(args[argsi+1], "UTF-8");
						args_ok_count += 2;
					} else if(args[argsi].equals("-country")) {
						args_country = URLDecoder.decode(args[argsi+1], "UTF-8");
						args_ok_count += 2;
					} else if(args[argsi].equals("-mode")) {
						args_mode = args[argsi+1];
						args_ok_count += 2;
						if( ( ! args_mode.equals("complete")) &&
							( ! args_mode.equals("recentchanges"))) {
							System.out.println("ERROR: invalide value for application parameter -mode, was ==="+args_mode+"===");
							return;
						}
					} else if(args[argsi].equals("-configuration")) {
						parameterConfiguration = args[argsi+1];
						args_ok_count += 2;
					} else {
						System.out.println("ERROR: unexpected commandline option ==="+args[argsi]+"=== STOP");
						return;
					}
				}
				if(args_ok_count != args.length) {
					System.out.println("ERROR: not all programm parameters were valid, STOP");
					return;
				}
			}
		} catch (UnsupportedEncodingException e) {
			System.out.println(e.toString());
			return;
		}

			// read main or explicit given configuration
		if(parameterConfiguration.equals(""))
			configuration = new Applicationconfiguration();
		else
			configuration = new Applicationconfiguration(parameterConfiguration);

		Streetlist_from_streetlistwiki wiki_streetlist_object = new Streetlist_from_streetlistwiki();
		wiki_streetlist_object.setConfiguration(configuration);
		try {
			String pagecontent = wiki_streetlist_object.read(args_mode, args_name, args_country, args_output);
		}
		catch (Exception e) {
			System.out.println("ERROR: Exception from sub-method wiki_streetlist_object.read, Details ...");
			System.out.println(e.toString());
			return;
		}
		if((wiki_streetlist_object != null) && (wiki_streetlist_object.strassenanzahl != null)) {
			System.out.println("erhaltene Anzahl Straßen: "+wiki_streetlist_object.strassenanzahl);
			for(Integer strassenindex = 0; strassenindex < wiki_streetlist_object.strassenanzahl; strassenindex++) {
				System.out.println("Straße # "+strassenindex+" ==="+wiki_streetlist_object.strassen[strassenindex]+"===");
			}
		}

		time_program_endtime = new java.util.Date();
		System.out.println("Program: Ended Time: "+time_program_endtime.toString());
		System.out.println("Program: Duration in ms: "+(time_program_endtime.getTime()-time_program_starttime.getTime()));
	}

}
	
