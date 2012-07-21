package com.welocally.geodb.services.util;

import java.io.IOException;
import java.io.StringWriter;

import org.json.JSONException;
import org.json.JSONObject;

import com.welocally.geodb.services.db.DbException;

public interface JsonStoreLoader {

	public abstract void load(String fileName, Integer maxRecords, Integer commitEvery,
	        String endpoint) throws DbException;

	public abstract void loadSingle(JSONObject place, Integer count,
			Integer commitEvery, StringWriter sw,  String endpoint) throws JSONException, IOException;
	
	public abstract void deleteSingle(String id, Integer count,
            Integer commitEvery, StringWriter sw,  String endpoint) throws JSONException, IOException;

}