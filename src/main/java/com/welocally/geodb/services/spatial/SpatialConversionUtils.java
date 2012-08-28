package com.welocally.geodb.services.spatial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.spatial.tier.projections.IProjector;
import org.apache.lucene.spatial.tier.projections.SinusoidalProjector;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class SpatialConversionUtils {
    
    static Logger logger = Logger.getLogger(SpatialConversionUtils.class);
	
	public static final String LAT_FIELD = "lat";
	public static final String LON_FIELD = "lng";
	
	public static final double MILE = 1.609344;
  	
	private double maxMiles = 250, minMiles = 1;
	private IProjector projector = new SinusoidalProjector();
	private CartesianTierPlotter ctp = new CartesianTierPlotter(0, projector, CartesianTierPlotter.DEFALT_FIELD_PREFIX);
	// startTier is 14 for 25 miles, 15 for 1 miles in lucene 3.0
	private int startTier = ctp.bestFit(maxMiles), endTier = ctp.bestFit(minMiles);
	
	
	/**
	 * {
    "classifiers[0][subcategory]": [
        "Japanese"
    ],
    "geometry[type]": [
        "Point"
    ],
    "properties[province]": [
        "CA"
    ],
    "type": [
        "Place"
    ],
    "properties[country]": [
        "US"
    ],
    "geometry[coordinates][]": [
        "-122.2711137",
        "37.8043637"
    ],
    "properties[address]": [
        "null null"
    ],
    "properties[postcode]": [
        "null"
    ],
    "owner": "anonymous",
    "callback": [
        "jQuery16109296544857788831_1332260765862"
    ],
    "classifiers[0][type]": [
        "Food & Drink"
    ],
    "classifiers[0][category]": [
        "Restaurant"
    ],
    "_": [
        "1332261879322"
    ],
    "properties[city]": [
        "Oakland"
    ]
}

converted to

[
    {
        "if": "WL_3Wnkj5RxX8iKzTR5qek2Fs_37.826065_-122.209171@1293134755",
        "properties": {
            "tags": [
                "sandwich"
            ],
            "phone": "+1 510 339 3721",
            "classifiers": [
                {
                    "category": "Restaurant",
                    "subcategory": "",
                    "type": "Food & Drink"
                }
            ],
            "address": "2069 Antioch Ct",
            "name": "Grinders Submarine Sandwiches",
            "province": "CA",
            "owner": "welocally",
            "postcode": "94611",
            "city": "Oakland",
            "country": "US"
        },
        "type": "Place",
        "geometry": {
            "type": "Point",
            "coordinates": [
                -122.209,
                37.826099
            ]
        }
    }
]
	 * 
	 * 
	 * @param placeQueryString
	 * @return
	 */
	
	public JSONObject convertQueryStringToPlace(JSONObject placeQueryString) {
	    
	 	   
        try {
            JSONObject place = new JSONObject();
            
            //properties
            Map<String,Object> properties = new HashMap<String,Object>();
            properties.put("address", ((String[])placeQueryString.get("properties[address]"))[0]);
            properties.put("name", ((String[])placeQueryString.get("properties[name]"))[0]);
            properties.put("postcode", ((String[])placeQueryString.get("properties[postcode]"))[0]);
            properties.put("city", ((String[])placeQueryString.get("properties[city]"))[0]);
            properties.put("province", ((String[])placeQueryString.get("properties[province]"))[0]);
            properties.put("country", ((String[])placeQueryString.get("properties[country]"))[0]);
            properties.put("name", ((String[])placeQueryString.get("properties[name]"))[0]);

            if(placeQueryString.has("properties[phone]"))
                properties.put("phone", ((String[])placeQueryString.get("properties[phone]"))[0]);
            if(placeQueryString.has("properties[website]"))
                properties.put("website", ((String[])placeQueryString.get("properties[website]"))[0]);
            place.put("properties", properties);
            
            //geometry
            JSONObject geometry = new JSONObject();
            geometry.put("type", "Point");           
            String[] coordinates = (String[])placeQueryString.get("geometry[coordinates][]");
            List<Double> coords = new ArrayList<Double>();
            coords.add(Double.parseDouble(coordinates[0]));
            coords.add(Double.parseDouble(coordinates[1]));
            
            geometry.put("coordinates", new JSONArray(coordinates));
            place.put("geometry", geometry);
            
            //classifiers
            JSONArray classifiers = new JSONArray();
            JSONObject classifier = new JSONObject();
            
            classifier.put("type", ((String[])placeQueryString.get("properties[classifiers][0][type]"))[0]);
            classifier.put("category", ((String[])placeQueryString.get("properties[classifiers][0][category]"))[0]);
            classifier.put("subcategory", ((String[])placeQueryString.get("properties[classifiers][0][subcategory]"))[0]);
            
            classifiers.put(classifier);
            properties.put("classifiers", classifiers);
                                        
            place.put("type", "Place");
            
            if(!placeQueryString.isNull("if")){
                place.put("if", ((String[])placeQueryString.get("if"))[0].toString());
            }
            
            if(!placeQueryString.isNull("owner")){
                place.put("owner", ((String[])placeQueryString.get("owner"))[0].toString());
            }
            
            return place;
            
        } catch (NumberFormatException e) {
            logger.error(e);
            return null;
        } catch (JSONException e) {
            logger.error(e);
            throw new RuntimeException(e);
        }
        
        
    }
	
	public Point getJSONPoint(JSONObject recordObject){
		try {
			JSONObject geom = recordObject.getJSONObject("geometry");
			JSONArray coords = geom.getJSONArray("coordinates");
			Point coord = new Point(
					Double.parseDouble(coords.getString(1)), Double
							.parseDouble(coords.getString(0)));
			return coord;
		} catch (NumberFormatException e) {
			throw new RuntimeException(e);
		} catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
	}


	public double getMaxMiles() {
		return maxMiles;
	}


	public void setMaxMiles(double maxMiles) {
		this.maxMiles = maxMiles;
	}


	public double getMinMiles() {
		return minMiles;
	}


	public void setMinMiles(double minMiles) {
		this.minMiles = minMiles;
	}


	public IProjector getProjector() {
		return projector;
	}


	public void setProjector(IProjector projector) {
		this.projector = projector;
	}


	public CartesianTierPlotter getCtp() {
		return ctp;
	}


	public void setCtp(CartesianTierPlotter ctp) {
		this.ctp = ctp;
	}


	public int getStartTier() {
		return startTier;
	}


	public void setStartTier(int startTier) {
		this.startTier = startTier;
	}


	public int getEndTier() {
		return endTier;
	}


	public void setEndTier(int endTier) {
		this.endTier = endTier;
	}
	
	

}
