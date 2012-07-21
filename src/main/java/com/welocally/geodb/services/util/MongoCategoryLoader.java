package com.welocally.geodb.services.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.welocally.geodb.services.app.CommandException;
import com.welocally.geodb.services.app.CommandSupport;
import com.welocally.geodb.services.db.DbException;
import com.welocally.geodb.services.db.DbPage;
import com.welocally.geodb.services.db.IdGen;
import com.welocally.geodb.services.db.JsonDatabase;

@Component
public class MongoCategoryLoader  {
	
//	
//
//	static Logger logger = 
//		Logger.getLogger(MongoCategoryLoader.class);
//	
//	@Autowired 
//	@Qualifier("mongoJsonDatabase")
//	JsonDatabase jsonDatabase;
//	
//	@Autowired IdGen idGen;
//	
//
//	@Override
//	public void doCommand(JSONObject command) throws CommandException {
//		
//		try {
//			
//			load(command.getString("file"), command.getString("target"), command.getInt("maxRecords")) ;
//			
//		} catch (UnknownHostException e) {
//			logger.error(e);
//			throw new CommandException(e);
//		} catch (JSONException e) {
//			logger.error(e);
//			throw new CommandException(e);
//		} catch (IOException e) {
//			logger.error(e);
//			throw new CommandException(e);
//		} catch (DbException e) {
//			logger.error(e);
//			throw new CommandException(e);
//		}
//		
//		
//	}
//	
//	public void load(String fileName, String targetCollection, int maxRecords) 
//	throws IOException, JSONException, DbException {
//
//
//		JSONArray classifiers = new JSONArray();
//		FileReader reader = 
//			new FileReader(new File(fileName));
//		
//		BufferedReader br = new BufferedReader(reader); 
//		String s = null; 
//		int count=0;
//		while((s = br.readLine()) != null && count<maxRecords) { 
//			JSONObject classifier = new JSONObject(s);
//			classifiers.put(classifier);
//			count++;
//		} 
//		reader.close(); 
//						
//		loadClassifiers(
//				classifiers, 
//				 targetCollection);
//
//						
//	}
//
//	
//	private void loadClassifiers(
//			JSONArray classifiers, 
//			String targetCollection) 
//	throws DbException, JSONException{
//		//for each classifier
//		for (int i = 0; i < classifiers.length(); i++) {
//			JSONObject classifier = classifiers.getJSONObject(i);
//			if(classifier.isNull("_id")){
//				//try to look the classifier up by the three fields
//				DbPage dbpage =
//					jsonDatabase.findByExample(targetCollection, 1,  classifier );
//				if(dbpage.getCount()==0){
//					//not found? add it
//					loadClassifierIntoDb(classifier, targetCollection);
//				}
//			} else {
//				//over write
//				jsonDatabase.put(classifier, targetCollection, classifier.getString("_id"), JsonDatabase.EntityType.CLASSIFER);
//			}
//			
//	
//		}	
//
//	}
//	
//	public String loadClassifierIntoDb(JSONObject classifier, String targetCollection) 
//	throws JSONException, DbException{
//		String id = idGen.genBasic(10);	
//		jsonDatabase.put(classifier, targetCollection, id, JsonDatabase.EntityType.CLASSIFER);
//		return id;
//	}


}
