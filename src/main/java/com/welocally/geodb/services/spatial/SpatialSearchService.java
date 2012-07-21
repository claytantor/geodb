package com.welocally.geodb.services.spatial;

import org.json.JSONArray;

public interface SpatialSearchService {

	
	public JSONArray find(Point point, double km, String queryString, int start, int rows, String endpoint) 
		throws SpatialSearchException ;
	

	
}