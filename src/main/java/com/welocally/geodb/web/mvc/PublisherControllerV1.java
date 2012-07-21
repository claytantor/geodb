package com.welocally.geodb.web.mvc;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.welocally.geodb.services.db.DbException;
import com.welocally.geodb.services.db.IdGen;
import com.welocally.geodb.services.db.JsonDatabase;
import com.welocally.geodb.services.db.JsonDatabase.StatusType;

@Controller
@RequestMapping("/publisher/1_0")
public class PublisherControllerV1 extends AbstractJsonController {
	
	static Logger logger = 
		Logger.getLogger(PublisherControllerV1.class);
	
	
	@Autowired 
	@Qualifier("dynamoJsonDatabase")
	JsonDatabase jsonDatabase;
	
	@Autowired IdGen idGen; 
		
	@Value("${publishersDatabase.collectionName:dev.publishers}")
	String publisherCollection;
	

    @RequestMapping(value = "/saveAll", method = RequestMethod.PUT)
    public ModelAndView putAll(@RequestBody String requestJson, HttpServletRequest req){
        ModelAndView mav = new ModelAndView("mapper-result");
        
        
        try {
            JSONArray publishers = new JSONArray(requestJson);
            for (int i = 0; i < publishers.length(); i++) {
                JSONObject publisher = publishers.getJSONObject(i);
                String id = publisher.getString("name");
                publisher.put("_id", id);
                jsonDatabase.put(publisher, publisherCollection, id,
                        JsonDatabase.EntityType.PUBLISHER, StatusType.PUBLISHED);  
                
                Map<String, Object> result = new HashMap<String,Object>();
                result.put("id", id);
                result.put("status", "SUCCEED");
                mav.addObject("mapperResult", makeModelJson(result));
                
            }
                      

        } catch (JSONException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } catch (DbException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } 
      
        
        return mav;
    }
    
    
    @RequestMapping(value = "/deleteAll", method = RequestMethod.PUT)
    public ModelAndView deleteAll(@RequestBody String requestJson, HttpServletRequest req){
        ModelAndView mav = new ModelAndView("mapper-result");
        
        
        try {
            JSONArray publishers = new JSONArray(requestJson);
            for (int i = 0; i < publishers.length(); i++) {
                JSONObject publisher = publishers.getJSONObject(i);
                String id = publisher.getString("name");
                jsonDatabase.delete(publisherCollection, id);
                
                Map<String, Object> result = new HashMap<String,Object>();
                result.put("id", id);
                result.put("status", "SUCCEED");
                mav.addObject("mapperResult", makeModelJson(result));
                
            }
                      

        } catch (JSONException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } catch (DbException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } 
      
        
        return mav;
    }


	public void setJsonDatabase(JsonDatabase jsonDatabase) {
		this.jsonDatabase = jsonDatabase;
	}


	public void setIdGen(IdGen idGen) {
		this.idGen = idGen;
	}




}
