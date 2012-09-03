package com.welocally.geodb.web.mvc;

import static org.elasticsearch.index.query.FilterBuilders.geoDistanceFilter;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.IndicesExistsResponse;
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
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.GeoDistanceSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
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
    @RequestMapping(value = "/publish", method = RequestMethod.POST)
    public ModelAndView publish(@RequestBody String requestJson, HttpServletRequest req) {
               
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
        CreateIndexResponse responseCreate=null;
        IndicesExistsResponse responseExists=null;
        Boolean ack = false;
        try {
            JSONObject model = 
                new JSONObject(requestJson);
            
            //try to find the index
            responseExists = transportClient.admin().indices().exists(
                    new IndicesExistsRequest(model.getString("indexId"))                   
                ).actionGet();
            
            
            
            
            //if it doesnt exist create it
            if(!responseExists.exists()){
                responseCreate = transportClient.admin().indices().create( 
                        new CreateIndexRequest(model.getString("indexId")). 
                                mapping("record", mapping) 
                ).actionGet();
                ack = responseCreate.acknowledged();
            } else {
                ack = true;
            }
                                 
            writeRecordsToIndex(
                    model.getString("indexId"), 
                    model.getJSONObject("data"), 
                    model.getJSONObject("schema"));
            
            //now write records to the store
            writeRecordsToStore(
                    model.getString("indexId"),
                    model.getJSONObject("data"), 
                    model.getJSONObject("schema"));
                       
            result.put("acknowledged", ack);
            result.put("status", "SUCCEED");
            result.put("action", "create");
            result.put("indexId", model.getString("indexId"));
            
            
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
                  String recordid = indexId+"-"+new String(Base64.encodeBase64(record.getString("id").toString().getBytes()));
                  Point p = 
                      spatialConversionUtils.getJSONPoint(records.getJSONObject(i));
                                   
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
    
    protected void writeRecordsToStore(String indexId, JSONObject data, JSONObject schema) throws JSONException{
        
        JSONArray records = data.getJSONArray("records");
        Map<String, Object> result = new HashMap<String,Object>();
        try {
              for (int i = 0; i < records.length(); i++) {
                  
                  JSONObject record = records.getJSONObject(i); 
                  writeRecordToStore( indexId, record, schema, recordsCollection);
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
    
    protected void writeRecordToStore(String indexId,JSONObject record,JSONObject schema, String storeCollectionName) throws DbException {
        try {                  
            String recordid = indexId+"-"+new String(Base64.encodeBase64(record.getString("id").toString().getBytes()));
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
                    
          
          response = transportClient.admin().indices().delete( 
                              new DeleteIndexRequest(model.getString("indexId"))).actionGet();
          
          result.put("acknowledged", response.acknowledged());
          result.put("status", "SUCCEED");
          result.put("action", "delete");
          result.put("indexId", model.getString("indexId"));
          
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

  @RequestMapping(value = "/search", method = RequestMethod.GET)
  public ModelAndView search(
          @RequestParam String indexId,
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

          double lat = Double.parseDouble(parts[0]);
          double lon = Double.parseDouble(parts[1]);
          SortBuilder sort = SortBuilders.geoDistanceSort("location").point(lat, lon);
          
          QueryBuilder searchQuery = filteredQuery(
                  termQuery("search", q.toLowerCase()),
                  geoDistanceFilter("location")                  
                  .point(lat, lon)
                  .distance(radiusKm, DistanceUnit.KILOMETERS));
        
			SearchResponse response = transportClient.prepareSearch(indexId)
					.setTypes("record")
					.addScriptField("distance", "doc['location'].arcDistanceInKm(" + lat+","+lon +")")
					.setQuery(searchQuery)
					.addSort(sort)
					.execute().actionGet();
          
          JSONArray results = new JSONArray();
          for (SearchHit hit: response.getHits()) {
              String id = hit.getId();
              JSONObject record = jsonDatabase.findById(recordsCollection, id);
              
				if (record != null) {
					Double distance = (Double) hit.getFields().get("distance")
							.getValue();
					record.put("geoDistance", distance);
					results.put(record);
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
 

}
