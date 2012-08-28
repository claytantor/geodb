package com.welocally.geodb.web.mvc;

import static org.elasticsearch.index.query.FilterBuilders.geoDistanceFilter;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
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
import com.welocally.geodb.services.util.WelocallyJSONUtils;

@Controller
@RequestMapping("/place/es")
public class ESPlaceController extends AbstractJsonController {
	
	static Logger logger = 
		Logger.getLogger(ESPlaceController.class);
		
	@Autowired 
	@Qualifier("dynamoJsonDatabase")
	JsonDatabase jsonDatabase;
	
	
	@Value("${placesDatabase.collectionName:dev.places.published}")
	String placesCollection;
	
	@Value("${placesDatabase.collectionName:dev.places.review}")
    String placesReviewCollection;
		    
    @Value("${publisherDatabase.collectionName:dev.publishers}")
    String publisherCollection;
    
    @Value("${userPlacesDatabase.collectionName:dev.places.user}")
    String publisherPlacesCollection;
    
    @Value("${ElasticSearch.transportClient.server:localhost}")
    String elasticSearchTransportServer;
    
    @Value("${ElasticSearch.transportClient.port:9300}")
    Integer elasticSearchTransportPort;
    
    @Value("${ElasticSearch.transportClient.clusterName:es-welocally-dev}")
    String elasticSearchTransportClusterName;
    
    @Value("${geodb.admin.username}")
    String adminUser;
    
    @Value("${geodb.admin.password}")
    String adminPassword;
    
      	
	@Autowired IdGen idGen; 
	
	@Autowired SpatialConversionUtils spatialConversionUtils; 
	
	@Autowired WelocallyJSONUtils welocallyJSONUtils;
	
	TransportClient transportClient = null;
	
	
	@PostConstruct
	public void initClient(){
	    Settings settings = ImmutableSettings.settingsBuilder()
           .put("cluster.name", elasticSearchTransportClusterName).build();
       transportClient = new TransportClient(settings);
       transportClient.addTransportAddress(
               new InetSocketTransportAddress(
                       elasticSearchTransportServer, 
                       elasticSearchTransportPort));
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
	
	
	@RequestMapping(value = "/search", method = RequestMethod.GET)
	public ModelAndView searchPublicIndex(@RequestParam String q, @RequestParam String loc,  
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

			QueryBuilder searchQuery = filteredQuery(
					termQuery("search", q.toLowerCase()),
					geoDistanceFilter("location")
					.point(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]))
					.distance(radiusKm, DistanceUnit.KILOMETERS));
			
