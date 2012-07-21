package com.welocally.geodb.services.spatial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SolrSearchService implements SpatialSearchService {
	
	static Logger logger = Logger.getLogger(SolrSearchService.class);

//	@Value("${SolrSearchService.endpoint:http://localhost:8983/solr/select/}")
//	private String endpoint;
//		

	@Override
	//wt=json&fq={!geofilt%20sfield=location}&pt=58.37587201036513,-134.58542687818408&d=5
	public JSONArray find(Point point, double km, String queryString, int start, int rows, String endpoint)
	        throws SpatialSearchException {
	 
		
		// Create an instance of HttpClient.
	    HttpClient client = new HttpClient();

	    // Create a method instance.
	    GetMethod method = new GetMethod(endpoint);
	    method.setQueryString(makeQueryString( point,  km,  queryString,  start, rows));
	    
	    // Provide custom retry handler is necessary
	    method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
	    		new DefaultHttpMethodRetryHandler(3, false));
	    
	    logger.debug(endpoint+"?"+method.getQueryString());

	    try {
	      // Execute the method.
	      int statusCode = client.executeMethod(method);

	      if (statusCode != HttpStatus.SC_OK) {
	        logger.error("Method failed: " + method.getStatusLine());
	      }

	      // Read the response body.
	      byte[] responseBody = method.getResponseBody();

	      // Deal with the response.
	      // Use caution: ensure correct character encoding and is not binary data
	      //System.out.println(new String(responseBody));
	      JSONArray ids = makeResultFromResponse(responseBody);
	      
	      //use the ids to get places
	      return ids;

	    } catch (HttpException e) {
	    	logger.error("Fatal protocol violation: " + e.getMessage(),e);
	    } catch (IOException e) {
	    	logger.error("Fatal protocol violation: " + e.getMessage(),e);
	    } catch (JSONException e) {
	    	logger.error("json problem: " + e.getMessage(),e);
        } finally {
	      // Release the connection.
	      method.releaseConnection();
	    } 
		
	    return null;
	}
	
	//wt=json&fq={!geofilt%20sfield=location}&pt=58.37587201036513,-134.58542687818408&d=5&start=0&rows=10
	//http://ec2-174-129-45-44.compute-1.amazonaws.com:8983/solr/select/?q=Art&version=2.2&start=0&rows=10&indent=on&wt=json&fq={!geofilt%20sfield=location}&pt=38.898748,-77.037684&d=5&sort=geodist(location)%20asc&fl=_id,_dist_:geodist(location)
	private NameValuePair[] makeQueryString(Point point, double km, String queryString, int start, int rows){
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new NameValuePair("q", queryString));
		pairs.add(new NameValuePair("wt", "json"));
		pairs.add(new NameValuePair("fq", "{!geofilt sfield=location}"));
		pairs.add(new NameValuePair("pt", point.getLat()+","+point.getLon()));
		pairs.add(new NameValuePair("d", ""+km));
		pairs.add(new NameValuePair("sort", "geodist(location) asc"));
		pairs.add(new NameValuePair("fl", "_id,_dist_:geodist(location)"));
		pairs.add(new NameValuePair("start", ""+start));
		pairs.add(new NameValuePair("rows", ""+rows));
		
		return (NameValuePair[]) pairs.toArray(new NameValuePair[pairs.size()]);
	}
	
	/**
	 * 
{
    "responseHeader": {
        "status": 0,
        "QTime": 6,
        "params": {
            "d": "5",
            "indent": "on",
            "start": "0",
            "q": "Art",
            "pt": "58.37587201036513,-134.58542687818408",
            "wt": "json",
            "fq": "{!geofilt sfield=location}",
            "version": "2.2",
            "rows": "10"
        }
    },
    "response": {
        "numFound": 9,
        "start": 0,
        "docs": [
            {
                "_id": "WL_7ZFwkbDdXSC2uudFhFFUP9_58.359826_-134.587981@1293731153",
                "search": "Alaska Graphic Arts Professional Art Services Services graphic designer ",
                "location_1_coordinate": -134.587981,
                "location_0_coordinate": 58.359826
            },
            {
                "_id": "WL_7lDSzsQATgpYi5B4LYyCCw_58.382350_-134.637219@1293731153",
                "search": "Gallery Art & Framing Shopping  Retail Goods art gallery dealer ",
                "location_1_coordinate": -134.637219,
                "location_0_coordinate": 58.38235
            },
            {
                "_id": "WL_2u2M3U9FqnrRKQIBlWrXDh_58.360017_-134.573908@1293731153",
                "search": "Photographic Endeavors Professional Art Services Services photographer ",
                "location_1_coordinate": -134.573908,
                "location_0_coordinate": 58.360017
            },
            {
                "_id": "WL_6j6qACIWhqcyi8VkXPgJGy_58.338559_-134.541280@1293731153",
                "search": "Anderson Artworks Professional Art Services Services artist ",
                "location_1_coordinate": -134.54128,
                "location_0_coordinate": 58.338559
            },
            {
                "_id": "WL_5g9F5sK1lN0ZsRm9aNpJxg_58.405909_-134.596797@1293731153",
                "search": "Interdesign Professional Art Services Services graphic designer ",
                "location_1_coordinate": -134.596797,
                "location_0_coordinate": 58.405909
            },
            {
                "_id": "WL_7IfZZNsBLKJ9XThlky0ciZ_58.363406_-134.577531@1293731153",
                "search": "Art Department Shopping  Retail Goods material artist supply ",
                "location_1_coordinate": -134.577531,
                "location_0_coordinate": 58.363406
            },
            {
                "_id": "WL_1eZqCHwfBsplJTDcrmVqly_58.363406_-134.577531@1293731153",
                "search": "Southeast Artworks Shopping  Retail Goods art gallery dealer ",
                "location_1_coordinate": -134.577531,
                "location_0_coordinate": 58.363406
            },
            {
                "_id": "WL_3vlABqEDkpdYssKuOhajlL_58.363506_-134.579814@1293731153",
                "search": "Rie Munoz Gallery Shopping  Retail Goods art gallery dealer ",
                "location_1_coordinate": -134.579814,
                "location_0_coordinate": 58.363506
            },
            {
                "_id": "WL_4p4Fv7fncXYDUyqTaQ1CqZ_58.399929_-134.557179@1293731153",
                "search": "S E Artwork Shopping  Retail Goods art gallery dealer ",
                "location_1_coordinate": -134.557179,
                "location_0_coordinate": 58.399929
            }
        ]
    }
}	 
	 * 
	 * @param response
	 * @return
	 * @throws JSONException 
	 */
	private JSONArray makeResultFromResponse(byte[] responseBuffer) throws JSONException{
		JSONObject responseAll = new JSONObject(new String(responseBuffer));
		JSONObject responseObject =responseAll.getJSONObject("response");
		JSONArray docs = responseObject.getJSONArray("docs");
		return docs;
	}


}
