package com.welocally.geodb.services.app;

import org.json.JSONObject;

public interface CommandSupport {
	
	public void doCommand(JSONObject command) throws CommandException;

}
