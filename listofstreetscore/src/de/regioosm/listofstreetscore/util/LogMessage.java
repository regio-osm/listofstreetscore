package de.regioosm.listofstreetscore.util;

/**
 * A single input source for an blabla on LogMessage
 *
 * <blockquote>
 * <em>This module, both source code and documentation, is in the
 * Public Domain, and comes with <strong>NO WARRANTY</strong>.</em>
 * See <a href='http://www.saxproject.org'>http://www.saxproject.org</a>
 * for further information.
 * </blockquote>
 */

/*

	V1.1, 19.02.2013, Dietmar Seifert
		*	class was not in production.
		*	now the message will be stored in files in filesystem

	V1.0, 18.12.2012, Dietmar Seifert
		* store Messages about Informations, Warnings and Errors into database

*/


import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


public class LogMessage {

	public static final int CLASS_INFORMATION = 1;
	public static final int CLASS_WARNING = 2;
	public static final int CLASS_ERROR = 3;
	public static final int CLASS_CRITICALERROR = 4;
	String[] class_text = { "", "Information", "Warnung", "Fehler", "KRITISCHER_FEHLER"};

	public LogMessage(					int		message_class,
										String	module, 
										String	municipality_name,
										long	municipality_id, 
										String	message_text
					) {
		PrintWriter output_handle = null;
		String outputstring = "";
		String log_message_filename = "log_message_" + module + ".txt";
		try {

			java.util.Date time_now = new java.util.Date();
			DateFormat time_formatter_iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");		// in iso8601 format, with timezone						

			outputstring = class_text[message_class] + "\t";
			outputstring += municipality_name + "\t";			
			outputstring += municipality_id + "\t";
			outputstring += message_text + "\t";
			outputstring += time_formatter_iso8601.format(time_now);

			output_handle = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(log_message_filename,true),"UTF-8")));

			output_handle.println(outputstring);
			output_handle.close();
		
		}
		catch (IOException ioerror) {
			System.out.println("ERROR: couldn't open file to write, filename was ==="+log_message_filename+"===   message was ==="+outputstring+"===");
			ioerror.printStackTrace();
			return;
		}
	}		
}
