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
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.welocally.geodb.services.db.IdGen;
import com.welocally.geodb.services.db.JsonDatabase;
import com.welocally.geodb.services.spatial.Point;
import com.welocally.geodb.services.spatial.SpatialConversionUtils;
import com.welocally.geodb.services.util.WelocallyJSONUtils;

@Controller
@RequestMapping("/geogrid/1_0")
public class GeoGridControllerV1 extends AbstractJsonController {
    
    static Logger logger = 
        Logger.getLogger(GeoGridControllerV1.class);
    
    @Autowired 
    @Qualifier("dynamoJsonDatabase")
    JsonDatabase jsonDatabase;
    
    
    @Value("${placesDatabase.collectionName:dev.places.published}")
    String placesCollection;
    
    @Value("${placesDatabase.collectionName:dev.places.review}")
    String placesReviewCollection;
            
    @Value("${users.collectionName:dev.users}")
    String usersCollection;

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
        logger.debug("init GeoGridControllerV1");
        Settings settings = ImmutableSettings.settingsBuilder()
           .put("cluster.name", elasticSearchTransportClusterName).build();
       transportClient = new TransportClient(settings);
       transportClient.addTransportAddress(
               new InetSocketTransportAddress(
                       elasticSearchTransportServer, 
                       elasticSearchTransportPort));
    }
    
    //creates an empty index
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public ModelAndView create(@RequestBody String requestJson, HttpServletRequest req) {
               
        //put it in the user store
        ModelAndView  mav = new ModelAndView("mapper-result");
              
        String mapping = "{" +
            "\"row\": {" +
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
        
        Map<String, Object> result = new HashMap<String,Object>();
        CreateIndexResponse response=null;
        try {
            JSONObject grid = 
                new JSONObject(requestJson);
            
            response = transportClient.admin().indices().create( 
                                new CreateIndexRequest(grid.getString("key")). 
                                        mapping("row", mapping) 
                        ).actionGet();
            
            result.put("acknowledged", response.acknowledged());
            result.put("status", "SUCCEED");
            
        } catch (ElasticSearchException e) {           
            logger.error("problem with create index", e);
            result.put("message", e.getMessage());
            result.put("acknowledged", false);
            result.put("status", "FAIL");
        } catch (JSONException e) {
            logger.error("problem with create index", e);
            result.put("message", e.getMessage());
            result.put("acknowledged", false);
            result.put("status", "FAIL");
        } 
                   
        
        mav.addObject("mapperResult", makeModelJson(result));
        
        return mav;
    }
    
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public ModelAndView update(
            @RequestBody String requestJson, 
            HttpServletRequest req) {
        
        ModelAndView  mav = new ModelAndView("mapper-result");
        
        Map<String, Object> result = new HashMap<String,Object>();
        try {
                      
            JSONObject grid = 
                new JSONObject(requestJson);
            
            //for each row
            JSONArray rows = grid.getJSONArray("rows");
            for (int i = 0; i < rows.length(); i++) {
                
                JSONObject row = rows.getJSONObject(i); 
                
                Point p = 
                    spatialConversionUtils.getJSONPoint(rows.getJSONObject(i));
                
                if(row.isNull("id")){
                    row.put("id", idGen.genPoint("WL_",p));
                }
                
                String id= row.getString("id");
                      
                if(p != null ){      
                     //make a compound document
                    JSONObject userPlaceDataDocument = new JSONObject();
                    userPlaceDataDocument.put("row", row);
                    
                    //user data OBSOLETE!
                    JSONObject userData = new JSONObject("{\"data\":[]}");
                    userPlaceDataDocument.put("userData", userData);
                              
                    //make it indexable
                    JSONObject userPlaceIndex = welocallyJSONUtils.makeIndexableUserData(row, userData);
                                                                  
                    IndexResponse response = transportClient.prepareIndex(grid.getString("key"), "row", id)
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
                    
                }                
            }           
            result.put("status", "SUCCEED");
            
        } catch (ElasticSearchException e) {           
            logger.error("problem with create index", e);
            result.put("message", e.getMessage());
            result.put("acknowledged", false);
            result.put("status", "FAIL");
        } catch (JSONException e) {
            logger.error("problem with create index", e);
            result.put("message", e.getMessage());
            result.put("acknowledged", false);
            result.put("status", "FAIL");
        } catch (IOException e) {
            logger.error("problem with create index", e);
            result.put("message", e.getMessage());
            result.put("acknowledged", false);
            result.put("status", "FAIL");
        } 
                   
        
        mav.addObject("mapperResult", makeModelJson(result));
        
        
        return mav;
    }
    
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ModelAndView delete(@RequestBody String requestJson, HttpServletRequest req) {
        //put it in the user store
        ModelAndView  mav = new ModelAndView("mapper-result"); 
        
        Map<String, Object> result = new HashMap<String,Object>();
        DeleteIndexResponse response=null;
        try {
            JSONObject grid = 
                new JSONObject(requestJson);
            
            response = transportClient.admin().indices().delete( 
                                new DeleteIndexRequest(grid.getString("key"))).actionGet();
            
            result.put("acknowledged", response.acknowledged());
            result.put("status", "SUCCEED");
            
        } catch (ElasticSearchException e) {           
            logger.error("problem with create index", e);
            result.put("message", e.getMessage());
            result.put("acknowledged", false);
            result.put("status", "FAIL");
        } catch (JSONException e) {
            logger.error("problem with create index", e);
            result.put("message", e.getMessage());
            result.put("acknowledged", false);
            result.put("status", "FAIL");
        } 
                          
        mav.addObject("mapperResult", makeModelJson(result));
        
        return mav;
    }
    
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public ModelAndView search(
            @RequestParam String key,
            @RequestParam String q, 
            @RequestParam String loc,  
            @RequestParam Double radiusKm, 
            @RequestParam(required=false) String callback, 
            HttpServletRequest req){
        
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
            
            SearchResponse response = transportClient.prepareSearch(key).setTypes("row").
            setQuery(searchQuery).execute().actionGet();
            
            JSONArray results = new JSONArray();
            for (SearchHit hit: response.getHits()) {
                String id = hit.getId();
                results.put(id);
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

}
