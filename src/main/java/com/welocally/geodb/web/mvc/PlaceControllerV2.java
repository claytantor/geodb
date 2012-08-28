package com.welocally.geodb.web.mvc;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.welocally.geodb.services.db.DbException;
import com.welocally.geodb.services.db.IdGen;
import com.welocally.geodb.services.db.JsonDatabase;
import com.welocally.geodb.services.db.JsonDatabase.StatusType;
import com.welocally.geodb.services.spatial.Point;
import com.welocally.geodb.services.spatial.SpatialConversionUtils;
import com.welocally.geodb.services.spatial.SpatialSearchService;
import com.welocally.geodb.services.util.JsonStoreLoader;

@Controller
@RequestMapping("/place/2_0")
public class PlaceControllerV2 extends AbstractJsonController {
	
	static Logger logger = 
		Logger.getLogger(PlaceControllerV2.class);
	
	@Qualifier("solrSearchService")
	@Autowired SpatialSearchService searchService;
	
	@Autowired 
	@Qualifier("dynamoJsonDatabase")
	JsonDatabase jsonDatabase;
	
	@Autowired IdGen idGen; 
	
	@Autowired SpatialConversionUtils spatialConversionUtils; 	
	
	@Value("${placesDatabase.collectionName:dev.places}")
	String placesCollection;
	
	@Value("${publisherDatabase.collectionName:dev.publishers}")
	String publisherCollection;
	
	@Value("${userDatabase.collectionName:dev.user}")
	String publisherPlacesCollection;
	
    @Value("${SolrSearchService.places.endpoint:http://localhost:8983/solr/select/}")
    private String searchEndpoint;
    
    @Value("${SolrPlaceLoader.places.endpoint:http://localhost:8983/solr/update/json}")
    private String updateEndpoint;
   	
	//solrDealLoader, solrPlaceLoader
    @Qualifier("solrPlaceLoader")
	@Autowired JsonStoreLoader loader;
		
    @RequestMapping(value = "/save", method = RequestMethod.PUT)
    public ModelAndView savePlacePut(@RequestBody String requestJson, HttpServletRequest req){      
        
        ModelAndView  mav = new ModelAndView("mapper-result");
              
        try {
            JSONObject place = 
                new JSONObject(requestJson);
            
             savePlace(place, mav, req);
            
        } catch (JSONException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } catch (DbException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } catch (IOException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } catch (RuntimeException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } 
        
        return mav;
        
        
    }
	
	/**
	 * FOR CROSS DOMAIN JSONP ADDS
	 * 
	 * http://localhost:8082/geodb/place/1_0/save.json?callback=jQuery16109296544857788831_1332260765859&properties%5Baddress%5D=null+null&properties%5Bcity%5D=Oakland&properties%5Bprovince%5D=CA&properties%5Bpostcode%5D=null&properties%5Bcountry%5D=US&type=Place&classifiers%5B0%5D%5Btype%5D=Food+%26+Drink&classifiers%5B0%5D%5Bcategory%5D=Restaurant&classifiers%5B0%5D%5Bsubcategory%5D=Japanese&geometry%5Btype%5D=Point&geometry%5Bcoordinates%5D%5B%5D=-122.2711137&geometry%5Bcoordinates%5D%5B%5D=37.8043637&_=1332260944730 
	 * 
	 * @param callback
	 * @param req
	 * @return
	 */

	@RequestMapping(value = "/save", method = RequestMethod.GET)
    public ModelAndView savePlaceJSONP(@RequestParam(required=false) String callback, HttpServletRequest req){
       
	    ModelAndView mav = null;
        if(StringUtils.isEmpty(callback))
            mav = new ModelAndView("mapper-result");
        else {
            mav = new ModelAndView("jsonp-mapper-result");
            mav.addObject(
                    "callback", 
                    callback);
        }
        
        try {
            JSONObject placeQueryString =
                new JSONObject(req.getParameterMap());
            
            JSONObject place = 
                spatialConversionUtils.convertQueryStringToPlace(placeQueryString);
            
            savePlace(place, mav, req);
                       
              
        } catch (JSONException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } catch (DbException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } catch (IOException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } catch (RuntimeException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } 
        
        return mav;
    }
	