			SearchResponse response = transportClient.prepareSearch("geodb").setTypes("place").
			setQuery(searchQuery).execute().actionGet();

			
			JSONArray results = new JSONArray();
			//get items from db 
			for (SearchHit hit: response.getHits()) {
                String id = hit.getId();
                JSONObject place = jsonDatabase.findById(placesCollection, id);
                
                if(place != null){
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
	
	
	
	@RequestMapping(value = "/public/save", method = RequestMethod.PUT)
    public ModelAndView savePlacePublicPut(@RequestBody String requestJson, HttpServletRequest req){  
        
        ModelAndView  mav = new ModelAndView("mapper-result");
              
        try {
            
            //right now we are going to secure this with the claytantor account
            JSONObject  admin =  checkPublisherKey(req);        
            
            if(admin.getString("username").equals(adminUser) && 
                    admin.getString("password").equals(adminPassword) )
            {
                JSONObject place = 
                    new JSONObject(requestJson);
                
                savePlacePublic(place, mav, req);
                 
                 
            } else {
                throw new RuntimeException("user not authorized for this activity");
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
               
    }
	
    @RequestMapping(value = "/public/saveall", method = RequestMethod.PUT)
    public ModelAndView savePlacesPublicPut(@RequestBody String requestJson, HttpServletRequest req){  
        
        ModelAndView  mav = new ModelAndView("mapper-result");
              
        try {
            
            //right now we are going to secure this with the claytantor account
            JSONObject  admin =  checkPublisherKey(req);        
            
            if(admin.getString("username").equals(adminUser) && 
                    admin.getString("password").equals(adminPassword) )
            {
                JSONArray places = 
                    new JSONArray(requestJson);
                               
                for (int i = 0; i < places.length(); i++) {
                    savePlacePublic(places.getJSONObject(i), mav, req);
                }   
                
                //override response
                Map<String, Object> result = new HashMap<String,Object>();
                result.put("size", places.length());
                result.put("action", "PUT");
                result.put("type", "places");
                result.put("status", "SUCCEED");
                
                mav.addObject("mapperResult", makeModelJson(result));
                 
            } else {
                throw new RuntimeException("user not authorized for this activity");
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
               
    }
    
//  curl -XPOST localhost:9200/user52ae/place/_mapping -d '{
//  "place": {
//      "properties": {
//          "location": {
//              "type": "geo_point"
//          },
//          "search": {
//              "type": "string",
//              "store": "yes"
//          }
//      }
//  }
//  }'
    
    @RequestMapping(value = "/user/create", method = RequestMethod.PUT)
    public ModelAndView createUser(@RequestBody String requestJson, HttpServletRequest req) {
        
        ModelAndView  mav = new ModelAndView("mapper-result");
      
        try {
            
            //right now we are going to secure this with the claytantor account
            JSONObject  publisher =  checkPublisherKey(req); 
            
            if(publisher.getString("username").equals(adminUser) && 
                    publisher.getString("password").equals(adminPassword) )
            {
                JSONObject user = 
                    new JSONObject(requestJson);
                
                //put it in the user store
                jsonDatabase.put(user, null,publisherCollection, user.getString("username"), JsonDatabase.EntityType.PUBLISHER, StatusType.PUBLISHED);
                
                String mapping = "{" +
                    "\"place\": {" +
                		"\"properties\":{" +
                		    "\"location\":{ " +
                		        "\"type\": \"geo_point\"" +
                		      "}," +
                		      "\"search\": {" +
                		         "\"type\": \"string\"," +
                		         "\"store\": \"yes\"" +
                		        "}" +
                		    "}" +
                		"}" +
                		"}";     
                
                CreateIndexResponse response = 
                    transportClient.admin().indices().create( 
                                        new CreateIndexRequest(user.getString("username")). 
                                                mapping("place", mapping) 
                                ).actionGet(); 
                    
                Map<String, Object> result = new HashMap<String,Object>();
                result.put("acknowledged", response.acknowledged());
                result.put("status", "SUCCEED");
                
                mav.addObject("mapperResult", makeModelJson(result));
                 
            } else {
                throw new RuntimeException("user not authorized for this activity");
            }
                       
        } catch (JSONException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } catch (RuntimeException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } catch (DbException e) {
            logger.error("could not get results");
            mav.addObject("mapperResult", makeErrorsJson(e));
        } 
        
        return mav;
   
    }

	
	@RequestMapping(value = "/user/save", method = RequestMethod.PUT)
    public ModelAndView savePlaceUserPut(@RequestBody String requestJson, HttpServletRequest req){  
                               
        ModelAndView  mav = new ModelAndView("mapper-result");
              
        try {
            JSONObject  publisher = checkPublisherKey(req);  
            
            JSONObject place = 
                new JSONObject(requestJson);
            
            savePlaceToUserStore(place, publisher, mav, req);
            
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
	
	@RequestMapping(value = "/user/search", method = RequestMethod.GET)
    public ModelAndView searchUserIndex(@RequestParam String q, @RequestParam String loc,  
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
            
            JSONObject publisher = checkPublisherKey(req);  
            
            String[] parts = loc.split("_");

            QueryBuilder searchQuery = filteredQuery(
                    termQuery("search", q.toLowerCase()),
                    geoDistanceFilter("location")
                    .point(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]))
                    .distance(radiusKm, DistanceUnit.KILOMETERS));
            
            SearchResponse response = transportClient.prepareSearch(publisher.getString("username")).setTypes("place").
            setQuery(searchQuery).execute().actionGet();
            
            JSONArray results = new JSONArray();
            //get items from db 
            for (SearchHit hit: response.getHits()) {
                String id = hit.getId();
                JSONObject place = jsonDatabase.findById(this.publisherPlacesCollection, id);
                
                if(place != null){
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
	
//----------- private implementations ------------//	
   private void savePlacePublic(JSONObject place, ModelAndView mav, HttpServletRequest req) throws DbException, JSONException, IOException, RuntimeException{      
       JSONObject  publisher =  checkPublisherKey(req);
                      
        
        Point p = spatialConversionUtils.getJSONPoint(place);
        
        if(place.isNull("if")){
            place.put("if", idGen.genPoint("WL_",p));
        }
        
        String id= place.getString("if");
              
        if(p != null && publisher != null){      
            place.put("if", id);
            place.getJSONObject("properties").put("owner", "welocally");  
            
            //put it in the public store
            jsonDatabase.put(place, null,placesCollection, id, JsonDatabase.EntityType.PLACE, StatusType.PUBLISHED);
            
         	//put it in the public index            
            JSONObject placeIndex = welocallyJSONUtils.makeIndexablePlace(place);
            IndexResponse response = transportClient.prepareIndex("geodb", "place", id)
            .setSource(XContentFactory.jsonBuilder()
                        .startObject()
                        .field("search", placeIndex.get("search"))
                        .startArray("location")
                        .value(placeIndex.get("location_1_coordinate"))
                        .value(placeIndex.get("location_0_coordinate"))
                        .endArray()
                        .endObject())
            .execute()
            .actionGet();
                           
            Map<String, Object> result = new HashMap<String,Object>();
            result.put("id", response.id());
            result.put("type", response.getType());
            result.put("status", "SUCCEED");
            
            mav.addObject("mapperResult", makeModelJson(result));
        } 
    }
	
	/**
	 * 
	 * this meathod is for the use case of a user saving or updating a place in their
	 * places. if they mark it for public it will be added to the review collection
	 * 
	 * @param place
	 * @param mav
	 * @param req
	 * @throws DbException
	 * @throws JSONException
	 * @throws IOException
	 * @throws RuntimeException
	 */
	private void savePlaceToUserStore(JSONObject place, JSONObject  publisher, ModelAndView mav, HttpServletRequest req) throws DbException, JSONException, IOException, RuntimeException{      
        
        Point p = spatialConversionUtils.getJSONPoint(place);
        
        if(place.isNull("if")){
            place.put("if", idGen.genPoint("WL_",p));
        }
        
        String id= place.getString("if");
              
        if(p != null && publisher != null){      
           
            //we need something that will see
            if(!place.isNull("public") && place.getBoolean("public")){
                place.getJSONObject("properties").put("owner", "welocally");   
                jsonDatabase.put(place, null, placesReviewCollection, id, JsonDatabase.EntityType.PLACE, StatusType.PUBLISHED);
            }
            
            //make a compound document
            JSONObject userPlaceDataDocument = new JSONObject();
            //userPlace
            JSONObject userPlace = new JSONObject(place.toString());
            userPlace.getJSONObject("properties").put("owner", publisher.getString("username"));
            userPlaceDataDocument.put("userPlace", userPlace);
            
            //user data
            JSONObject userData = new JSONObject("{\"data\":[]}");
            userPlaceDataDocument.put("userData", userData);
                      
            //make it indexable
            JSONObject userPlaceIndex = welocallyJSONUtils.makeIndexableUserData(userPlace, userData);
                       
            jsonDatabase.put(userPlaceDataDocument, null, publisherPlacesCollection, id, JsonDatabase.EntityType.USER_PLACE, StatusType.PUBLISHED);
                             
            IndexResponse response = transportClient.prepareIndex(publisher.getString("username"), "place", id)
            .setSource(XContentFactory.jsonBuilder()
                        .startObject()
                        .field("search", userPlaceIndex.get("search"))
                        .startArray("location")
                        .value(userPlaceIndex.get("location_1_coordinate"))
                        .value(userPlaceIndex.get("location_0_coordinate"))
                        .endArray()
                        .endObject())
            .execute()
            .actionGet();
                           
            Map<String, Object> result = new HashMap<String,Object>();
            result.put("id", response.id());
            result.put("type", response.getType());
            result.put("status", "SUCCEED");
            
            mav.addObject("mapperResult", makeModelJson(result));
        } 
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
	
	

    
    

	public void setJsonDatabase(JsonDatabase jsonDatabase) {
		this.jsonDatabase = jsonDatabase;
	}

	public void setIdGen(IdGen idGen) {
		this.idGen = idGen;
	}

	public void setSpatialConversionUtils(
			SpatialConversionUtils spatialConversionUtils) {
		this.spatialConversionUtils = spatialConversionUtils;
	}
	
	


}
