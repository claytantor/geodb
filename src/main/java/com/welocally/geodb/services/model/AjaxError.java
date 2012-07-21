package com.welocally.geodb.services.model;

public class AjaxError {
	private Integer errorCode;
	private String errorMessage;
	
	public static int REQ_PARSE_ERROR = 		103;
	public static int WL_SERVER_ERROR = 		106;
	public static int RES_GEN_ERROR = 			105;
	public static int PUBLISHER_KEY_MISSING = 	101;
	public static int SUBSCRIPTION_INVALID = 	102;
	public static int DATA_VALIDATION_MISSING = 104;
	public static int NO_IMPL = 110;
		
	public AjaxError(Integer errorCode, String errorMessage) {
		super();
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	public Integer getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}	
}
