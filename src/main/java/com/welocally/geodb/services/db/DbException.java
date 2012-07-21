package com.welocally.geodb.services.db;

import org.apache.commons.lang.StringUtils;

public class DbException extends Exception {
	
	public enum Type { OBJECT_NOT_FOUND, DB_ERROR };
	
	private Type exceptionType;
	
	private String entity;

	public DbException(Type t) {
		super();
		this.exceptionType = t;
	}
	
	public DbException(Type t, String entity) {
        super();
        this.exceptionType = t;
        this.entity = entity;
    }

	public DbException(Type t,Throwable arg1) {
		super(arg1);
		this.exceptionType = t;
	}

	public Type getExceptionType() {
		return exceptionType;
	}

	public void setExceptionType(Type exceptionType) {
		this.exceptionType = exceptionType;
		
	}

	@Override
	public String getMessage() {
		switch(exceptionType){
		case OBJECT_NOT_FOUND:{
		    String message = "The requested object was not found. ";
		    if(StringUtils.isNotEmpty(entity))
		        message = message+ " "+entity;

		    return message;
		}
		case DB_ERROR:{
			return "there was an error in the database";
		}
		}
		
		return super.getMessage();
	}

	

}
