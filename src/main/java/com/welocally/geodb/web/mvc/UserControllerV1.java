package com.welocally.geodb.web.mvc;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.welocally.geodb.services.db.DbException;
import com.welocally.geodb.services.db.IdGen;
import com.welocally.geodb.services.db.JsonDatabase;
import com.welocally.geodb.services.db.JsonDatabase.StatusType;
import com.welocally.geodb.services.spatial.SpatialConversionUtils;
import com.welocally.geodb.services.util.WelocallyJSONUtils;

@Controller
@RequestMapping("/user/1_0")
public class UserControllerV1 extends AbstractJsonController {
    
    static Logger logger = 
        Logger.getLogger(ESPlaceController.class);
        
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
        Settings settings = ImmutableSettings.settingsBuilder()
           .put("cluster.name", elasticSearchTransportClusterName).build();
       transportClient = new TransportClient(settings);
       transportClient.addTransportAddress(
               new InetSocketTransportAddress(
                       elasticSearchTransportServer, 
                       elasticSearchTransportPort));
    }
      
    @RequestMapping(value = "/exists/{id}", method = RequestMethod.GET)
    public ModelAndView findById(@PathVariable String id, HttpServletRequest req) {
        
        ModelAndView  mav = new ModelAndView("mapper-result");
      
        try {
            
            //right now we are going to secure this with the claytantor account
            JSONObject requestUser =  checkPublisherKey(req); 
            
            if(requestUser.getString("username").equals(adminUser) && 
                    requestUser.getString("password").equals(adminPassword) )
            {
                JSONObject user = jsonDatabase.findById(usersCollection, id);
                
                Map<String, Object> result = new HashMap<String,Object>();
                result.put("id", user.get("name"));
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
    
    @RequestMapping(value = "/create", method = RequestMethod.PUT)
    public ModelAndView createUser(@RequestBody String requestJson, HttpServletRequest req) {
        
        ModelAndView  mav = new ModelAndView("mapper-result");
      
        try {
            
            //right now we are going to secure this with the claytantor account
            JSONObject requestUser =  checkPublisherKey(req); 
            
            if(requestUser.getString("username").equals(adminUser) && 
                    requestUser.getString("password").equals(adminPassword) )
            {
                JSONObject user = 
                    new JSONObject(requestJson);
                
                createUser( user,  mav);
                
                 
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
    
    
    private void createUser(JSONObject user,ModelAndView  mav) throws DbException, JSONException{
      //put it in the user store
        jsonDatabase.put(user, usersCollection, user.getString("username"), JsonDatabase.EntityType.PUBLISHER, StatusType.PUBLISHED);
        
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
        
        Map<String, Object> result = new HashMap<String,Object>();
        CreateIndexResponse response=null;
        try {
            response = transportClient.admin().indices().create( 
                                new CreateIndexRequest(user.getString("username")). 
                                        mapping("place", mapping) 
                        ).actionGet();
            result.put("acknowledged", response.acknowledged());
            result.put("status", "SUCCEED");
            
        } catch (ElasticSearchException e) {
            
            logger.error("problem with create index", e);
            result.put("message", e.getMessage());
            result.put("acknowledged", false);
            result.put("status", "FAIL");
        } 
            
        
        
        
        mav.addObject("mapperResult", makeModelJson(result));
    }
     
    private JSONObject checkPublisherKey(HttpServletRequest req){
        if(req.getHeader("key") != null){   
            String publisherKey = req.getHeader("key");
                      
            try {
                return jsonDatabase.findById(usersCollection, publisherKey);
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