	private JSONObject checkPublisherKey(HttpServletRequest req){
	    if(req.getHeader("key") != null){   
            String publisherKey = req.getHeader("key");
                      
            try {
                return jsonDatabase.findById(publisherCollection, publisherKey);
            } catch (Exception e) {
               throw new RuntimeException(e);
            }  
                                       
        } else {  
            throw new RuntimeException("publisher key not found in header");
        }
	}
	
    public void savePlace(JSONObject place, ModelAndView mav, HttpServletRequest req) throws DbException, JSONException, IOException, RuntimeException{      
        JSONObject  publisher =  checkPublisherKey(req);
               
        
        Point p = spatialConversionUtils.getJSONPoint(place);
        
        if(place.isNull("id")){
            place.put("id", idGen.genPoint("WL_",p));
        }
        
        String id= place.getString("id");
        
        
        if(p != null && publisher != null){      
            place.put("id", id);
            place.getJSONObject("properties").put("owner", "welocally");   
            jsonDatabase.put(place, null,  placesCollection, id, JsonDatabase.EntityType.PLACE, StatusType.PUBLISHED);
            JSONObject userPlace = new JSONObject(place.toString());
            userPlace.getJSONObject("properties").put("owner", publisher.get("id"));            
            jsonDatabase.put(userPlace, null,  publisherPlacesCollection, id, JsonDatabase.EntityType.PLACE, StatusType.PUBLISHED);
            StringWriter sw = new StringWriter();
            //now add it to the index
            loader.loadSingle(place, 1, 1, sw, updateEndpoint);
            sw.flush();
            sw.close();
                            
            Map<String, Object> result = new HashMap<String,Object>();
            result.put("id", id);
            result.put("status", "SUCCEED");
            
            mav.addObject("mapperResult", makeModelJson(result));
        } 
    }
    
    @RequestMapping(value = "/load", method = RequestMethod.PUT)
    public ModelAndView loadPlaces(@RequestBody String requestJson, HttpServletRequest req) throws DbException, JSONException, IOException, RuntimeException{ 

        ModelAndView  mav = new ModelAndView("mapper-result");
        
        try {
            
            JSONObject publisher = null;

            if (req.getHeader("key") != null) {
                String publisherKey = req.getHeader("key");

                try {
                    publisher = jsonDatabase.findById(publisherCollection,
                            publisherKey);
                } catch (DbException e) {
                    throw e;
                }

            } else {
                throw new RuntimeException("publisher key not found in header");
            }   
            
            JSONArray places = 
                new JSONArray(requestJson);
            
            
            for (int i = 0; i < places.length(); i++) {
                loadPlace(places.getJSONObject(i), publisher, mav, req);
            }
            
            
        } catch (JSONException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } catch (DbException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } catch (IOException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } catch (RuntimeException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } 
        
        return mav;
        
        //        JSONObject  publisher = null;
//        
//        if(req.getHeader("key") != null){   
//            String publisherKey = req.getHeader("key");
//                      
//            try {
//                publisher = 
//                    jsonDatabase.findById(publisherCollection, publisherKey);
//            } catch (DbException e) {
//               throw e;
//            }  
//                                       
//        } else {  
//            throw new RuntimeException("publisher key not found in header");
//        }   
//        
//        for (int i = 0; i < places.length(); i++) {
//                loadPlace(places.getJSONObject(i),publisher,mav, req);
//        }
    }
    
    public void loadPlace(JSONObject place, JSONObject  publisher, ModelAndView mav, HttpServletRequest req) throws DbException, JSONException, IOException, RuntimeException{      
        
        
        Point p = spatialConversionUtils.getJSONPoint(place);
        
        if(place.isNull("id")){
            place.put("id", idGen.genPoint("WL_",p));
        }
        
        String id= place.getString("id");
        
        
        if(p != null && publisher != null){      
            place.put("id", id);
            place.getJSONObject("properties").put("owner", "welocally");   
            jsonDatabase.put(place, null,  placesCollection, id, JsonDatabase.EntityType.PLACE, StatusType.PUBLISHED);

            StringWriter sw = new StringWriter();
            //now add it to the index
            loader.loadSingle(place, 1, 1, sw, updateEndpoint);
            sw.flush();
            sw.close();
                            
            Map<String, Object> result = new HashMap<String,Object>();
            result.put("id", id);
            result.put("status", "SUCCEED");
            
            mav.addObject("mapperResult", makeModelJson(result));
        } 
    }
	
