package com.welocally.geodb.services.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;

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
import com.welocally.geodb.services.db.JsonDatabase.StatusType;
import com.welocally.geodb.services.jmx.LoadMonitor;

/**
 * supplied with a specific region the bounding loader will
 * load data into 
 * @author claygraham
 *
 */
@Component
public class DynamoLineLoader implements CommandSupport {
	
	static Logger logger = 
		Logger.getLogger(DynamoLineLoader.class);
	
	@Autowired WelocallyJSONUtils welocallyJSONUtils;
	
	@Autowired LoadMonitor loadMonitor;
	
	@Autowired
	@Qualifier("dynamoJsonDatabase")
	private JsonDatabase jsonDatabase;

	@Override
	public void doCommand(JSONObject command) throws CommandException {
		try {			
			loadMonitor.reset();
			load(command.getString("file"), command.getInt("maxRecords"),
			        command.getString("collection"), JsonDatabase.EntityType.valueOf(command.getString("type")));
	
		} catch (DbException e) {
			logger.error(e);
			throw new CommandException(e);
		} catch (JSONException e) {
			logger.error(e);
			throw new CommandException(e);
        }

	}

	public void load(String fileName, int maxRecords, String collectionName, JsonDatabase.EntityType type) throws DbException {
		try {
			logger.debug("starting load");
			String[] files = fileName.split(",");
			for (String file : files) {
				FileReader reader = 
					new FileReader(new File(file));
				
				BufferedReader br = new BufferedReader(reader); 
				String s = null; 
				int count=0;
				while((s = br.readLine()) != null && count<maxRecords) { 
					
					JSONObject doc = 
						new JSONObject(s);
					
					logger.debug("adding document:"+doc.getString("_id"));
					jsonDatabase.put(doc,collectionName, doc.getString("_id"), type, StatusType.PUBLISHED);
					loadMonitor.increment();
			        count++;
					
				} 
				reader.close(); 
			
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
	
	

}
