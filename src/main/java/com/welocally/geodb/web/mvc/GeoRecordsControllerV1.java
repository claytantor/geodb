package com.welocally.geodb.web.mvc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.indices.IndexMissingException;
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
import com.welocally.geodb.services.spatial.Point;
import com.welocally.geodb.services.spatial.SpatialConversionUtils;
import com.welocally.geodb.services.util.WelocallyJSONUtils;

@Controller
@RequestMapping("/georecords/1_0")
public class GeoRecordsControllerV1 extends AbstractJsonController {
    
    static Logger logger = 
        Logger.getLogger(GeoRecordsControllerV1.class);
    
    @Autowired 
    @Qualifier("dynamoJsonDatabase")
    JsonDatabase jsonDatabase;
    
    
    @Value("${records.collectionName:dev.records.published}")
    String recordsCollection;
    
            
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
    
    //creates an empty index
    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public ModelAndView create(@RequestBody String requestJson, HttpServletRequest req) {
               
        //put it in the user store
        ModelAndView  mav = new ModelAndView("mapper-result");
              
        String mapping = "{" +
            "\"record\": {" +
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
            JSONObject model = 
                new JSONObject(requestJson);
            
            String indexId = new String(Base64.encodeBase64(model.getString("indexId").toString().getBytes())).toLowerCase();
            
            response = transportClient.admin().indices().create( 
                                new CreateIndexRequest(indexId). 
                                        mapping("record", mapping) 
                        ).actionGet();
            
            //now write the records to the index
            writeRecordsToIndex(indexId, model.getJSONObject("data"), model.getJSONObject("schema"));
            
            //now write records to the store
            writeRecordsToStore(model.getJSONObject("data"), model.getJSONObject("schema"));
            
            
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
    
    protected void writeRecordsToIndex(String indexId, JSONObject data, JSONObject schema) throws JSONException{
        //for each record
        
        JSONArray records = data.getJSONArray("records");
        Map<String, Object> result = new HashMap<String,Object>();
        try {
              for (int i = 0; i < records.length(); i++) {
                  
                  JSONObject record = records.getJSONObject(i); 
                  String recordid = new String(Base64.encodeBase64(record.getString("id").toString().getBytes()));
                  
                  
                  Point p = 
                      spatialConversionUtils.getJSONPoint(records.getJSONObject(i));
                                   
                  //String recordid = record.getString("id");
                        
                  if(p != null ){      
                       //make it indexable
                      JSONObject recordData = 
                          welocallyJSONUtils.makeIndexableGeoRecord(
                                  record, 
                                  schema);
                                                                    
                      IndexResponse response = 
                          transportClient.prepareIndex(indexId, "record", recordid)
                      .setSource(XContentFactory.jsonBuilder()
                                  .startObject()
                                  .field("search", recordData.get("search"))
                                  .startArray("location")
                                  .value(recordData.get("location_1_coordinate"))
                                  .value(recordData.get("location_0_coordinate"))
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
    }
    
    protected void writeRecordsToStore(JSONObject data, JSONObject schema) throws JSONException{
        
        JSONArray records = data.getJSONArray("records");
        Map<String, Object> result = new HashMap<String,Object>();
        try {
              for (int i = 0; i < records.length(); i++) {
                  
                  JSONObject record = records.getJSONObject(i); 
                  writeRecordToStore( record, schema, recordsCollection);
              }
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
        } catch (DbException e) {
            logger.error("problem with save record", e);
            result.put("message", e.getMessage());
            result.put("acknowledged", false);
            result.put("status", "FAIL");
        }    
        
    }
    
    protected void writeRecordToStore(JSONObject record,JSONObject schema, String storeCollectionName) throws DbException {
        try {                  
            String recordid = new String(Base64.encodeBase64(record.getString("id").toString().getBytes()));           
            logger.debug("adding document:"+recordid);
            jsonDatabase.put(record, schema, storeCollectionName, recordid, JsonDatabase.EntityType.PLACE, StatusType.PUBLISHED);
               
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
    }
    
  @RequestMapping(value = "/delete", method = RequestMethod.POST)
  public ModelAndView delete(@RequestBody String requestJson, HttpServletRequest req) {
      //put it in the user store
      ModelAndView  mav = new ModelAndView("mapper-result"); 
      
      Map<String, Object> result = new HashMap<String,Object>();
      DeleteIndexResponse response=null;
      try {
          JSONObject model = 
              new JSONObject(requestJson);
          
          String indexId = new String(Base64.encodeBase64(model.getString("indexId").toString().getBytes())).toLowerCase();
          
          
          response = transportClient.admin().indices().delete( 
                              new DeleteIndexRequest(indexId)).actionGet();
          
          result.put("acknowledged", response.acknowledged());
          result.put("status", "SUCCEED");
          
      } catch (ElasticSearchException e) {           
          logger.error("problem with delete index", e);
          result.put("message", e.getMessage());
          result.put("acknowledged", false);
          result.put("status", "FAIL");
      } catch (JSONException e) {
          logger.error("problem with create index", e);
          result.put("message", e.getMessage());
          result.put("acknowledged", false);
          result.put("status", "FAIL");
      } catch (Exception e) {
          logger.error("problem with create index", e);
          result.put("message", e.getMessage());
          result.put("acknowledged", false);
          result.put("status", "FAIL");
      } 
                        
      mav.addObject("mapperResult", makeModelJson(result));
      
      return mav;
  }   
    
//    @RequestMapping(value = "/update", method = RequestMethod.POST)
//    public ModelAndView update(
//            @RequestBody String requestJson, 
//            HttpServletRequest req) {
//        
//        ModelAndView  mav = new ModelAndView("mapper-result");
//        
//        Map<String, Object> result = new HashMap<String,Object>();
//        try {
//                      
//            JSONObject grid = 
//                new JSONObject(requestJson);
//            
//            //for each row
//            JSONArray rows = grid.getJSONArray("rows");
//            for (int i = 0; i < rows.length(); i++) {
//                
//                JSONObject row = rows.getJSONObject(i); 
//                
//                Point p = 
//                    spatialConversionUtils.getJSONPoint(rows.getJSONObject(i));
//                
//                if(row.isNull("id")){
//                    row.put("id", idGen.genPoint("WL_",p));
//                }
//                
//                String id= row.getString("id");
//                      
//                if(p != null ){      
//                     //make a compound document
//                    JSONObject userPlaceDataDocument = new JSONObject();
//                    userPlaceDataDocument.put("row", row);
//                    
//                    //user data OBSOLETE!
//                    JSONObject userData = new JSONObject("{\"data\":[]}");
//                    userPlaceDataDocument.put("userData", userData);
//                              
//                    //make it indexable
//                    JSONObject userPlaceIndex = welocallyJSONUtils.makeIndexableUserData(row, userData);
//                                                                  
//                    IndexResponse response = transportClient.prepareIndex(grid.getString("key"), "row", id)
//                    .setSource(XContentFactory.jsonBuilder()
//                                .startObject()
//                                .field("search", userPlaceIndex.get("search"))
//                                .startArray("location")
//                                .value(userPlaceIndex.get("location_1_coordinate"))
//                                .value(userPlaceIndex.get("location_0_coordinate"))
//                                .endArray()
//                                .endObject())
//                    .execute()
//                    .actionGet();
//                    
//                }                
//            }           
//            result.put("status", "SUCCEED");
//            
//        } catch (ElasticSearchException e) {           
//            logger.error("problem with create index", e);
//            result.put("message", e.getMessage());
//            result.put("acknowledged", false);
//            result.put("status", "FAIL");
//        } catch (JSONException e) {
//            logger.error("problem with create index", e);
//            result.put("message", e.getMessage());
//            result.put("acknowledged", false);
//            result.put("status", "FAIL");
//        } catch (IOException e) {
//            logger.error("problem with create index", e);
//            result.put("message", e.getMessage());
//            result.put("acknowledged", false);
//            result.put("status", "FAIL");
//        } 
//                   
//        
//        mav.addObject("mapperResult", makeModelJson(result));
//        
//        
//        return mav;
//    }
//    
//    @RequestMapping(value = "/delete", method = RequestMethod.POST)
//    public ModelAndView delete(@RequestBody String requestJson, HttpServletRequest req) {
//        //put it in the user store
//        ModelAndView  mav = new ModelAndView("mapper-result"); 
//        
//        Map<String, Object> result = new HashMap<String,Object>();
//        DeleteIndexResponse response=null;
//        try {
//            JSONObject grid = 
//                new JSONObject(requestJson);
//            
//            response = transportClient.admin().indices().delete( 
//                                new DeleteIndexRequest(grid.getString("key"))).actionGet();
//            
//            result.put("acknowledged", response.acknowledged());
//            result.put("status", "SUCCEED");
//            
//        } catch (ElasticSearchException e) {           
//            logger.error("problem with create index", e);
//            result.put("message", e.getMessage());
//            result.put("acknowledged", false);
//            result.put("status", "FAIL");
//        } catch (JSONException e) {
//            logger.error("problem with create index", e);
//            result.put("message", e.getMessage());
//            result.put("acknowledged", false);
//            result.put("status", "FAIL");
//        } 
//                          
//        mav.addObject("mapperResult", makeModelJson(result));
//        
//        return mav;
//    }
//    
//    @RequestMapping(value = "/search", method = RequestMethod.GET)
//    public ModelAndView search(
//            @RequestParam String key,
//            @RequestParam String q, 
//            @RequestParam String loc,  
//            @RequestParam Double radiusKm, 
//            @RequestParam(required=false) String callback, 
//            HttpServletRequest req){
//        
//        ModelAndView mav = null;
//        if(StringUtils.isEmpty(callback))
//            mav = new ModelAndView("mapper-result");
//        else {
//            mav = new ModelAndView("jsonp-mapper-result");
//            mav.addObject(
//                    "callback", 
//                    callback);
//        }
//                
//        try {
//                       
//            String[] parts = loc.split("_");
//
//            QueryBuilder searchQuery = filteredQuery(
//                    termQuery("search", q.toLowerCase()),
//                    geoDistanceFilter("location")
//                    .point(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]))
//                    .distance(radiusKm, DistanceUnit.KILOMETERS));
//            
//            SearchResponse response = transportClient.prepareSearch(key).setTypes("row").
//            setQuery(searchQuery).execute().actionGet();
//            
//            JSONArray results = new JSONArray();
//            for (SearchHit hit: response.getHits()) {
//                String id = hit.getId();
//                results.put(id);
//            }
//                            
//            mav.addObject(
//                    "mapperResult", 
//                    results.toString());
//                    
//        } 
//        catch (Exception e) {
//            logger.error("could not get results",e);
//            mav.addObject("mapperResult", makeErrorsJson(e));
//        }   
//            
//        return mav;
//    }   

}
