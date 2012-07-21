package com.welocally.geodb.services.db;

import org.json.JSONArray;

public class DbPage {
	
	private int pageSize;
	private int count;
	private int pageNum;
	private JSONArray objects;
	
	public DbPage() {
		super();
		objects = new JSONArray();
	}
	
	public int getPageSize() {
		return pageSize;
	}
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getPageNum() {
		return pageNum;
	}
	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
	}
	
	public JSONArray getObjects() {
		return objects;
	}

	public void setObjects(JSONArray objects) {
		this.objects = objects;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
	

}
