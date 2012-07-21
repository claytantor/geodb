package com.welocally.geodb.web.mvc;

import java.io.IOException;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import com.welocally.geodb.services.model.AjaxError;
import com.welocally.geodb.services.model.AjaxErrors;
import com.welocally.geodb.services.util.JsonObjectSerializer;

public abstract class AbstractJsonController {
	
	@Autowired
	JsonObjectSerializer jsonObjectSerializer;
	
	protected String makeObjectJson(JSONObject obj){
		try {
			String serialized = jsonObjectSerializer.serialize(obj);
			return serialized;
		} catch (IOException e1) {
			return "[{\"errorMessage\":\"server error\",\"errorCode\":106 }]";
		}
	}
	
	protected String makeModelJson(Map<String,Object> model){
		try {
			return jsonObjectSerializer.serialize(model);
		} catch (IOException e1) {
			return "[{\"errorMessage\":\"server error\",\"errorCode\":106 }]";
		}
	}
	
	protected String makeErrorsJson(Exception e){
		try {
			return jsonObjectSerializer.serialize(makeErrors(e));
		} catch (IOException e1) {
			return "[{\"errorMessage\":\"server error\",\"errorCode\":106 }]";
		}
	}
	
	protected AjaxErrors makeErrors(Exception e){
		AjaxErrors errors = new AjaxErrors();
		AjaxError error = new AjaxError(AjaxError.WL_SERVER_ERROR,e.getMessage());
		errors.getErrors().add(error);
		return errors;
	}

}
