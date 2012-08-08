package com.welocally.geodb.web.mvc;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Value;

@Deprecated
public abstract class ShardableJsonController extends AbstractJsonController {
	
	static Logger logger = 
		Logger.getLogger(ShardableJsonController.class);
	
	
	@Value("${geodb.shard.sharded:false}") Boolean sharded;
	
	@Value("${geodb.shard.nodeList:node}") String shardNodeList;
	
	//this would be pretty big in PROD something like 100
	@Value("${geodb.shard.requestThreads:10}") Integer requestThreads;

    private ExecutorService threadExecutor;

    public void initExecutor(){
    	threadExecutor = Executors.newFixedThreadPool(requestThreads);
    }
	
	public JSONArray shardRequest(HttpServletRequest request){
		
		JSONArray response = null;
		String[] shards = shardNodeList.split(",");
		BlockingQueue<ShardResponse> responses = 
			new LinkedBlockingQueue<ShardResponse>();
		
		 HttpClient httpClient = 
			 new HttpClient(new MultiThreadedHttpConnectionManager());
		
		for (String instance : shards) {
			String url = instance+request.getRequestURI()+"?"+request.getQueryString();
			logger.debug("sharding to url:"+url);
			String key = UUID.randomUUID().toString();
			ShardRequest shardRequest = new ShardRequest(key,url,responses,httpClient);
			threadExecutor.execute(shardRequest);
		}
		
//		try {
//			ShardResponse shardedResponse = 
//				responses.poll(10, TimeUnit.SECONDS);
//			response = (JSONArray)JSON.parse(shardedResponse.getResponseJson());
//			
//		} catch (InterruptedException e) {
//			logger.error("problem getting response");
//		}

		throw new RuntimeException("NO IMPL");
	
	}
	
	private class ShardRequest  implements Runnable{
		
		private HttpClient httpClient;
		
		private String key;
		
		private String jsonResult;
		
		private GetMethod method;
		
		private BlockingQueue<ShardResponse> responses;
		
		public ShardRequest(
				String key, 
				String url, 
				BlockingQueue<ShardResponse> responses,
				HttpClient httpClient){
			this.key = key;
			this.httpClient = httpClient;
			this.method = new GetMethod(url);
			this.responses = responses;
		}
		

		public void run() {
			throw new RuntimeException("NO IMPL");
//			try {
//
//				logger.debug(key + " - about to get something from "
//						+ method.getURI());
//				// execute the method
//				
//				httpClient.executeMethod(method);
//
//				logger.debug(key + " - get executed");
//				// get the response body as an array of bytes
//				byte[] bytes = method.getResponseBody();
//				String json = new String(bytes);
//				JSONArray array = (JSONArray)JSON.parse(json);
//				if(array.length()>0){
//					ShardResponse response = 
//						new ShardResponse(key, method.getURI().toString(), json);
//					
//					responses.put(response);
//				}
//
//				logger.debug(key + " - " + bytes.length + " bytes read");
//
//			} catch (Exception e) {
//				logger.debug(key + " - error: " + e);
//			} finally {
//				// always release the connection after we're done
//				method.releaseConnection();
//				logger.debug(key + " - connection released");
//			}

		}


		public String getJsonResult() {
			return jsonResult;
		}

		public void setJsonResult(String jsonResult) {
			this.jsonResult = jsonResult;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}		
		
	}

	
	class ShardResponse {
		private String key;
		private String url;
		private String responseJson;
		
		
		
		public ShardResponse(String key, String url, String responseJson) {
			super();
			this.key = key;
			this.url = url;
			this.responseJson = responseJson;
		}
		public String getKey() {
			return key;
		}
		public void setKey(String key) {
			this.key = key;
		}
		public String getUrl() {
			return url;
		}
		public void setUrl(String url) {
			this.url = url;
		}
		public String getResponseJson() {
			return responseJson;
		}
		public void setResponseJson(String responseJson) {
			this.responseJson = responseJson;
		}
		
		
	}
	
	

}
