package com.welocally.geodb.services.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.welocally.geodb.services.app.CommandException;
import com.welocally.geodb.services.app.CommandSupport;
import com.welocally.geodb.services.db.DbException;
import com.welocally.geodb.services.db.JsonDatabase;
import com.welocally.geodb.services.jmx.LoadMonitor;

import au.com.bytecode.opencsv.CSVWriter;

@Component
public class SolrCSVWriter implements CommandSupport {
	
	public static final String DEFAULT_POST_URL = "http://localhost:8983/solr/update";
	public static final String POST_ENCODING = "UTF-8";
	public static final String VERSION_OF_THIS_TOOL = "1.2";
	
	//need to make this json
	private static final String SOLR_OK_RESPONSE_EXCERPT = "<int name=\"status\">0</int>";

	//private static final String DEFAULT_COMMIT = "yes";

	private static final String DATA_MODE_FILES = "files";
	private static final String DATA_MODE_ARGS = "args";
	private static final String DATA_MODE_STDIN = "stdin";
	//private static final String DEFAULT_DATA_MODE = DATA_MODE_FILES;

	private static final Set<String> DATA_MODES = new HashSet<String>();
	static {
		DATA_MODES.add(DATA_MODE_FILES);
		DATA_MODES.add(DATA_MODE_ARGS);
		DATA_MODES.add(DATA_MODE_STDIN);
	}

	protected URL solrUrl;

	private class PostException extends RuntimeException {
		PostException(String reason, Throwable cause) {
			super(reason + " (POST URL=" + solrUrl + ")", cause);
		}
	}

	
	static Logger logger = 
		Logger.getLogger(SolrCSVWriter.class);
	
	@Autowired WelocallyJSONUtils welocallyJSONUtils;
	
	@Autowired LoadMonitor loadMonitor;
	
	
	@Autowired
	@Qualifier("dynamoJsonDatabase")
	private JsonDatabase jsonDatabase;

	
	public void doCommand(JSONObject command) throws CommandException {
		try {			
			loadMonitor.reset();
			write(command.getString("file"),
					command.getString("target"));
			
	
		} catch (DbException e) {
			logger.error(e);
			throw new CommandException(e);
		} catch (JSONException e) {
			logger.error(e);
			throw new CommandException(e);
        }

	}

	/* (non-Javadoc)
     * @see com.welocally.geodb.services.util.JsonStoreLoader#load(java.lang.String, int, int, java.lang.String)
     */
	public void write(String fileName, String target) throws DbException {

		logger.info("version " + VERSION_OF_THIS_TOOL);
		
		try {

			
			String[] files = fileName.split(",");
			for (String file : files) {
				FileReader reader = 
					new FileReader(new File(file));
				
				BufferedReader br = new BufferedReader(reader); 
				String s = null; 
				BufferedWriter writer = new BufferedWriter(new FileWriter(target));
				CSVWriter csvWriter = new CSVWriter(writer);
				writer.write("_id,search,location_0_coordinate,location_1_coordinate");
				writer.newLine();
				while((s = br.readLine()) != null) { 
					
					JSONObject place = 
						new JSONObject(s);
					
					
					welocallyJSONUtils.updatePlaceToWelocallyFormat(place);
					JSONObject doc = welocallyJSONUtils.makeIndexablePlace(place);
					csvWriter.writeNext(new String[] {
						doc.getString("_id"),
						doc.getString("search"),
						doc.getString("location_0_coordinate"),
						doc.getString("location_1_coordinate")});
				} 
				reader.close(); 
				logger.debug("commit");
				csvWriter.flush();			
			}

			System.exit(0);				
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
	}
}
