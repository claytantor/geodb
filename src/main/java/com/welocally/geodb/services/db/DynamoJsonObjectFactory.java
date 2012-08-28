package com.welocally.geodb.services.db;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.welocally.geodb.services.spatial.SpatialDocumentFactory;

@Component
public class DynamoJsonObjectFactory {
	
	static Logger logger = 
		Logger.getLogger(DynamoJsonObjectFactory.class);
	
	@Autowired SpatialDocumentFactory spatialDocumentFactory;
	
	
	public Map<String, AttributeValue> makeRecord(JSONObject recordObject, JSONObject schema, String id, String status) throws JSONException{
		
		Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
		
		JSONArray properties = recordObject.getJSONArray("properties");
		JSONObject geom = recordObject.getJSONObject("geometry");
		JSONArray coords = geom.getJSONArray("coordinates");
		
		item.put("id", new AttributeValue(id));
		item.put("lat", new AttributeValue().withN(coords.getString(1)));
		item.put("lng", new AttributeValue().withN(coords.getString(0)));
		item.put("search", new AttributeValue(spatialDocumentFactory.makeSearchableContent(properties, schema)));
		item.put("status", new AttributeValue(status));
		item.put("document", new AttributeValue(recordObject.toString()));
		
        return item;
	}
	
	public Map<String, AttributeValue> makeUserPlace(JSONObject placeObject, JSONObject userData, String status) throws JSONException{
        
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        
        JSONObject properties = placeObject.getJSONObject("properties");
        JSONObject geom = placeObject.getJSONObject("geometry");
        JSONArray coords = geom.getJSONArray("coordinates");
        
        item.put("if", new AttributeValue(placeObject.getString("if")));
        item.put("lat", new AttributeValue().withN(coords.getString(1)));
        item.put("lng", new AttributeValue().withN(coords.getString(0)));
        item.put("search", new AttributeValue(spatialDocumentFactory.makeSearchableUserDataContent(properties, userData)));
        item.put("status", new AttributeValue(status));
        item.put("owner", new AttributeValue(placeObject.getJSONObject("properties").getString("owner")));
        item.put("document", new AttributeValue(placeObject.toString()));
        item.put("data", new AttributeValue(userData.toString()));
        
        return item;
    }
	
	public Map<String, AttributeValue> makeClassifier(JSONObject jsonObject) throws JSONException{
		Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
		item.put("if", new AttributeValue(jsonObject.getString("if")));
		item.put("type", new AttributeValue(jsonObject.getString("type")));
		item.put("category", new AttributeValue(jsonObject.getString("category")));
		if(!jsonObject.getString("subcategory").isEmpty())
			item.put("subcategory", new AttributeValue(jsonObject.getString("subcategory")));
		else
			item.put("subcategory", new AttributeValue(jsonObject.getString("category")));		
		
		return item;
	}
	
	public Map<String, AttributeValue> makeDeal(JSONObject jsonObject, String status) throws JSONException{
	    Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        
        JSONObject location = jsonObject.getJSONObject("location");
        
        JSONArray coordsNew =new JSONArray();
        coordsNew.put(new Double(location.getDouble("latitude")).toString());
        coordsNew.put(new Double(location.getDouble("longitude")).toString());

        
        item.put("if", new AttributeValue(jsonObject.getString("if")));
        item.put("lat", new AttributeValue().withN(new Double(location.getDouble("latitude")).toString()));
        item.put("lng", new AttributeValue().withN(new Double(location.getDouble("longitude")).toString()));
        item.put("search", new AttributeValue(spatialDocumentFactory.makeSearchableDealContent(jsonObject)));
        item.put("status", new AttributeValue(status));
        item.put("document", new AttributeValue(jsonObject.toString()));
        
        return item;
    }
	
	/*
	 * {
    "name": "dff3794a01ef",
    "password": "5f4dcc3b5aa765d61d8327deb882cf99",
    "username": "dff3794a01ef",
    "email": null,
    "enabled": false,
    "principal": "dff3794a01ef",
    "userClass": null,
    "authorities": [],
    "accountNonExpired": true,
    "accountNonLocked": true,
    "credentialsNonExpired": true,
    "details": {
        "password": "5f4dcc3b5aa765d61d8327deb882cf99",
        "username": "dff3794a01ef",
        "enabled": false,
        "authorities": [],
        "accountNonExpired": true,
        "accountNonLocked": true,
        "credentialsNonExpired": false
    },
    "credentials": "5f4dcc3b5aa765d61d8327deb882cf99",
    "credentialsExpired": false,
    "locked": false,
    "twitterToken": null,
    "authGuid": null,
    "twitterId": null,
    "twitterUsername": null,
    "twitterProfileImg": null,
    "twitterVerify": null,
    "twitterSecret": null,
    "id": 71,
    "version": 0,
    "timeCreated": 1322771497262,
    "timeUpdated": 1322771497262
}
	 */
	public Map<String, AttributeValue> makePublisher(JSONObject jsonObject, String status) throws JSONException{
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("if", new AttributeValue(jsonObject.getString("name")));
        item.put("password", new AttributeValue(jsonObject.getString("password")));
        item.put("status", new AttributeValue(status));
        item.put("document", new AttributeValue(jsonObject.toString()));       
        return item;
    }

}