	/**
     * @param callback
     * @param req
     * @return
     */
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public ModelAndView savePlacePost(@RequestBody String requestJson, HttpServletRequest req){      
        
        ModelAndView  mav = new ModelAndView("mapper-result");
              
        try {
            JSONObject place = 
                new JSONObject(requestJson);
            
             savePlace(place, mav, req);
            
        } catch (JSONException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } catch (DbException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } catch (IOException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } 
        
        return mav;
        
        
    }
	
	
	@RequestMapping(value = "/delete/{id}", method = RequestMethod.GET)
    public ModelAndView deleteById(@PathVariable String id, @RequestParam(required=false) String callback, Model m){
	    return delete(  id, callback, m);
	}
	
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ModelAndView delete(@PathVariable String id, String callback, Model m){
	    ModelAndView mav = null;
        if(StringUtils.isEmpty(callback))
            mav = new ModelAndView("mapper-result");
        else {
            mav = new ModelAndView("jsonp-mapper-result");
            mav.addObject(
                    "callback", 
                    callback);
        }
        
        try {

            jsonDatabase.delete(placesCollection, id);
            jsonDatabase.delete(publisherPlacesCollection, id);
            
            StringWriter sw = new StringWriter();
            //now add it to the index
            loader.deleteSingle(id, 1, 1, sw, updateEndpoint);
            sw.flush();
            sw.close();
            
            
            Map<String, Object> result = new HashMap<String,Object>();
            result.put("id", id);
            result.put("status", "SUCCEED");
            
            mav.addObject("mapperResult", makeModelJson(result));
            
        } catch (DbException e) {
            logger.error("could not get results");
            if(e.getExceptionType() == DbException.Type.OBJECT_NOT_FOUND)
            {
                mav.addObject("mapperResult", new JSONArray().toString());
            } else {
                mav.addObject("mapperResult", makeErrorsJson(e));
            }

        } catch (JSONException e) {
            mav.addObject("mapperResult", makeErrorsJson(e));
        } catch (IOException e) {
            mav.addObject("mapperResult", makeErrorsJson(e));
        } 
        return mav;
    }
	
	@RequestMapping(value = "/user/delete/{id}", method = RequestMethod.GET)
    public ModelAndView deleteUserPlace(@PathVariable String id, String callback, Model m, HttpServletRequest req){
        ModelAndView mav = null;
        if(StringUtils.isEmpty(callback))
            mav = new ModelAndView("mapper-result");
        else {
            mav = new ModelAndView("jsonp-mapper-result");
            mav.addObject(
                    "callback", 
                    callback);
        }
        
        try {
            JSONObject  publisher =  checkPublisherKey(req);

            
            jsonDatabase.delete(publisherPlacesCollection, id);
            
            Map<String, Object> result = new HashMap<String,Object>();
            result.put("id", id);
            result.put("status", "SUCCEED");
            
            mav.addObject("mapperResult", makeModelJson(result));
            
        } catch (DbException e) {
            logger.error("could not get results");
            if(e.getExceptionType() == DbException.Type.OBJECT_NOT_FOUND)
            {
                mav.addObject("mapperResult", new JSONArray().toString());
            } else {
                mav.addObject("mapperResult", makeErrorsJson(e));
            }

        } catch (Exception e) {
            mav.addObject("mapperResult", makeErrorsJson(e));
        } 
        
        return mav;
    }
	
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public ModelAndView get(@PathVariable String id, @RequestParam(required=false) String callback, Model m){
	    ModelAndView mav = null;
        if(StringUtils.isEmpty(callback))
            mav = new ModelAndView("mapper-result");
        else {
            mav = new ModelAndView("jsonp-mapper-result");
            mav.addObject(
                    "callback", 
                    callback);
        }
		
		try {
			JSONArray places = new JSONArray();
			JSONObject place = jsonDatabase.findById(placesCollection, id);
			places.put(place);
			mav.addObject("mapperResult", places.toString());
		} catch (DbException e) {
			logger.error("could not get results");
			if(e.getExceptionType() == DbException.Type.OBJECT_NOT_FOUND)
			{
				mav.addObject("mapperResult", new JSONArray().toString());
			} else {
				mav.addObject("mapperResult", makeErrorsJson(e));
			}

		} 
		return mav;
	}
	
