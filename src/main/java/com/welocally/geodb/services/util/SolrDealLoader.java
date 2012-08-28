package com.welocally.geodb.services.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.welocally.geodb.services.app.CommandException;
import com.welocally.geodb.services.app.CommandSupport;
import com.welocally.geodb.services.db.DbException;
import com.welocally.geodb.services.jmx.LoadMonitor;

@Component
public class SolrDealLoader implements CommandSupport, JsonStoreLoader {
	
	//public static final String DEFAULT_POST_URL = "http://localhost:8983/solr/update/json";
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
		Logger.getLogger(SolrDealLoader.class);
	
	@Autowired WelocallyJSONUtils welocallyJSONUtils;
	
	@Autowired LoadMonitor loadMonitor;
	

	
	public void doCommand(JSONObject command) throws CommandException {
		try {			
			loadMonitor.reset();
			deleteAll(command.getString("endpoint"));
			load(command.getString("file"), command.getInt("maxRecords"),
					command.getInt("commitEvery"),
					command.getString("endpoint"));
			
	
		} catch (DbException e) {
			logger.error(e);
			throw new CommandException(e);
		} catch (JSONException e) {
			logger.error(e);
			throw new CommandException(e);
        } catch (IOException e) {
            logger.error(e);
            throw new CommandException(e);
        }

	}

