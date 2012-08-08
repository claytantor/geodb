package com.welocally.geodb.services.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class GeoDBUtilsApp {
	
	static Logger logger = 
		Logger.getLogger(GeoDBUtilsApp.class);

	/**
	 * 
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		BufferedReader br = null;
		
		Map<String,String> argsModel = makeArgsModel(args);
		
		
		
		StringBuffer buf = new StringBuffer();
					
		try {
			
			if(argsModel.get("input")==null)
				throw new RuntimeException("input type of SYSTEM|FILE is required");
			else{
				if(argsModel.get("input").equalsIgnoreCase("FILE")){
					br = 
				          new BufferedReader(
				        		  new InputStreamReader(
				        				  new FileInputStream(new File(argsModel.get("file")))));
					
				} else if(argsModel.get("input").equalsIgnoreCase("SYSTEM")) {
					br =  new BufferedReader(new InputStreamReader(System.in));
				} else {
					throw new RuntimeException("input type of SYSTEM|FILE is required");
				}
			}
			
	        
	     // read stdin buffer until EOF (or skip)
	    	while(br.ready()){
	    		buf.append( br.readLine());
	    	}
	    	
	    	if(!buf.toString().isEmpty()){ 
	    	    		
	    		JSONObject harness = new JSONObject(buf.toString());
	    		
	    		setSystemProperties(harness.getJSONArray("properties"));
	    		
	    		doLoggingConfig() ;
	    		
	    		ApplicationContext ctx = new ClassPathXmlApplicationContext(
	    			    new String[] {"geodb-applicationContext.xml"});
	    		
	    		logger.debug("doing command:"+harness.getString("bean"));
//	    		if(ctx.getBean(harness.getString("bean")) instanceof org.mortbay.jetty.Server){
//	    			logger.debug("server found");
//	    		} else {
	    			CommandSupport beanCommand = (CommandSupport)ctx.getBean(harness.getString("bean"));
		    		beanCommand.doCommand(harness.getJSONObject("command"));
		    		System.exit(0);
	    		//}
	    		
    		
	    	}
	        
	        
	    } catch (IOException e) {
	        logger.error("problem reading input", e);
	    } catch (JSONException e) {
	    	logger.error("error parsing input", e);
		} 
		catch (CommandException e) {
			logger.error("cannot execute command", e);
		}

	}
	
	private static void setSystemProperties(JSONArray properties) 
	throws CommandException, JSONException{
		for (int i = 0; i < properties.length(); i++) {
			String property=properties.getString(i);
			String[] prop = property.split("=");
			System.setProperty(prop[0], prop[1]);
		}
	}
	
	private static void doLoggingConfig() 
	throws CommandException, JSONException{
		Properties p = System.getProperties();
		String loggingConfig = p.getProperty("log4j.config");
		//FileInputStream f = new FileInputStream(new File(loggingConfig));
		System.out.println("trying to setup logging config:"+p.getProperty("log4j.config"));
		if(loggingConfig != null){
			DOMConfigurator.configure(loggingConfig);
		}
		logger.debug("finished");
		
	}
	
	public static Map<String,String> makeArgsModel(String[] args) throws RuntimeException {
		Map<String,String> model = new HashMap<String,String>();
		for (int i = 0; i < args.length; i++) {
			String[] nv = args[i].split("=");
			model.put(nv[0].replace("--", ""), nv[1]);
		}
		return model;
		
	}	

	

}