	@RequestMapping(value = "/publisher/{id}", method = RequestMethod.GET)
    public ModelAndView getPublisherPlace(@PathVariable String id, @RequestParam(required=false) String callback, Model m){
        ModelAndView mav = null;
        if(StringUtils.isEmpty(callback))
            mav = new ModelAndView("mapper-result");
        else {
            mav = new ModelAndView("jsonp-mapper-result");
            mav.addObject(
                    "callback", 
                    callback);
        }
        
        try {
            JSONArray places = new JSONArray();
            JSONObject place = jsonDatabase.findById(publisherPlacesCollection, id);
            places.put(place);
            mav.addObject("mapperResult", places.toString());
        } catch (DbException e) {
            logger.error("could not get results");
            if(e.getExceptionType() == DbException.Type.OBJECT_NOT_FOUND)
            {
                mav.addObject("mapperResult", new JSONArray().toString());
            } else {
                mav.addObject("mapperResult", makeErrorsJson(e));
            }

        } 
        return mav;
    }
	
	@RequestMapping(value = "/publisher", method = RequestMethod.GET)
	public ModelAndView getPublisherPlaces(@RequestParam String key, @RequestParam(required=false) String callback, Model m){
	    ModelAndView mav = null;
        if(StringUtils.isEmpty(callback))
            mav = new ModelAndView("mapper-result");
        else {
            mav = new ModelAndView("jsonp-mapper-result");
            mav.addObject(
                    "callback", 
                    callback);
        }
        
        try {
            JSONArray places = jsonDatabase.findUserPlaces(key, publisherPlacesCollection, "published");
                       
            mav.addObject("mapperResult", places.toString());
            
        } catch (DbException e) {
            logger.error("could not get results");
            if(e.getExceptionType() == DbException.Type.OBJECT_NOT_FOUND)
            {
                mav.addObject("mapperResult", new JSONArray().toString());
            } else {
                mav.addObject("mapperResult", makeErrorsJson(e));
            }

        } 
        

        
	    return mav;	    
	}

	
	
	@RequestMapping(value = "/search", method = RequestMethod.GET)
	public ModelAndView search(@RequestParam String q, @RequestParam String loc,  
			@RequestParam Double radiusKm, @RequestParam(required=false) String callback, HttpServletRequest req){
		
	    ModelAndView mav = null;
	    if(StringUtils.isEmpty(callback))
	        mav = new ModelAndView("mapper-result");
	    else {
	        mav = new ModelAndView("jsonp-mapper-result");
	        mav.addObject(
                    "callback", 
                    callback);
	    }
        	    
		try {
			String[] parts = loc.split("_");
			
			Point p = new Point(Double.parseDouble(parts[0]),Double.parseDouble(parts[1]));

			JSONArray resultIds = 
				searchService.find(p, radiusKm, q, 0, 25, searchEndpoint);
			JSONArray results = new JSONArray();
			//get items from db 
			for (int i = 0; i < resultIds.length(); i++) {
                JSONObject id = resultIds.getJSONObject(i);
                JSONObject place = jsonDatabase.findById(placesCollection, id.getString("id"));
                
                if(place != null){
                	place.put("distance", id.getDouble("_dist_"));
                	results.put(place);
                }
            }
							
			mav.addObject(
					"mapperResult", 
					results.toString());
					
		} 
		catch (Exception e) {
			logger.error("could not get results",e);
			mav.addObject("mapperResult", makeErrorsJson(e));
		}	
			
		return mav;
	}

	public void setJsonDatabase(JsonDatabase jsonDatabase) {
		this.jsonDatabase = jsonDatabase;
	}

	public void setSearchService(SpatialSearchService searchService) {
		this.searchService = searchService;
	}

	public void setIdGen(IdGen idGen) {
		this.idGen = idGen;
	}

	public void setSpatialConversionUtils(
			SpatialConversionUtils spatialConversionUtils) {
		this.spatialConversionUtils = spatialConversionUtils;
	}
	


}
