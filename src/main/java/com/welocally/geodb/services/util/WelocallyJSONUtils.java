package com.welocally.geodb.services.util;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.welocally.geodb.services.db.IdGen;
import com.welocally.geodb.services.spatial.Point;
import com.welocally.geodb.services.spatial.SpatialDocumentFactory;


@Component
public class WelocallyJSONUtils {
	
	@Autowired SpatialDocumentFactory spatialDocumentFactory;
	
	@Autowired IdGen idGen; 
	
	public void updatePlaceToWelocallyFormat(JSONObject place) throws JSONException{
		place.put("id", place.getString("id").replaceAll("SG_", "WL_"));
		place.put("type", "Place");
		place.remove("id");
		JSONObject properties = place.getJSONObject("properties");
		properties.remove("href");
		properties.put("owner", "welocally");
	}
	
	public void updateSignpostDealToWelocally(JSONObject deal) throws JSONException{
	    
	    JSONObject location = deal.getJSONObject("location");
	    
	    Point p = new Point(
	            location.getDouble("latitude"),
	            location.getDouble("longitude"));
	    
        deal.put("id", idGen.genPoint("WLD_",p));

    }
	
	public JSONObject makeIndexablePlace(JSONObject placeObject) throws JSONException{
		
		JSONObject properties = placeObject.getJSONObject("properties");
		JSONObject geom = placeObject.getJSONObject("geometry");
		JSONArray coords = geom.getJSONArray("coordinates");
		Point coord = 
			new Point(
					Double.parseDouble(coords.getString(1)), 
					Double.parseDouble(coords.getString(0)));
		JSONArray coordsNew =new JSONArray();
		coordsNew.put(coords.getString(1));
		coordsNew.put(coords.getString(0));
		
		JSONObject newPlace = new JSONObject();
		newPlace.put("id", placeObject.getString("id"));
		newPlace.put("search", spatialDocumentFactory.makeSearchablePlaceContent(properties));		
		newPlace.put("location_0_coordinate",coord.getLat());
		newPlace.put("location_1_coordinate",coord.getLon());
		
		return newPlace;
	}
	

    public JSONObject makeIndexableGeoRecord(JSONObject record, JSONObject schema) throws JSONException {
        String recordid = new String(Base64.encodeBase64(record.getString("id").toString().getBytes()));
        
        JSONArray properties = record.getJSONArray("properties");
        JSONObject geom = record.getJSONObject("geometry");
        JSONArray coords = geom.getJSONArray("coordinates");
        Point coord = 
            new Point(
                    Double.parseDouble(coords.getString(1)), 
                    Double.parseDouble(coords.getString(0)));
        JSONArray coordsNew =new JSONArray();
        coordsNew.put(coords.getString(1));
        coordsNew.put(coords.getString(0));
        
        JSONObject newPlace = new JSONObject();
        newPlace.put("id", recordid);
        newPlace.put("search", spatialDocumentFactory.makeSearchableContent(properties, schema));      
        newPlace.put("location_0_coordinate",coord.getLat());
        newPlace.put("location_1_coordinate",coord.getLon());
        
        return newPlace;
    }
	
	@Deprecated
	public JSONObject makeIndexableUserData(JSONObject placeObject, JSONObject userData) throws JSONException{
        
        JSONObject properties = placeObject.getJSONObject("properties");
        JSONObject geom = placeObject.getJSONObject("geometry");
        JSONArray coords = geom.getJSONArray("coordinates");
        Point coord = 
            new Point(
                    Double.parseDouble(coords.getString(1)), 
                    Double.parseDouble(coords.getString(0)));
        JSONArray coordsNew =new JSONArray();
        coordsNew.put(coords.getString(1));
        coordsNew.put(coords.getString(0));
        
        JSONObject newPlace = new JSONObject();
        newPlace.put("id", placeObject.getString("id"));
        newPlace.put("search", spatialDocumentFactory.makeSearchableUserDataContent(properties, userData));      
        newPlace.put("location_0_coordinate",coord.getLat());
        newPlace.put("location_1_coordinate",coord.getLon());
        
        return newPlace;
    }
	
	public JSONObject makeIndexableRecord(JSONObject record, JSONObject schema) throws JSONException{
	    
	    //geom is static
        JSONObject geom = record.getJSONObject("geometry");
        JSONArray coords = geom.getJSONArray("coordinates");
        Point coord = 
              new Point(
                      Double.parseDouble(coords.getString(1)), 
                      Double.parseDouble(coords.getString(0)));
        JSONArray coordsNew =new JSONArray();
        coordsNew.put(coords.getString(1));
        coordsNew.put(coords.getString(0));
        
        JSONObject georecord = new JSONObject();
	    
	    
	    return georecord;
        
//        JSONObject properties = placeObject.getJSONObject("properties");
//        JSONObject geom = placeObject.getJSONObject("geometry");
//        JSONArray coords = geom.getJSONArray("coordinates");
//        Point coord = 
//            new Point(
//                    Double.parseDouble(coords.getString(1)), 
//                    Double.parseDouble(coords.getString(0)));
//        JSONArray coordsNew =new JSONArray();
//        coordsNew.put(coords.getString(1));
//        coordsNew.put(coords.getString(0));
//        
//        JSONObject newPlace = new JSONObject();
//        newPlace.put("id", placeObject.getString("id"));
//        newPlace.put("search", spatialDocumentFactory.makeSearchableUserDataContent(properties, userData));      
//        newPlace.put("location_0_coordinate",coord.getLat());
//        newPlace.put("location_1_coordinate",coord.getLon());
//        
//        return newPlace;
    }
	
	
	
	
	public JSONObject makeIndexableDeal(JSONObject deal) throws JSONException{
        
        JSONObject location = deal.getJSONObject("location");
                
        JSONArray coordsNew =new JSONArray();
        coordsNew.put(new Double(location.getDouble("latitude")).toString());
        coordsNew.put(new Double(location.getDouble("longitude")).toString());
        
        JSONObject newDeal = new JSONObject();
        newDeal.put("id", deal.getString("id"));
        newDeal.put("search", spatialDocumentFactory.makeSearchableDealContent(deal));       
        newDeal.put("location_0_coordinate",location.getDouble("latitude"));
        newDeal.put("location_1_coordinate",location.getDouble("longitude"));
        
        return newDeal;
    }

}