	/* (non-Javadoc)
     * @see com.welocally.geodb.services.util.JsonStoreLoader#load(java.lang.String, int, int, java.lang.String)
     */
	public void load(String fileName, Integer maxRecords, Integer commitEvery, String endpoint) throws DbException {

		info("version " + VERSION_OF_THIS_TOOL);
		
		try {
			logger.debug("starting load to:"+endpoint);
			
			URL solrUrl = new URL(endpoint);
			
			String[] files = fileName.split(",");
			for (String file : files) {
				FileReader reader = 
					new FileReader(new File(file));
				
				BufferedReader br = new BufferedReader(reader); 
				String s = null; 
				Integer count=0;
				StringWriter sw = new StringWriter();
				while((s = br.readLine()) != null && count<maxRecords) { 
					
					JSONObject place = 
						new JSONObject(s);

					loadSingle(place, count, commitEvery, sw, endpoint);
					count++;
					
				} 
				reader.close(); 
				logger.debug("commit");
				commit(sw, solrUrl);
				sw.flush();
				System.exit(1);
			
			}
		
				
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
	}
	

    /* (non-Javadoc)
     * @see com.welocally.geodb.services.util.JsonStoreLoader#loadSingle(org.json.JSONObject, int, int, java.io.StringWriter)
     */
    public void deleteAll(String endpoint) throws JSONException, IOException{
        logger.debug("deleting all documents");

        URL solrUrl = new URL(endpoint);
        //<delete><query>*:*</query></delete>
        JSONObject solrCommand = 
            new JSONObject("{\"delete\": {\"query\":\"*:*\"}}");
        StringWriter sw = new StringWriter();
        ByteArrayInputStream bais = new ByteArrayInputStream(solrCommand.toString().getBytes());
        BufferedReader newReader = new BufferedReader(new InputStreamReader(bais));
                            
        postData(newReader, sw, solrUrl);
        
        commit(sw, solrUrl);
        sw.flush();
        sw.close();
              
                            
        loadMonitor.increment();
        
        
    }
	
	
	/* (non-Javadoc)
     * @see com.welocally.geodb.services.util.JsonStoreLoader#loadSingle(org.json.JSONObject, int, int, java.io.StringWriter)
     */
	public void loadSingle(JSONObject deal, Integer count, Integer commitEvery, StringWriter sw, String endpoint) throws JSONException, IOException{
		logger.debug("adding document:"+deal.getString("id"));
		
		URL solrUrl = new URL(endpoint);
		
		JSONObject doc = welocallyJSONUtils.makeIndexableDeal(deal);
		JSONObject solrCommand = 
			new JSONObject("{\"add\": {\"doc\":"+doc.toString()+"}}");
		
		ByteArrayInputStream bais = new ByteArrayInputStream(solrCommand.toString().getBytes());
		BufferedReader newReader = new BufferedReader(new InputStreamReader(bais));
							
        postData(newReader, sw, solrUrl);
        
        //commit only every x docs
        if(count%commitEvery==0){
        	logger.debug("commit");
        	commit(sw, solrUrl);
        	sw.flush();
        	sw.close();
        	sw = new StringWriter();
        }
        					
		loadMonitor.increment();
		
		
	}
	
	
	/* (non-Javadoc)
     * @see com.welocally.geodb.services.util.JsonStoreLoader#loadSingle(org.json.JSONObject, int, int, java.io.StringWriter)
     */
    public void deleteSingle(String id, Integer count, Integer commitEvery, StringWriter sw, String endpoint) throws JSONException, IOException{
        
        
        logger.debug("removing document:"+id);

        URL solrUrl = new URL(endpoint);
        
        //JSONObject doc = welocallyJSONUtils.makeIndexablePlace(place);
        JSONObject solrCommand = 
            new JSONObject("{\"delete\": {\"id\":"+id+"}}");
        
        ByteArrayInputStream bais = new ByteArrayInputStream(solrCommand.toString().getBytes());
        BufferedReader newPlaceReader = new BufferedReader(new InputStreamReader(bais));
                            
        postData(newPlaceReader, sw, solrUrl);
        
        commit(sw, solrUrl);
        sw.flush();
        sw.close();
   
    }

	  /** Post all filenames provided in args, return the number of files posted*/
	  int postFiles(String [] args,int startIndexInArgs) throws IOException {
	    int filesPosted = 0;
	    for (int j = startIndexInArgs; j < args.length; j++) {
	      File srcFile = new File(args[j]);
	      final StringWriter sw = new StringWriter();
	      
	      if (srcFile.canRead()) {
	        info("POSTing file " + srcFile.getName());
	        postFile(srcFile, sw, solrUrl);
	        filesPosted++;
	        warnIfNotExpectedResponse(sw.toString(),SOLR_OK_RESPONSE_EXCERPT);
	      } else {
	        warn("Cannot read input file: " + srcFile);
	      }
	    }
	    return filesPosted;
	  }
	  
	  /** Check what Solr replied to a POST, and complain if it's not what we expected.
	   *  TODO: parse the response and check it XMLwise, here we just check it as an unparsed String  
	   */
	  static void warnIfNotExpectedResponse(String actual,String expected) {
	    if(actual.indexOf(expected) < 0) {
	    	logger.info("Unexpected response from Solr: '" + actual + "' does not contain '" + expected + "'");
	    }
	  }
	  
	  static void warn(String msg) {
	    logger.warn("SolrPlaceLoader: WARNING: " + msg);
	  }

	  static void info(String msg) {
	   logger.debug("SolrPlaceLoader: " + msg);
	  }

	  static void fatal(String msg) {
	    logger.error("SolrPlaceLoader: FATAL: " + msg);
	    System.exit(1);
	  }


	  /**
	   * Does a simple commit operation 
	   */
	  public void commit(Writer output, URL solrUrl) throws IOException {
	    postData(new StringReader("{\"commit\":{}}"), output, solrUrl);
	  }

	  /**
	   * Opens the file and posts it's contents to the solrUrl,
	   * writes to response to output.
	   * @throws UnsupportedEncodingException 
	   */
	  public void postFile(File file, Writer output, URL solrUrl) 
	    throws FileNotFoundException, UnsupportedEncodingException {

	    // FIXME; use a real XML parser to read files, so as to support various encodings
	    // (and we can only post well-formed XML anyway)
	    Reader reader = new InputStreamReader(new FileInputStream(file),POST_ENCODING);
	    try {
	      postData(reader, output, solrUrl);
	    } finally {
	      try {
	        if(reader!=null) reader.close();
	      } catch (IOException e) {
	        throw new PostException("IOException while closing file", e);
	      }
	    }
	  }

	  /**
	   * Reads data from the data reader and posts it to solr,
	   * writes to the response to output
	   */
	  public void postData(Reader data, Writer output, URL solrUrl) {

	    HttpURLConnection urlc = null;
	    try {
	      urlc = (HttpURLConnection) solrUrl.openConnection();
	      try {
	        urlc.setRequestMethod("POST");
	        
	      } catch (ProtocolException e) {
	        throw new PostException("Shouldn't happen: HttpURLConnection doesn't support POST??", e);
	      }
	      urlc.setDoOutput(true);
	      urlc.setDoInput(true);
	      urlc.setUseCaches(false);
	      urlc.setAllowUserInteraction(false);
	      //urlc.setRequestProperty("Content-type", "text/xml; charset=" + POST_ENCODING);
	      urlc.setRequestProperty("Content-type", "application/json; charset=" + POST_ENCODING);
	      
	      OutputStream out = urlc.getOutputStream();
	      
	      try {
	        Writer writer = new OutputStreamWriter(out, POST_ENCODING);
	        pipe(data, writer);
	        writer.close();
	      } catch (IOException e) {
	        throw new PostException("IOException while posting data", e);
	      } finally {
	        if(out!=null) out.close();
	      }
	      
	      InputStream in = urlc.getInputStream();
	      try {
	        Reader reader = new InputStreamReader(in);
	        pipe(reader, output);
	        reader.close();
	      } catch (IOException e) {
	        throw new PostException("IOException while reading response", e);
	      } finally {
	        if(in!=null) in.close();
	      }
	      
	    } catch (IOException e) {
	      try {
	        fatal("Solr returned an error: " + urlc.getResponseMessage());
	      } catch (IOException f) { }
	      fatal("Connection error (is Solr running at " + solrUrl + " ?): " + e);
	    } finally {
	      if(urlc!=null) urlc.disconnect();
	    }
	  }

	  /**
	   * Pipes everything from the reader to the writer via a buffer
	   */
	  private static void pipe(Reader reader, Writer writer) throws IOException {
	    char[] buf = new char[1024];
	    int read = 0;
	    while ( (read = reader.read(buf) ) >= 0) {
	      writer.write(buf, 0, read);
	    }
	    writer.flush();
	  }
	

}
