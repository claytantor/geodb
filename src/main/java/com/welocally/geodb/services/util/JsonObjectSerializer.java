package com.welocally.geodb.services.util;

import java.io.IOException;
import java.io.StringWriter;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JsonObjectSerializer {
	
	private ObjectMapper jacksonMapper = new ObjectMapper();
	
	public String serialize(Object o) throws IOException{
		StringWriter sw = new StringWriter(); // serialize
		MappingJsonFactory jsonFactory = new MappingJsonFactory();
		JsonGenerator jsonGenerator = jsonFactory
				.createJsonGenerator(sw);
		jacksonMapper.writeValue(jsonGenerator, o);
		sw.flush();
		sw.close();

		return sw.getBuffer().toString();
	}

}
