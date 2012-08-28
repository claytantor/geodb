package com.welocally.geodb.services.db;

import org.json.JSONArray;
import org.json.JSONObject;

public interface JsonDatabase {
    
    public enum EntityType { PLACE, USER_PLACE, CLASSIFER, DEAL, PUBLISHER };
    
    public enum StatusType { PUBLISHED, INCOMPLETE, REVIEW };

	public JSONObject findById(String collectionName, String id)
		throws DbException;
	
	public JSONArray findUserPlaces(String publisherKey, String collectionName, String status) 
	throws DbException;
	
	public void put(JSONObject doc, JSONObject schema, String collectionName, String id, EntityType type, StatusType status)
    throws DbException;

	public void delete(String collectionName, String id)
        throws DbException;

	public void deleteAll(String collectionName)
        throws DbException;
	
	public DbPage findAll(String collectionName, int pageNum) throws DbException;
	
	public JSONArray findDistinct(String collectionName, String key, JSONObject query)
		throws DbException;
	
	public DbPage findByExample(String collectionName, int pageNumber, JSONObject example)
		throws DbException;
	
	public DbPage findByExampleIncluding(String collectionName, int pageNumber, JSONObject example, JSONObject inclusions)
	throws DbException;
	
	

}