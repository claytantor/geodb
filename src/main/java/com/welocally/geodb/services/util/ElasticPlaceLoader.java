package com.welocally.geodb.services.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.welocally.geodb.services.app.CommandException;
import com.welocally.geodb.services.app.CommandSupport;
import com.welocally.geodb.services.db.DbException;
import com.welocally.geodb.services.db.JsonDatabase;
import com.welocally.geodb.services.jmx.LoadMonitor;

@Component
public class ElasticPlaceLoader implements CommandSupport {
    
    static Logger logger = 
        Logger.getLogger(ElasticPlaceLoader.class);
    
    @Autowired WelocallyJSONUtils welocallyJSONUtils;
    
    @Autowired LoadMonitor loadMonitor;
    

    
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

    

    public void load(String fileName, int maxRecords, String collectionName, JsonDatabase.EntityType type) 
    throws DbException {
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
                    logger.debug(s);
                    JSONObject doc = 
                        new JSONObject(s);
                    
                    logger.debug("adding document:"+doc.getString("id"));
                    put(doc);
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
    
    public void put(JSONObject doc) throws JSONException{
        HttpClient httpclient = new HttpClient();
        httpclient.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
        String url = "http://localhost:9200/place/"+doc.getString("id");
        PutMethod put = new PutMethod(url);
        
        RequestEntity placeRequest = new ByteArrayRequestEntity(doc.toString().getBytes(),"text/plain; charset=UTF-8");
        
        put.setRequestEntity(placeRequest);
        
        try{
            int result = httpclient.executeMethod(put);
            
            logger.debug("Response status code: " + result+" Response body: "+put.getResponseBodyAsString());
            
        } catch (HttpException he) {
            logger.error("Http error connecting to '" + url + "'");
        } catch (IOException ioe){
            logger.error("Http error connecting to '" + url + "'");
        }
        
    }
	

}
